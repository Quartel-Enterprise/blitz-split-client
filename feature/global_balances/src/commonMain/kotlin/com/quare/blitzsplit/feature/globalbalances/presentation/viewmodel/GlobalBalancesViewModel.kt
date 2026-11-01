package com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.expenses.domain.RecordedPaymentNotices
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.expenses.domain.model.PersonBalance
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
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
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.navigation.popToGroups
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
    consumeRecordedPayment: ConsumeRecordedPayment,
    deleteSettlement: DeleteSettlement,
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
                pickedPersonId = null,
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

    // Why: Contatos' "Cobrar" opens the message once, as soon as Room is read.
    private var isRouteChargeOpened = false

    // Why: what a tap set each row's "Por grupo" to; the others open as the filter opens them.
    private var expandedById = emptyMap<String, Boolean>()

    private val notices = RecordedPaymentNotices(consumeRecordedPayment, deleteSettlement)

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
            .onEach { key ->
                uiState.update {
                    it.copy(
                        pixKey = key,
                        charge = it.charge?.copy(hasPixKey = key != null),
                    )
                }
            }.launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
        notices.watchSettledUp(
            scope = viewModelScope,
            recorded = observeSettledUp(),
            backStack = navigator.backStack,
            isLoaded = uiState.map { it.isLoaded },
            isOnTop = ::isOnTop,
            show = { notice -> uiState.update { it.copy(recordedPayment = notice) } },
        )
    }

    fun onEvent(event: GlobalBalancesUiEvent) {
        when (event) {
            is GlobalBalancesUiEvent.OnFilterChanged -> changeFilter(event.filter)

            is GlobalBalancesUiEvent.OnPersonExpandToggled -> toggle(event.personId)

            /*
             * Why: only someone the filter lists, so a stale tap never picks a person a sync just settled, nor one the
             * filter hides, whom the pane would jump to once the filter changes.
             */
            is GlobalBalancesUiEvent.OnPersonSelected -> if (isListed(event.personId)) {
                uiState.update { it.copy(pickedPersonId = event.personId) }
            }

            is GlobalBalancesUiEvent.OnSettleClicked -> openSettleUp(event.personId, owesYou = false)

            is GlobalBalancesUiEvent.OnReceivedClicked -> openSettleUp(event.personId, owesYou = true)

            is GlobalBalancesUiEvent.OnChargeClicked -> openCharge(event.personId)

            is GlobalBalancesUiEvent.OnChargePixKeyToggled -> updateCharge { charge ->
                if (charge.canTogglePixKey) charge.copy(pixKeyChoice = event.isIncluded) else charge
            }

            // Why: the switch locks as it is once the message is the person's, whatever key loads later.
            is GlobalBalancesUiEvent.OnChargeMessageChanged -> updateCharge {
                it.copy(
                    editedMessage = event.message,
                    pixKeyChoice = it.includesPixKey,
                )
            }

            is GlobalBalancesUiEvent.OnChargeShareClicked -> emitForCharge(
                GlobalBalancesUiAction.ShareMessage(event.message),
            )

            is GlobalBalancesUiEvent.OnChargeCopyClicked -> emitForCharge(
                GlobalBalancesUiAction.CopyMessage(event.message),
            )

            GlobalBalancesUiEvent.OnChargeDismissed -> dismissCharge()

            GlobalBalancesUiEvent.OnEmptyActionClicked -> handleEmptyAction()

            GlobalBalancesUiEvent.OnHomeTabClicked -> navigator.openHomeTab(Route.Home)

            GlobalBalancesUiEvent.OnGroupsTabClicked -> navigator.popToGroups()

            GlobalBalancesUiEvent.OnContactsTabClicked -> if (isOnTop()) navigator.openHomeTab(Route.Contacts)

            GlobalBalancesUiEvent.OnActivityTabClicked ->
                viewModelScope.launch { uiAction.emit(GlobalBalancesUiAction.ShowComingSoon) }

            GlobalBalancesUiEvent.OnProfileClicked -> if (isOnTop()) navigator.navigate(Route.Profile)

            GlobalBalancesUiEvent.OnUndoPaymentClicked -> undoSettledUp()

            GlobalBalancesUiEvent.OnRecordedPaymentNoticeShown -> uiState.update { it.copy(recordedPayment = null) }
        }
    }

    private fun showPeople() {
        val state = uiState.value
        val rows = rowsOf(balances.people, state.filter, expandedById)
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
        openRouteChargeOnce()
    }

    // Why: from Contatos' "Cobrar", the person's message right away, in the currency the profile showed.
    private fun openRouteChargeOnce() {
        val charge = route.charge ?: return
        if (isRouteChargeOpened) return
        isRouteChargeOpened = true
        val person = balances.findPersonOrNull(charge.personKey, charge.currency.code)?.takeIf { it.netMinor > 0 }
        if (person == null) {
            // Why: nothing to charge any more (a sync settled it): back to the profile rather than a stray Saldos.
            if (isOnTop()) navigator.navigateBack()
            return
        }
        openCharge(idOf(person))
    }

    // Why: opened for Contatos' "Cobrar", Saldos is only that message: closing it goes back to the profile.
    private fun dismissCharge() {
        uiState.update { it.copy(charge = null) }
        if (route.charge != null && isOnTop()) navigator.navigateBack()
    }

    // Why: a new filter opens its lone person's "Por grupo" afresh.
    private fun changeFilter(filter: BalancesFilter) {
        if (filter == uiState.value.filter) return
        expandedById = emptyMap()
        uiState.update { it.copy(filter = filter) }
        showPeople()
    }

    private fun toggle(personId: String) {
        val row = uiState.value.people.firstOrNull { it.id == personId } ?: return
        expandedById = expandedById + (personId to !row.isExpanded)
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
        hasPixKey = pixKey != null,
        pixKeyChoice = previous?.pixKeyChoice,
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
            BalancesEmptyState.SETTLED -> navigator.popToGroups()
            BalancesEmptyState.NOTHING_TO_PAY -> changeFilter(BalancesFilter.OWES_YOU)
            BalancesEmptyState.NOTHING_TO_RECEIVE -> changeFilter(BalancesFilter.YOU_OWE)
            null -> Unit
        }
    }

    // Why: the payments of the settling up go back together, all or none; a failure keeps them all, which it says.
    private fun undoSettledUp() {
        val notice = uiState.value.recordedPayment ?: return
        uiState.update { it.copy(recordedPayment = null) }
        notices.delete(viewModelScope, notice.settlementIds) { uiAction.emit(GlobalBalancesUiAction.ShowChangeError) }
    }

    private fun personOrNull(personId: String): PersonBalance? = balances.people.firstOrNull { idOf(it) == personId }

    private fun isListed(personId: String): Boolean = uiState.value.sections.any { section ->
        section.rows.any { it.id == personId }
    }

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
