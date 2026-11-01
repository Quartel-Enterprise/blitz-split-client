package com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettlementLeg
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.RecordSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.SettleUp
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentTarget
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiAction
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.math.abs

/*
 * Why: one pair of a group (Route.RegisterPayment), or the whole net with someone across groups from the global
 * Saldos (Route.SettleUp), recorded as one payment per group.
 */
class RegisterPaymentViewModel(
    private val route: Route,
    private val navigator: Navigator,
    private val recordSettlement: RecordSettlement,
    private val settleUp: SettleUp,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    private val decimalSeparator: DeviceDecimalSeparator,
    observeGroup: ObserveGroup,
    observeGroupBalances: ObserveGroupBalances,
    observePeopleBalances: ObservePeopleBalances,
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
    private var latest: PaymentTarget? = null

    // Why: a save that fails before Room answered has nothing to show again.
    private var hasRead = false

    /*
     * Why: a sync may bring a payment or a bill that changes the debt while the form is open, settle it, or remove
     * either person or the whole group, which leaves nothing to pay.
     */
    init {
        observeTarget(observeGroup, observeGroupBalances, observePeopleBalances)
            .onEach { target ->
                hasRead = true
                handle(target)
            }.launchIn(viewModelScope)
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
            is RegisterPaymentUiEvent.OnAmountChanged -> if (!uiState.value.isAmountLocked) {
                uiState.update {
                    it.copy(
                        amountText = event.text
                            .filter { char -> char.isDigit() || char in amountSeparators }
                            .take(AMOUNT_MAX_LENGTH),
                        hasEditedAmount = true,
                    )
                }
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

            // Why: a payment being saved finishes first, on every platform, so it is never left half done.
            RegisterPaymentUiEvent.OnCloseClicked -> if (!uiState.value.isSaving) leave()
        }
    }

    private fun createInitialState(): RegisterPaymentUiState {
        val now = currentTimeMillis()
        return RegisterPaymentUiState(
            currency = GroupCurrency.BRL,
            from = null,
            to = null,
            debtMinor = 0L,
            groupParts = emptyList(),
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

    private fun observeTarget(
        observeGroup: ObserveGroup,
        observeGroupBalances: ObserveGroupBalances,
        observePeopleBalances: ObservePeopleBalances,
    ): Flow<PaymentTarget?> = when (route) {
        is Route.RegisterPayment -> combine(observeGroup(route.groupId), observeGroupBalances(route.groupId)) {
            group,
            balances,
            ->
            group?.let { pairTargetOf(route, it, balances) }
        }

        is Route.SettleUp -> observePeopleBalances().map { people -> personTargetOf(route, people) }

        else -> flowOf(null)
    }

    // Why: a debt with someone removed from the group stays until it is paid, "Ex-membro" on the form.
    private fun pairTargetOf(
        route: Route.RegisterPayment,
        group: Group,
        balances: GroupBalances,
    ): PaymentTarget {
        val debtMinor = balances.debtOf(route.fromMemberId, route.toMemberId)
        return PaymentTarget(
            currency = group.currency,
            from = group.members.firstOrNull { it.id == route.fromMemberId }?.let(::toPerson),
            to = group.members.firstOrNull { it.id == route.toMemberId }?.let(::toPerson),
            debtMinor = debtMinor,
            legs = listOf(
                SettlementLeg(
                    groupId = route.groupId,
                    fromMemberId = route.fromMemberId,
                    toMemberId = route.toMemberId,
                    amountMinor = debtMinor,
                ),
            ),
            groupParts = emptyList(),
        )
    }

    // Why: the net decides who pays whom; each group's payment goes the way that group's pair owes.
    private fun personTargetOf(
        route: Route.SettleUp,
        people: PeopleBalances,
    ): PaymentTarget? {
        val person = people.findPersonOrNull(route.personKey, route.currency.code) ?: return null
        val me = people.me?.let(::toPerson)
        val other = PaymentPerson(
            id = person.groups.first().memberId,
            name = person.name.orEmpty(),
            initials = person.name?.let(Initials::of).orEmpty(),
            avatarColor = person.face?.color ?: AvatarColor.default,
            isMe = false,
        ).takeIf { person.name != null }
        val owesMe = person.netMinor > 0
        return PaymentTarget(
            currency = person.currency,
            from = if (owesMe) other else me,
            to = if (owesMe) me else other,
            debtMinor = abs(person.netMinor),
            legs = person.legs,
            groupParts = person.groups,
        )
    }

    private fun handle(target: PaymentTarget?) {
        latest = target
        // Why: once saved, the debt drops by the payment; the form is on its way out and keeps what it shows.
        if (uiState.value.isSaving) return
        isDebtGone = target == null || target.debtMinor == 0L
        if (target == null || isDebtGone) {
            leave()
            return
        }
        uiState.update {
            it.copy(
                currency = target.currency,
                from = target.from,
                to = target.to,
                debtMinor = target.debtMinor,
                groupParts = target.groupParts,
                // Why: until something is typed, the field follows the whole debt as a sync changes it.
                amountText = if (it.hasEditedAmount && target.groupParts.size <= 1) {
                    it.amountText
                } else {
                    toAmountText(target.debtMinor, target.currency)
                },
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
        val target = latest
        if (!state.canConfirm || target == null) return
        val amountMinor = state.amountMinor ?: return
        uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            // Why: a pair of one group may pay part of the debt; a settling up of a single group too, through its one payment.
            val result = if (route is Route.SettleUp) {
                settleUp(
                    legs = target.legs
                        .singleOrNull()
                        ?.let { leg -> listOf(leg.copy(amountMinor = amountMinor)) }
                        ?: target.legs,
                    method = state.method,
                    paidAt = state.paidAt,
                    personName = (if (state.kind == PaymentKind.PAID) state.to else state.from)?.name,
                    isPaidByMe = state.kind == PaymentKind.PAID,
                ).map {}
            } else {
                val leg = target.legs.first()
                recordSettlement(
                    groupId = leg.groupId,
                    fromMemberId = leg.fromMemberId,
                    toMemberId = leg.toMemberId,
                    amountMinor = amountMinor,
                    method = state.method,
                    paidAt = state.paidAt,
                ).map {}
            }
            result
                .onSuccess {
                    leave()
                }.onFailure {
                    uiState.update { it.copy(isSaving = false) }
                    if (hasRead) handle(latest)
                    // Why: a debt settled or a group removed meanwhile closed the form, which leaves nothing to retry.
                    if (isOnTop()) uiAction.emit(RegisterPaymentUiAction.ShowSaveError)
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
        if (isOnTop()) navigator.navigateBack()
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    private companion object {
        // Why: "100.000.000,00" with room for the separators someone types.
        const val AMOUNT_MAX_LENGTH = 16
    }
}
