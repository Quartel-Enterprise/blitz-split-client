package com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.expenses.domain.RecordedPaymentNotices
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.expenses.domain.model.PersonBalance
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsByActivity
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettledUp
import com.quare.blitzsplit.core.groups.domain.model.UserBadge
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveHasGroups
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.navigation.openSearchOver
import com.quare.blitzsplit.core.navigation.popToGroups
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.globalbalances.presentation.mapper.idOf
import com.quare.blitzsplit.feature.globalbalances.presentation.mapper.rowsOf
import com.quare.blitzsplit.feature.globalbalances.presentation.model.BalancesEmptyState
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GLOBAL_BALANCES_SOURCE
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiAction
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiEvent
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import software.quare.connectivity.NetworkConnectivityObserver
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded
import software.quare.loadable.valueOrNull

class GlobalBalancesViewModel(
    private val route: Route.GlobalBalances,
    private val navigator: Navigator,
    consumeRecordedPayment: ConsumeRecordedPayment,
    deleteSettlement: DeleteSettlement,
    observePeopleBalances: ObservePeopleBalances,
    observeSettledUp: ObserveSettledUp,
    observeProfile: ObserveProfile,
    observeAccount: ObserveAccount,
    observeFirstDownload: ObserveFirstDownload,
    connectivityObserver: NetworkConnectivityObserver,
    observeHasGroups: ObserveHasGroups,
    observeGroupsByActivity: ObserveGroupsByActivity,
    trackEvent: TrackEvent,
) : TrackedViewModel<GlobalBalancesUiEvent>(trackEvent) {
    val uiState: StateFlow<GlobalBalancesUiState>
        field = MutableStateFlow(
            GlobalBalancesUiState(
                displayName = "",
                avatarInitial = "",
                avatarColor = AvatarColor.default,
                photoUrl = null,
                accountProvider = null,
                people = Loadable.Loading,
                totals = Loadable.Loading,
                isWaitingForInternet = false,
                filter = route.filter,
                isOnline = true,
                recordedPayment = null,
                pickedPersonId = null,
                canSearch = false,
                showsSplitBillButton = false,
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

    // Why: the people wait for Room's first answer and for the account's copy on this device to be whole.
    private var isBalancesRead = false

    private var firstDownload: FirstDownloadProgress? = null

    // Why: what a tap set each row's "Por grupo" to; the others open as the filter opens them.
    private var expandedById = emptyMap<String, Boolean>()

    private val notices = RecordedPaymentNotices(consumeRecordedPayment, deleteSettlement)

    // Why: the most recently used group, which "Rachar conta" marks in "Rachar em qual grupo?"; null with none.
    private var splitBillGroupId: String? = null

    init {
        combine(observePeopleBalances(), observeFirstDownload()) { people, download ->
            balances = people
            isBalancesRead = true
            firstDownload = download
            showPeople()
        }.launchIn(viewModelScope)
        combine(observeProfile(), observeAccount()) { profile, account ->
            val badge = UserBadge.of(profile, account.status)
            uiState.update {
                it.copy(
                    displayName = badge.displayName,
                    avatarInitial = badge.avatarInitial,
                    avatarColor = badge.avatarColor,
                    photoUrl = badge.photoUrl,
                    accountProvider = badge.accountProvider,
                )
            }
        }.launchIn(viewModelScope)
        observeHasGroups()
            .onEach { hasGroups -> uiState.update { it.copy(canSearch = hasGroups) } }
            .launchIn(viewModelScope)
        observeGroupsByActivity()
            .map { groups -> groups.firstOrNull()?.id }
            .distinctUntilChanged()
            .onEach { groupId ->
                splitBillGroupId = groupId
                uiState.update { it.copy(showsSplitBillButton = groupId != null) }
            }.launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
        /*
         * Why: Apple keeps Saldos while another tab is in front. Opened again by its route, it shows that route's
         * filter, so "A receber" or "Ver saldos" lands where it says even after a filter was changed here.
         */
        navigator.backStack
            .map { route in it }
            .distinctUntilChanged()
            .drop(1)
            .filter { isInBackStack -> isInBackStack }
            .onEach { changeFilter(route.filter) }
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

    override fun handleEvent(event: GlobalBalancesUiEvent) {
        when (event) {
            is GlobalBalancesUiEvent.OnFilterChanged -> if (event.filter != uiState.value.filter) {
                trackEvent(
                    "balances_filter_selected",
                    mapOf(
                        "filter" to event.filter,
                        "source" to GLOBAL_BALANCES_SOURCE,
                    ),
                )
                changeFilter(event.filter)
            }

            is GlobalBalancesUiEvent.OnPersonExpandToggled -> toggle(event.personId)

            /*
             * Why: only someone the filter lists, so a stale tap never picks a person a sync just settled, nor one the
             * filter hides, whom the pane would jump to once the filter changes.
             */
            is GlobalBalancesUiEvent.OnPersonSelected -> if (isListed(event.personId)) {
                trackEvent("balances_person_selected", emptyMap())
                uiState.update { it.copy(pickedPersonId = event.personId) }
            }

            is GlobalBalancesUiEvent.OnPersonClicked -> openProfile(event.personId)

            is GlobalBalancesUiEvent.OnGroupClicked -> openGroup(event.groupId)

            is GlobalBalancesUiEvent.OnSettleClicked -> openSettleUp(event.personId, owesYou = false)

            is GlobalBalancesUiEvent.OnReceivedClicked -> openSettleUp(event.personId, owesYou = true)

            is GlobalBalancesUiEvent.OnChargeClicked -> openCharge(event.personId)

            GlobalBalancesUiEvent.OnEmptyActionClicked -> handleEmptyAction()

            GlobalBalancesUiEvent.OnSplitBillClicked -> openSplitBill()

            GlobalBalancesUiEvent.OnHomeTabClicked, GlobalBalancesUiEvent.OnBrandClicked -> navigator.openHomeTab(
                Route.Home,
            )

            GlobalBalancesUiEvent.OnGroupsTabClicked -> navigator.popToGroups()

            GlobalBalancesUiEvent.OnContactsTabClicked -> if (isOnTop()) navigator.openHomeTab(Route.Contacts)

            GlobalBalancesUiEvent.OnProfileClicked -> if (isOnTop()) navigator.navigate(Route.Profile)

            GlobalBalancesUiEvent.OnSearchClicked -> if (uiState.value.canSearch) {
                if (navigator.openSearchOver(HomeTab.BALANCES)) {
                    trackEvent("search_opened", mapOf("source" to GLOBAL_BALANCES_SOURCE))
                }
            }

            GlobalBalancesUiEvent.OnUndoPaymentClicked -> undoSettledUp()

            GlobalBalancesUiEvent.OnRecordedPaymentNoticeShown -> uiState.update { it.copy(recordedPayment = null) }
        }
    }

    private fun showPeople() {
        val download = firstDownload
        if (!isBalancesRead || download != null) {
            uiState.update {
                it.copy(
                    people = Loadable.Loading,
                    totals = Loadable.Loading,
                    isWaitingForInternet = download?.isWaitingForInternet == true,
                )
            }
            return
        }
        val state = uiState.value
        val rows = rowsOf(balances.people, state.filter, expandedById)
        uiState.update {
            it.copy(
                people = rows.toLoaded(),
                totals = balances.totals.ifEmpty { listOf(zeroTotalOf(GroupCurrency.fallback)) }.toLoaded(),
                isWaitingForInternet = false,
            )
        }
    }

    // Why: a new filter opens its lone person's "Por grupo" afresh.
    private fun changeFilter(filter: BalancesFilter) {
        if (filter == uiState.value.filter) return
        expandedById = emptyMap()
        uiState.update { it.copy(filter = filter) }
        showPeople()
    }

    private fun toggle(personId: String) {
        val row = uiState.value.people
            .valueOrNull()
            ?.firstOrNull { it.id == personId } ?: return
        trackEvent("balances_person_expand_toggled", mapOf("is_expanded" to !row.isExpanded))
        expandedById = expandedById + (personId to !row.isExpanded)
        showPeople()
    }

    // Why: the person's profile, the same Contatos opens; someone no longer in the group has none.
    private fun openProfile(personId: String) {
        if (!isOnTop()) return
        val row = uiState.value.people
            .valueOrNull()
            ?.firstOrNull { it.id == personId }
            ?.takeIf { it.opensProfile } ?: return
        navigator.navigate(Route.ContactProfile(row.personKey))
    }

    // Why: only a group someone's "Por grupo" still lists, so a stale tap never opens one a sync just took away.
    private fun openGroup(groupId: String) {
        if (!isOnTop()) return
        val rows = uiState.value.people
            .valueOrNull()
            .orEmpty()
        if (rows.none { row -> row.groups.any { it.groupId == groupId } }) return
        navigator.navigate(
            Route.GroupDetail(
                groupId,
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
        )
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

    // Why: Cobrar over Saldos, its own route, in the currency the row shows.
    private fun openCharge(personId: String) {
        if (!isOnTop()) return
        val person = personOrNull(personId)?.takeIf { it.netMinor > 0 } ?: return
        navigator.navigate(
            Route.Charge(
                personKey = person.key,
                currency = person.currency,
            ),
        )
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

    // Why: "Rachar em qual grupo?" first (7.1i), the most recently used group marked, as Início's button asks.
    private fun openSplitBill() {
        val groupId = splitBillGroupId ?: return
        if (!isOnTop()) return
        trackEvent("split_bill_clicked", mapOf("source" to GLOBAL_BALANCES_SOURCE))
        navigator.navigate(
            Route.SplitBill(
                groupId = groupId,
                expenseId = null,
                picksGroupFirst = true,
                scansReceipt = false,
            ),
        )
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
