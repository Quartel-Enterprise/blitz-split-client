package com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.RecordSettlement
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiAction
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class RegisterPaymentViewModel(
    private val route: Route.RegisterPayment,
    private val navigator: Navigator,
    private val recordSettlement: RecordSettlement,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    private val decimalSeparator: DeviceDecimalSeparator,
    observeGroup: ObserveGroup,
    observeGroupBalances: ObserveGroupBalances,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<RegisterPaymentUiState>
        field = MutableStateFlow(createInitialState())

    val uiAction: SharedFlow<RegisterPaymentUiAction>
        field = MutableSharedFlow<RegisterPaymentUiAction>()

    val stateWatcher: FlowWatcher<RegisterPaymentUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<RegisterPaymentUiAction> = FlowWatcher(uiAction)

    private val amountSeparators = setOf(',', '.')

    // Why: the debt is gone (paid elsewhere, the pair or the group gone) while another screen covered this one.
    private var isDebtGone = false

    // Why: what Room said last, read again when a save fails, since the changes meanwhile were set aside.
    private var latest: Pair<Group?, GroupBalances>? = null

    /*
     * Why: a sync may bring a payment or a bill that changes the debt while the form is open, settle it, or remove
     * either person or the whole group, which leaves nothing to pay.
     */
    init {
        combine(observeGroup(route.groupId), observeGroupBalances(route.groupId), ::Pair)
            .onEach { (group, balances) -> handle(group, balances) }
            .launchIn(viewModelScope)
        navigator.backStack
            .onEach { if (isDebtGone) leave() }
            .launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: RegisterPaymentUiEvent) {
        when (event) {
            is RegisterPaymentUiEvent.OnAmountChanged -> uiState.update {
                it.copy(
                    amountText = event.text
                        .filter { char -> char.isDigit() || char in amountSeparators }
                        .take(AMOUNT_MAX_LENGTH),
                    hasEditedAmount = true,
                )
            }

            RegisterPaymentUiEvent.OnFullDebtClicked -> uiState.update {
                it.copy(
                    amountText = toAmountText(it.debtMinor, it.currency),
                    hasEditedAmount = false,
                )
            }

            is RegisterPaymentUiEvent.OnMethodPicked -> uiState.update { it.copy(method = event.method) }

            RegisterPaymentUiEvent.OnPaidAtOpened -> refreshPaidAtLimit()

            is RegisterPaymentUiEvent.OnPaidAtChanged -> changePaidAt(event.epochMillis)

            RegisterPaymentUiEvent.OnConfirmClicked -> confirm()

            RegisterPaymentUiEvent.OnCloseClicked -> leave()
        }
    }

    private fun createInitialState(): RegisterPaymentUiState {
        val now = currentTimeMillis()
        return RegisterPaymentUiState(
            currency = GroupCurrency.BRL,
            from = null,
            to = null,
            debtMinor = 0L,
            amountText = "",
            hasEditedAmount = false,
            method = SettlementMethod.PIX,
            paidAt = now,
            paidDay = relativeDayOf(now),
            latestPaidAt = now,
            isOnline = true,
            isSaving = false,
            isLoaded = false,
        )
    }

    private fun handle(
        group: Group?,
        balances: GroupBalances,
    ) {
        latest = group to balances
        // Why: once saved, the debt drops by the payment; the form is on its way out and keeps what it shows.
        if (uiState.value.isSaving) return
        val debtMinor = balances.debtOf(route.fromMemberId, route.toMemberId)
        isDebtGone = group == null || debtMinor == 0L
        if (group == null || isDebtGone) {
            leave()
            return
        }
        // Why: a debt with someone removed from the group stays until it is paid, "Ex-membro" on the form.
        val from = group.members.firstOrNull { it.id == route.fromMemberId }
        val to = group.members.firstOrNull { it.id == route.toMemberId }
        uiState.update {
            it.copy(
                currency = group.currency,
                from = from?.let(::toPerson),
                to = to?.let(::toPerson),
                debtMinor = debtMinor,
                // Why: until something is typed, the field follows the whole debt as a sync changes it.
                amountText = if (it.hasEditedAmount) it.amountText else toAmountText(debtMinor, group.currency),
                isLoaded = true,
            )
        }
    }

    // Why: "Hoje" may have become "Ontem" since the form opened, and the picker may now reach a new day.
    private fun refreshPaidAtLimit() {
        val now = currentTimeMillis()
        uiState.update {
            it.copy(
                paidDay = relativeDayOf(it.paidAt),
                latestPaidAt = now,
            )
        }
    }

    // Why: a later time today, which the pickers can't rule out, becomes now.
    private fun changePaidAt(epochMillis: Long) {
        val now = currentTimeMillis()
        val paidAt = minOf(epochMillis, now)
        uiState.update {
            it.copy(
                paidAt = paidAt,
                paidDay = relativeDayOf(paidAt),
                latestPaidAt = now,
            )
        }
    }

    private fun confirm() {
        val state = uiState.value
        if (!state.canConfirm) return
        val amountMinor = state.amountMinor ?: return
        uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            recordSettlement(
                groupId = route.groupId,
                fromMemberId = route.fromMemberId,
                toMemberId = route.toMemberId,
                amountMinor = amountMinor,
                method = state.method,
                paidAt = state.paidAt,
            ).onSuccess {
                leave()
            }.onFailure {
                uiState.update { it.copy(isSaving = false) }
                latest?.let { (group, balances) -> handle(group, balances) }
                uiAction.emit(RegisterPaymentUiAction.ShowSaveError)
            }
        }
    }

    private fun toPerson(member: GroupMember): PaymentPerson = PaymentPerson(
        id = member.id,
        name = member.name,
        initials = Initials.ofMember(member.name, member.isMe),
        avatarColor = member.avatarColor,
        isMe = member.isMe,
    )

    // Why: a language whose separator is neither (Arabic's "٫") would fill in text AmountInput can't read back.
    private fun toAmountText(
        amountMinor: Long,
        currency: GroupCurrency,
    ): String = AmountInput.format(
        amountMinor = amountMinor,
        currency = currency,
        decimalSeparator = decimalSeparator().takeIf { it in amountSeparators } ?: '.',
    )

    private fun relativeDayOf(epochMillis: Long): RelativeDay = SpentDay.relativeDayOf(
        epochMillis = epochMillis,
        nowMillis = currentTimeMillis(),
        timeZone = currentTimeZone(),
    )

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    private fun leave() {
        if (navigator.backStack.value.lastOrNull() == route) navigator.navigateBack()
    }

    private companion object {
        // Why: "100.000.000,00" with room for the separators someone types.
        const val AMOUNT_MAX_LENGTH = 16
    }
}
