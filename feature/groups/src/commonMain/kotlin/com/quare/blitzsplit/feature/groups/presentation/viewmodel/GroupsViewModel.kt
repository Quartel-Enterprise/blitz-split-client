package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.expenses.domain.RecordedPaymentNotices
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveRecordedPayment
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ConsumeGroupDeletion
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroupDeletions
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.groupBesideGroupsOrNull
import com.quare.blitzsplit.core.navigation.openBesideGroups
import com.quare.blitzsplit.core.navigation.popToGroups
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveGroupsLayout
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateGroupsLayout
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.sync.exit.GroupExitController
import com.quare.blitzsplit.core.sync.exit.GroupExitTarget
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.groups.presentation.mapper.ActivityClock
import com.quare.blitzsplit.feature.groups.presentation.mapper.GroupWithMoney
import com.quare.blitzsplit.feature.groups.presentation.mapper.GroupsOnHome
import com.quare.blitzsplit.feature.groups.presentation.mapper.agedRowsOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.pendingChangeCountOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.rowsOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.searchResultsOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.summaryOf
import com.quare.blitzsplit.feature.groups.presentation.model.DeletedGroupNotice
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearch
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSummary
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiState
import com.quare.blitzsplit.feature.groups.presentation.model.SyncBadge
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class GroupsViewModel(
    private val retrySync: RetrySync,
    private val syncNow: SyncNow,
    private val navigator: Navigator,
    private val consumeGroupDeletion: ConsumeGroupDeletion,
    private val updateGroupsLayout: UpdateGroupsLayout,
    leaveGroup: LeaveGroup,
    deleteGroup: DeleteGroup,
    observeRecordedPayment: ObserveRecordedPayment,
    consumeRecordedPayment: ConsumeRecordedPayment,
    deleteSettlement: DeleteSettlement,
    currentTimeMillis: CurrentTimeMillis,
    currentTimeZone: CurrentTimeZone,
    minuteTicks: MinuteTicks,
    observeProfile: ObserveProfile,
    observeAccount: ObserveAccount,
    observeSyncState: ObserveSyncState,
    observeGroups: ObserveGroups,
    observeGroupsMoney: ObserveGroupsMoney,
    connectivityObserver: NetworkConnectivityObserver,
    observeGroupDeletions: ObserveGroupDeletions,
    observeGroupsLayout: ObserveGroupsLayout,
    observePeopleBalances: ObservePeopleBalances,
) : ViewModel() {
    val uiState: StateFlow<GroupsUiState>
        field = MutableStateFlow(
            GroupsUiState(
                displayName = "",
                avatarInitial = "",
                avatarColor = AvatarColor.default,
                photoUrl = null,
                accountProvider = null,
                syncBadge = null,
                pendingChangeCount = 0,
                showsRecoveryCodePrompt = false,
                groups = emptyList(),
                hasLoadedGroups = false,
                summary = GroupsSummary(totals = emptyList()),
                showsSettledGroups = false,
                filterQuery = "",
                isRefreshing = false,
                deletedGroupNotice = null,
                exit = GroupExitState.idle,
                exitGroupId = null,
                layout = GroupsLayout.initial,
                recordedPayment = null,
                search = GroupsSearch.closed,
                selectedGroupId = null,
            ),
        )

    val uiAction: SharedFlow<GroupsUiAction>
        field = MutableSharedFlow<GroupsUiAction>()

    val stateWatcher: FlowWatcher<GroupsUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<GroupsUiAction> = FlowWatcher(uiAction)

    // Why: the deletions no screen showed yet, so the next one shows once the current snackbar goes.
    private var pendingDeletions = emptyList<GroupDeletion>()

    // Why: what the dialogs of "Sair do grupo" and "Apagar grupo" read of the group asked for, as Room has it now.
    private var groupsWithMoney = emptyList<GroupWithMoney>()

    // Why: whom "A pagar" settles up with straight away, as the global Saldos has them.
    private var peopleBalances = PeopleBalances(
        people = emptyList(),
        totals = emptyList(),
        me = null,
    )

    private val notices = RecordedPaymentNotices(consumeRecordedPayment, deleteSettlement)

    // Why: the group drawn beside the list (list and detail), as the UI's window decides; a phone never does.
    private val detailPane = GroupsDetailPane(navigator)

    // Why: leaving or deleting from a row stays on the home, which the group just leaves.
    private val exit = GroupExitController(
        leaveGroup = leaveGroup,
        deleteGroup = deleteGroup,
        scope = viewModelScope,
        openRegisterPayment = { groupId, fromMemberId, toMemberId ->
            open(
                Route.RegisterPayment(
                    groupId = groupId,
                    fromMemberId = fromMemberId,
                    toMemberId = toMemberId,
                ),
            )
        },
        // Why: a group's Saldos is a tab of the group, which takes the place of the group beside the list.
        openBalances = { groupId -> if (isListShown()) navigator.openBesideGroups(Route.Balances(groupId)) },
        // Why: the group left the device, so the pane beside the list no longer shows it.
        onExited = {
            if (navigator.backStack.value.groupBesideGroupsOrNull() == uiState.value.exitGroupId) {
                navigator.popToGroups()
            }
        },
        showError = { isDelete ->
            uiAction.emit(if (isDelete) GroupsUiAction.ShowDeleteError else GroupsUiAction.ShowLeaveError)
        },
    )

    init {
        combine(
            observeProfile(),
            observeAccount(),
            observeSyncState(),
            connectivityObserver.observe(),
        ) { profile, account, syncState, isOnline ->
            exit.setOnline(isOnline)
            val displayName = profile?.displayName?.trim().orEmpty()
            // Why: only the fields these flows own, so the groups' own flow below keeps what it set.
            val update: (GroupsUiState) -> GroupsUiState = { state ->
                state.copy(
                    displayName = displayName,
                    avatarInitial = Initials.firstOf(displayName),
                    avatarColor = profile?.avatarColor ?: AvatarColor.default,
                    photoUrl = profile?.photo?.toImageUrl(),
                    accountProvider = (account.status as? AccountStatus.Connected)?.provider,
                    syncBadge = syncBadgeOf(
                        syncState = syncState,
                        isOnline = isOnline,
                    ),
                    showsRecoveryCodePrompt = account.recoveryCodePrompt.isShown,
                )
            }
            update
        }.onEach(uiState::update)
            .launchIn(viewModelScope)
        val activityClock = {
            ActivityClock(
                nowMillis = currentTimeMillis(),
                timeZone = currentTimeZone(),
            )
        }
        // Why: each group's bills and payments, again whenever any of them changes here or through a sync.
        combine(observeGroups(), observeGroupsMoney()) { groups, money ->
            groups.map { group ->
                val ofGroup = money[group.id] ?: GroupMoney.NONE
                GroupWithMoney(
                    group = group,
                    expenses = ofGroup.expenses,
                    settlements = ofGroup.settlements,
                    pendingDeletionCount = ofGroup.pendingDeletionCount,
                )
            }
        }.onEach { groups ->
            groupsWithMoney = groups
            // Why: only a dialog showing, or the server answering for it, reads the group; asking again rebuilds it.
            if (exit.state.value.dialog != null) exit.update(exitTargetOrNull(uiState.value.exitGroupId))
        }.map { groups ->
            GroupsOnHome(
                rows = rowsOf(
                    groups = groups,
                    clock = activityClock(),
                ),
                pendingChangeCount = pendingChangeCountOf(groups),
            )
        }
            // Why: the ages again each minute, not the balances, so a home left open says "ontem" after midnight.
            .combine(minuteTicks()) { home, _ -> home.copy(rows = agedRowsOf(home.rows, activityClock())) }
            .onEach { home ->
                uiState.update {
                    it.copy(
                        groups = home.rows,
                        hasLoadedGroups = true,
                        summary = summaryOf(home.rows),
                        pendingChangeCount = home.pendingChangeCount,
                        /*
                         * Why: what the search found changes with the device's data, a sync's included. Once the last
                         * group goes the search closes, so the next group doesn't bring back a forgotten search.
                         */
                        search = if (home.rows.isEmpty()) GroupsSearch.closed else searchedOf(it.search, home.rows),
                    )
                }
            }.launchIn(viewModelScope)
        exit.state
            .onEach { exit -> uiState.update { it.copy(exit = exit) } }
            .launchIn(viewModelScope)
        observeGroupsLayout()
            .onEach { layout -> uiState.update { it.copy(layout = layout) } }
            .launchIn(viewModelScope)
        observePeopleBalances()
            .onEach { people -> peopleBalances = people }
            .launchIn(viewModelScope)
        // Why: only the group a row's "Acertar" was about can owe the home a notice; a group's own screens show theirs.
        notices.watch(
            scope = viewModelScope,
            recorded = uiState
                .map { it.exitGroupId }
                .distinctUntilChanged()
                .flatMapLatest { groupId -> groupId?.let(observeRecordedPayment::invoke) ?: flowOf(null) },
            backStack = navigator.backStack,
            isLoaded = uiState.map { it.hasLoadedGroups },
            // Why: the group beside the list shows its own notice, so the home leaves that group's to it.
            isOnTop = { isListShown() && paneGroupIdOrNull() != uiState.value.exitGroupId },
            meId = { exitGroupOrNull()?.members?.firstOrNull { it.isMe }?.id },
            nameOf = { id -> exitGroupOrNull()?.members?.firstOrNull { it.id == id }?.name },
            show = { notice -> uiState.update { it.copy(recordedPayment = notice) } },
        )
        // Why: only while the home is on top; a deleted group still open shows its own "Grupo apagado".
        combine(observeGroupDeletions(), navigator.backStack, ::Pair)
            .onEach { (deletions, _) ->
                pendingDeletions = deletions
                showNextDeletion()
            }.launchIn(viewModelScope)
        navigator.backStack
            .map { stack -> stack.groupBesideGroupsOrNull() }
            .distinctUntilChanged()
            .onEach { groupId -> uiState.update { it.copy(selectedGroupId = groupId) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: GroupsUiEvent) {
        when (event) {
            GroupsUiEvent.OnRetrySyncClicked -> if (uiState.value.canRetrySync) retrySync()

            GroupsUiEvent.OnProfileClicked -> navigator.navigate(Route.Profile)

            // Why: the list may just have come in sight, so a deletion waiting for it shows now.
            is GroupsUiEvent.OnDetailPaneChanged -> {
                detailPane.change(event.isShown, uiState.value.search)
                showNextDeletion()
            }

            GroupsUiEvent.OnCreateGroupClicked -> navigator.navigate(Route.CreateGroup)

            /*
             * Why: only while the list shows, so a quick tap on a second row of a phone doesn't push its group over the
             * first's; beside the list, the group tapped takes the place of the one shown.
             */
            is GroupsUiEvent.OnGroupClicked -> if (isListShown()) {
                navigator.openBesideGroups(Route.GroupDetail(event.groupId))
            }

            GroupsUiEvent.OnJoinWithLinkClicked -> if (isListShown()) {
                navigator.navigate(
                    Route.JoinWithLink(
                        skippedCode = null,
                        skippedMemberCode = null,
                    ),
                )
            }

            is GroupsUiEvent.OnInviteClicked -> open(Route.Invite(event.groupId, isNewGroup = false))

            is GroupsUiEvent.OnLeaveGroupClicked -> askToExit(event.groupId, GroupExitController::askToLeave)

            is GroupsUiEvent.OnDeleteGroupClicked -> askToExit(event.groupId, GroupExitController::askToDelete)

            GroupsUiEvent.OnGroupExitConfirmed -> exit.confirm()

            GroupsUiEvent.OnGroupExitDismissed -> exit.dismiss()

            GroupsUiEvent.OnDeletedGroupNoticeShown -> {
                uiState.update { it.copy(deletedGroupNotice = null) }
                showNextDeletion()
            }

            GroupsUiEvent.OnUndoPaymentClicked -> undoRecordedPayment()

            GroupsUiEvent.OnRecordedPaymentNoticeShown -> uiState.update { it.copy(recordedPayment = null) }

            GroupsUiEvent.OnRefresh -> refresh()

            GroupsUiEvent.OnSplitBillClicked -> openSplitBill()

            // Why: only from the groups on top, so a late tap never closes a screen over them, a half-typed bill's.
            GroupsUiEvent.OnToReceiveClicked -> if (isListShown() && uiState.value.summary.canOpenToReceive) {
                openTab(toBalancesTab(BalancesFilter.OWES_YOU))
            }

            GroupsUiEvent.OnToPayClicked -> if (uiState.value.summary.canOpenToPay) openToPay()

            is GroupsUiEvent.OnShowSettledChanged -> uiState.update { it.copy(showsSettledGroups = event.isShown) }

            is GroupsUiEvent.OnFilterChanged -> uiState.update { it.copy(filterQuery = event.query) }

            is GroupsUiEvent.OnDisplayChanged -> changeDisplay(event.display)

            is GroupsUiEvent.OnGridSizeChanged -> changeGridSize(event.size)

            GroupsUiEvent.OnSearchOpened -> {
                openSearch()
                detailPane.preview(uiState.value.search)
            }

            GroupsUiEvent.OnSearchClosed -> closeSearch()

            GroupsUiEvent.OnSearchTabLeft -> leaveSearchTab()

            is GroupsUiEvent.OnSearchQueryChanged -> {
                search { it.copy(query = event.query) }
                detailPane.preview(uiState.value.search)
            }

            is GroupsUiEvent.OnSearchScopeChanged -> {
                search { it.copy(scope = event.scope) }
                detailPane.preview(uiState.value.search)
            }

            // Why: Membros is a tab of the group, which takes the place of the group beside the list.
            is GroupsUiEvent.OnSearchPersonClicked -> if (isListShown()) {
                navigator.openBesideGroups(Route.Members(event.groupId, joinedAs = null))
            }

            is GroupsUiEvent.OnSearchBillClicked -> openBill(event.groupId, event.expenseId)

            GroupsUiEvent.OnBalancesTabClicked -> openTab(toBalancesTab(BalancesFilter.ALL))

            GroupsUiEvent.OnGroupsTabClicked -> navigator.popToGroups()

            GroupsUiEvent.OnContactsTabClicked -> openTab(Route.Contacts)

            GroupsUiEvent.OnActivityTabClicked -> showComingSoon()
        }
    }

    /*
     * Why: there is nothing to look in before the first group, and only from the groups on top, so ⌘K over a dialog
     * on the web or iOS's search tab over a pushed screen doesn't open a search whose close would pop that screen.
     */
    private fun openSearch() {
        if (!uiState.value.canSearch || !isListShown()) return
        uiState.update { it.copy(search = searchedOf(it.search.copy(isOpen = true), it.groups)) }
    }

    // Why: closing forgets what was typed.
    private fun closeSearch() {
        detailPane.forgetPreview()
        uiState.update { it.copy(search = GroupsSearch.closed) }
    }

    // Why: what iOS's search tab opened goes with it, back to the groups.
    private fun leaveSearchTab() {
        closeSearch()
        navigator.popToGroups()
    }

    // Why: what is typed or picked only counts while the search shows, so a late keystroke can't reopen it.
    private fun search(change: (GroupsSearch) -> GroupsSearch) {
        if (!uiState.value.search.isOpen) return
        uiState.update { it.copy(search = searchedOf(change(it.search), it.groups)) }
    }

    private fun searchedOf(
        search: GroupsSearch,
        rows: List<GroupRow>,
    ): GroupsSearch = search.copy(
        results = searchResultsOf(
            groups = groupsWithMoney,
            rows = rows,
            query = search.query,
            scope = search.scope,
        ),
    )

    // Why: as a group's Despesas opens it, over the group, which takes the place of the group beside the list.
    private fun openBill(
        groupId: String,
        expenseId: String,
    ) {
        if (!isListShown()) return
        navigator.openBesideGroups(Route.GroupDetail(groupId))
        navigator.navigate(Route.BillDetail(groupId, expenseId))
    }

    private fun changeDisplay(display: GroupsDisplay) {
        saveLayout(uiState.value.layout.copy(display = display))
    }

    // Why: the size is disabled while the list is picked, so a late tap on it doesn't change the grid unseen.
    private fun changeGridSize(size: GroupGridSize) {
        if (!uiState.value.canPickGridSize) return
        saveLayout(uiState.value.layout.copy(gridSize = size))
    }

    /*
     * Why: the state takes the pick at once, so a second pick before the device stored the first (the Mac's slider
     * dragged back and forth) builds on it instead of on the stale layout; the stored layout then confirms it.
     */
    private fun saveLayout(layout: GroupsLayout) {
        if (layout == uiState.value.layout) return
        uiState.update { it.copy(layout = layout) }
        viewModelScope.launch { updateGroupsLayout(layout) }
    }

    // Why: Saldos and Contatos, drawn over the groups, first close a screen the groups opened, as a tab bar does.
    private fun openTab(tab: Route) {
        val stack = navigator.backStack.value
        val groupsIndex = stack.indexOf(Route.Groups)
        if (groupsIndex < 0 || stack.getOrNull(groupsIndex + 1)?.let { it::class } == tab::class) return
        closeSearch()
        navigator.popToGroups()
        navigator.navigate(tab)
    }

    private fun toBalancesTab(filter: BalancesFilter): Route = Route.GlobalBalances(filter, chargePersonKey = null)

    // Why: "A pagar" with a single person to pay goes straight to settling up with them (4.5), over Você deve.
    private fun openToPay() {
        if (!isListShown()) return
        openTab(toBalancesTab(BalancesFilter.YOU_OWE))
        val person = peopleBalances.people.singleOrNull { it.netMinor < 0 } ?: return
        navigator.navigate(
            Route.SettleUp(
                personKey = person.key,
                currency = person.currency,
            ),
        )
    }

    private fun showComingSoon() {
        viewModelScope.launch { uiAction.emit(GroupsUiAction.ShowComingSoon) }
    }

    /*
     * Why: with no group yet "Rachar conta" makes one first, as the empty home does. Otherwise it asks which group
     * (7.1i), the group beside the list marked, else the most recently used one: the first row, since the rows go by
     * their last activity.
     */
    private fun openSplitBill() {
        // Why: before Room answers, no group yet doesn't mean there is none.
        if (!isListShown() || !uiState.value.hasLoadedGroups) return
        val groups = uiState.value.groups
        val recent = groups.firstOrNull { it.id == paneGroupIdOrNull() } ?: groups.firstOrNull()
        if (recent == null) {
            navigator.navigate(Route.CreateGroup)
        } else {
            navigator.navigate(
                Route.SplitBill(
                    groupId = recent.id,
                    expenseId = null,
                    picksGroupFirst = true,
                ),
            )
        }
    }

    private fun refresh() {
        if (uiState.value.isRefreshing) return
        // Why: offline, a pull to refresh ends at once instead of waiting for a sync that can't reach the server.
        if (!exit.state.value.isOnline) return
        uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            // Why: a failure shows as the sync badge's "Sem resposta do servidor", so the result is not read here.
            syncNow()
            uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun isListShown(): Boolean = detailPane.isListShown

    private fun paneGroupIdOrNull(): String? = detailPane.groupIdOrNull

    // Why: only while the list shows, as a row's tap; it opens over the group beside the list too.
    private fun open(route: Route) {
        if (isListShown()) navigator.navigate(route)
    }

    // Why: a request in flight keeps its group; offline the menu shows both disabled, so the controller does nothing.
    private fun askToExit(
        groupId: String,
        ask: GroupExitController.() -> Unit,
    ) {
        if (!uiState.value.exit.canLeaveOrDelete || !isListShown()) return
        uiState.update { it.copy(exitGroupId = groupId) }
        exit.update(exitTargetOrNull(groupId))
        exit.ask()
    }

    private fun undoRecordedPayment() {
        val notice = uiState.value.recordedPayment ?: return
        uiState.update { it.copy(recordedPayment = null) }
        notices.delete(viewModelScope, notice.settlementIds) { uiAction.emit(GroupsUiAction.ShowChangeError) }
    }

    private fun exitGroupOrNull(): Group? = groupsWithMoney
        .firstOrNull { it.group.id == uiState.value.exitGroupId }
        ?.group

    private fun exitTargetOrNull(groupId: String?): GroupExitTarget? {
        val item = groupsWithMoney.firstOrNull { it.group.id == groupId } ?: return null
        return GroupExitTarget(
            group = item.group,
            balances = GroupBalances.of(item.expenses, item.settlements),
            billCount = item.expenses.size,
        )
    }

    // Why: the group beside the list says "Grupo apagado" itself, so the home leaves its deletion to it.
    private fun showNextDeletion() {
        val paneGroupId = paneGroupIdOrNull()
        val deletion = pendingDeletions.firstOrNull { it.groupId != paneGroupId } ?: return
        if (!isListShown() || uiState.value.deletedGroupNotice != null) return
        consumeGroupDeletion(deletion.groupId)
        pendingDeletions = pendingDeletions - deletion
        uiState.update {
            it.copy(
                deletedGroupNotice = DeletedGroupNotice(
                    groupId = deletion.groupId,
                    groupName = deletion.groupName,
                    deletedByName = deletion.deletedByName,
                ),
            )
        }
    }

    // Why: online, a queued change is about to go, since the coordinator syncs as soon as one is queued.
    private fun syncBadgeOf(
        syncState: SyncState,
        isOnline: Boolean,
    ): SyncBadge? = when {
        syncState == SyncState.SYNCED -> null
        !isOnline -> SyncBadge.OFFLINE
        syncState == SyncState.FAILED -> SyncBadge.FAILED
        else -> SyncBadge.SYNCING
    }
}
