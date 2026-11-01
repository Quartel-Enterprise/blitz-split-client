package com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.expenses.domain.model.PersonBalance
import com.quare.blitzsplit.core.expenses.domain.model.SettledUp
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeSettledUp
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettledUp
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePaymentKeys
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.feature.globalbalances.presentation.mapper.idOf
import com.quare.blitzsplit.feature.globalbalances.presentation.mapper.rowsOf
import com.quare.blitzsplit.feature.globalbalances.presentation.model.BalancesEmptyState
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeDraft
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiAction
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiEvent
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class GlobalBalancesViewModel(
    private val route: Route.GlobalBalances,
    private val navigator: Navigator,
    private val consumeSettledUp: ConsumeSettledUp,
    private val deleteSettlement: DeleteSettlement,
    observePeopleBalances: ObservePeopleBalances,
    observeSettledUp: ObserveSettledUp,
    observeProfile: ObserveProfile,
    observeAccount: ObserveAccount,
    observePaymentKeys: ObservePaymentKeys,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<GlobalBalancesUiState>
        field = MutableStateFlow(
            GlobalBalancesUiState(
                displayName = "",
                avatarInitial = "",
                avatarColor = AvatarColor.default,
                photoUrl = null,
                accountProvider = null,
                people = emptyList(),
                totals = listOf(zeroTotalOf(GroupCurrency.fallback)),
                filter = route.filter,
                pixKey = null,
                charge = null,
                isOnline = true,
                isLoaded = false,
                recordedPayment = null,
            ),
        )

    val uiAction: SharedFlow<GlobalBalancesUiAction>
        field = MutableSharedFlow<GlobalBalancesUiAction>()

    val stateWatcher: FlowWatcher<GlobalBalancesUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<GlobalBalancesUiAction> = FlowWatcher(uiAction)

    // Why: what Room said last, which the rows are drawn from again when the filter or a toggle changes.
    private var balances = PeopleBalances(
        people = emptyList(),
        totals = emptyList(),
        me = null,
    )

    // Why: the rows whose "Por grupo" differs from how they open.
    private var toggledIds = emptySet<String>()

    // Why: the payments the notice on screen undoes together.
    private var noticeSettlementIds = emptyList<String>()

    init {
        combine(observeProfile(), observeAccount()) { profile, account ->
            val displayName = profile?.displayName?.trim().orEmpty()
            uiState.update {
                it.copy(
                    displayName = displayName,
                    avatarInitial = Initials.firstOf(displayName),
                    avatarColor = profile?.avatarColor ?: AvatarColor.default,
                    photoUrl = profile?.photo?.toImageUrl(),
                    accountProvider = (account.status as? AccountStatus.Connected)?.provider,
                )
            }
        }.launchIn(viewModelScope)
        observePeopleBalances()
            .onEach { people ->
                balances = people
                showPeople()
            }.launchIn(viewModelScope)
        observePaymentKeys()
            .map(::pixKeyOrNull)
            .distinctUntilChanged()
            .onEach { key -> uiState.update { it.copy(pixKey = key) } }
            .launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
        // Why: the notice waits until Saldos is on top again, under Registrar pagamento, and Room is read.
        combine(observeSettledUp(), navigator.backStack, uiState.map { it.isLoaded }.distinctUntilChanged(), ::Triple)
            .onEach { (settledUp, _, isLoaded) ->
                if (settledUp != null && isLoaded && isOnTop()) showSettledUp(settledUp)
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: GlobalBalancesUiEvent) {
        when (event) {
            is GlobalBalancesUiEvent.OnFilterChanged -> changeFilter(event.filter)

            is GlobalBalancesUiEvent.OnPersonExpandToggled -> toggle(event.personId)

            is GlobalBalancesUiEvent.OnSettleClicked -> openSettleUp(event.personId, owesYou = false)

            is GlobalBalancesUiEvent.OnReceivedClicked -> openSettleUp(event.personId, owesYou = true)

            is GlobalBalancesUiEvent.OnChargeClicked -> openCharge(event.personId)

            is GlobalBalancesUiEvent.OnChargePixKeyToggled -> updateCharge { charge ->
                if (charge.canTogglePixKey) charge.copy(includesPixKey = event.isIncluded) else charge
            }

            is GlobalBalancesUiEvent.OnChargeMessageChanged -> updateCharge { it.copy(editedMessage = event.message) }

            is GlobalBalancesUiEvent.OnChargeShareClicked -> emitForCharge(
                GlobalBalancesUiAction.ShareMessage(event.message),
            )

            is GlobalBalancesUiEvent.OnChargeCopyClicked -> emitForCharge(
                GlobalBalancesUiAction.CopyMessage(event.message),
            )

            GlobalBalancesUiEvent.OnChargeDismissed -> uiState.update { it.copy(charge = null) }

            GlobalBalancesUiEvent.OnEmptyActionClicked -> handleEmptyAction()

            GlobalBalancesUiEvent.OnGroupsTabClicked -> backToGroups()

            GlobalBalancesUiEvent.OnContactsTabClicked,
            GlobalBalancesUiEvent.OnActivityTabClicked,
            -> viewModelScope.launch { uiAction.emit(GlobalBalancesUiAction.ShowComingSoon) }

            GlobalBalancesUiEvent.OnProfileClicked -> if (isOnTop()) navigator.navigate(Route.Profile)

            GlobalBalancesUiEvent.OnUndoPaymentClicked -> undoSettledUp()

            GlobalBalancesUiEvent.OnRecordedPaymentNoticeShown -> uiState.update { it.copy(recordedPayment = null) }
        }
    }

    private fun showPeople() {
        val state = uiState.value
        val rows = rowsOf(balances.people, state.filter, toggledIds)
        uiState.update {
            it.copy(
                people = rows,
                totals = balances.totals.ifEmpty { listOf(zeroTotalOf(GroupCurrency.fallback)) },
                // Why: a sync may settle the person being charged meanwhile, which leaves nothing to ask for.
                charge = it.charge
                    ?.let { charge ->
                        balances.people.firstOrNull { person ->
                            idOf(person) ==
                                charge.personId
                        }
                    }?.takeIf { person -> person.netMinor > 0 }
                    ?.let { person -> chargeOf(person, it.charge, it.pixKey) },
                isLoaded = true,
            )
        }
    }

    // Why: a new filter opens its lone person's "Por grupo" afresh.
    private fun changeFilter(filter: BalancesFilter) {
        if (filter == uiState.value.filter) return
        toggledIds = emptySet()
        uiState.update { it.copy(filter = filter) }
        showPeople()
    }

    private fun toggle(personId: String) {
        toggledIds = if (personId in toggledIds) toggledIds - personId else toggledIds + personId
        showPeople()
    }

    // Why: only from Saldos on top, and only in the direction the row offers, so a stale tap never pays the wrong way.
    private fun openSettleUp(
        personId: String,
        owesYou: Boolean,
    ) {
        if (!isOnTop()) return
        val person = personOrNull(personId)?.takeIf { (it.netMinor > 0) == owesYou } ?: return
        navigator.navigate(
            Route.SettleUp(
                personKey = person.key,
                currency = person.currency,
            ),
        )
    }

    private fun openCharge(personId: String) {
        if (!isOnTop()) return
        val person = personOrNull(personId)?.takeIf { it.netMinor > 0 } ?: return
        uiState.update {
            it.copy(
                charge = chargeOf(
                    person,
                    previous = null,
                    pixKey = it.pixKey,
                ),
            )
        }
    }

    private fun chargeOf(
        person: PersonBalance,
        previous: ChargeDraft?,
        pixKey: PaymentKey?,
    ): ChargeDraft = ChargeDraft(
        personId = idOf(person),
        name = person.name,
        initials = person.name?.let(Initials::of).orEmpty(),
        color = person.face?.color,
        currency = person.currency,
        amountMinor = person.netMinor,
        groupNames = person.groups.map { it.groupName },
        isGuest = person.isGuest,
        guestBills = person.guestBills,
        guestPaidMinor = person.guestPaidMinor,
        includesPixKey = previous?.includesPixKey ?: (pixKey != null),
        editedMessage = previous?.editedMessage,
    )

    private fun updateCharge(change: (ChargeDraft) -> ChargeDraft) {
        uiState.update { state -> state.copy(charge = state.charge?.let(change)) }
    }

    private fun emitForCharge(action: GlobalBalancesUiAction) {
        if (uiState.value.charge == null) return
        viewModelScope.launch { uiAction.emit(action) }
    }

    private fun handleEmptyAction() {
        when (uiState.value.emptyState) {
            BalancesEmptyState.SETTLED -> backToGroups()
            BalancesEmptyState.NOTHING_TO_PAY -> changeFilter(BalancesFilter.OWES_YOU)
            BalancesEmptyState.NOTHING_TO_RECEIVE -> changeFilter(BalancesFilter.YOU_OWE)
            null -> Unit
        }
    }

    // Why: Grupos is the tab under Saldos, so going back to it also closes whatever Saldos opened over itself.
    private fun backToGroups() {
        val groupsIndex = navigator.backStack.value.indexOf(Route.Groups)
        if (groupsIndex >= 0) navigator.popTo(groupsIndex + 1)
    }

    private fun showSettledUp(settledUp: SettledUp) {
        consumeSettledUp(settledUp)
        noticeSettlementIds = settledUp.settlementIds
        uiState.update {
            it.copy(
                recordedPayment = PaymentNotice(
                    settlementId = settledUp.settlementIds.firstOrNull().orEmpty(),
                    fromName = settledUp.personName.takeUnless { settledUp.isPaidByMe },
                    fromIsMe = settledUp.isPaidByMe,
                    toName = settledUp.personName.takeIf { settledUp.isPaidByMe },
                ),
            )
        }
    }

    // Why: the payments of the settling up go back together, all or none; a failure keeps them all, which it says.
    private fun undoSettledUp() {
        if (uiState.value.recordedPayment == null) return
        val ids = noticeSettlementIds
        noticeSettlementIds = emptyList()
        uiState.update { it.copy(recordedPayment = null) }
        viewModelScope.launch {
            deleteSettlement(ids).onFailure { uiAction.emit(GlobalBalancesUiAction.ShowChangeError) }
        }
    }

    private fun personOrNull(personId: String): PersonBalance? = balances.people.firstOrNull { idOf(it) == personId }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: the default Pix key, as the profile marks it; Bitcoin keys never go in a message.
    private fun pixKeyOrNull(keys: List<PaymentKey>): PaymentKey? {
        val pix = keys.filter { it.type.method == PaymentMethod.PIX }
        return pix.firstOrNull { it.isDefault } ?: pix.firstOrNull()
    }

    private fun zeroTotalOf(currency: GroupCurrency): PeopleTotal = PeopleTotal(
        currency = currency,
        netMinor = 0L,
        toReceiveMinor = 0L,
        toPayMinor = 0L,
        toReceiveCount = 0,
        toPayCount = 0,
    )
}
