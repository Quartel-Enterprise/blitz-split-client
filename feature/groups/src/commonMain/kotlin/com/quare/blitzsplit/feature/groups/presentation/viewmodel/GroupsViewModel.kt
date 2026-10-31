package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitUiState
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ConsumeGroupDeletion
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroupDeletions
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
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
import com.quare.blitzsplit.feature.groups.presentation.mapper.summaryOf
import com.quare.blitzsplit.feature.groups.presentation.model.DeletedGroupNotice
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
                exit = GroupExitUiState.idle,
                exitGroupId = null,
                layout = GroupsLayout.initial,
            ),
        )

    val uiAction: SharedFlow<GroupsUiAction>
        field = MutableSharedFlow<GroupsUiAction>()

    val stateWatcher: FlowWatcher<GroupsUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<GroupsUiAction> = FlowWatcher(uiAction)

    // Why: the deletions no screen showed yet, so the next one shows once the current snackbar goes.
    private var pendingDeletions = emptyList<GroupDeletion>()

    // Why: offline, a pull to refresh ends at once instead of waiting for a sync that can't reach the server.
    private var isOnline = true

    // Why: what the dialogs of "Sair do grupo" and "Apagar grupo" read of the group asked for, as Room has it now.
    private var groupsWithMoney = emptyList<GroupWithMoney>()

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
        openBalances = { groupId -> open(Route.Balances(groupId)) },
        onExited = {},
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
            this.isOnline = isOnline
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
            exit.update(exitTargetOrNull(uiState.value.exitGroupId))
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
                    )
                }
            }.launchIn(viewModelScope)
        exit.state
            .onEach { exit -> uiState.update { it.copy(exit = exit) } }
            .launchIn(viewModelScope)
        observeGroupsLayout()
            .onEach { layout -> uiState.update { it.copy(layout = layout) } }
            .launchIn(viewModelScope)
        // Why: only while the home is on top; a deleted group still open shows its own "Grupo apagado".
        combine(observeGroupDeletions(), navigator.backStack, ::Pair)
            .onEach { (deletions, _) ->
                pendingDeletions = deletions
                showNextDeletion()
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: GroupsUiEvent) {
        when (event) {
            GroupsUiEvent.OnRetrySyncClicked -> if (uiState.value.canRetrySync) retrySync()

            GroupsUiEvent.OnProfileClicked -> navigator.navigate(Route.Profile)

            GroupsUiEvent.OnCreateGroupClicked -> navigator.navigate(Route.CreateGroup)

            // Why: only from the groups on top, so a quick tap on a second row doesn't push its group over the first's.
            is GroupsUiEvent.OnGroupClicked -> if (isOnTop()) navigator.navigate(Route.GroupDetail(event.groupId))

            GroupsUiEvent.OnJoinWithLinkClicked -> if (isOnTop()) {
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

            GroupsUiEvent.OnRefresh -> refresh()

            GroupsUiEvent.OnSplitBillClicked -> openSplitBill()

            // Why: the global Saldos (flow 8) opens already filtered once it exists; until then, "Em breve".
            GroupsUiEvent.OnToReceiveClicked -> if (uiState.value.summary.canOpenToReceive) showComingSoon()

            GroupsUiEvent.OnToPayClicked -> if (uiState.value.summary.canOpenToPay) showComingSoon()

            is GroupsUiEvent.OnShowSettledChanged -> uiState.update { it.copy(showsSettledGroups = event.isShown) }

            is GroupsUiEvent.OnFilterChanged -> uiState.update { it.copy(filterQuery = event.query) }

            is GroupsUiEvent.OnDisplayChanged -> changeDisplay(event.display)

            is GroupsUiEvent.OnGridSizeChanged -> changeGridSize(event.size)

            GroupsUiEvent.OnBalancesTabClicked,
            GroupsUiEvent.OnContactsTabClicked,
            GroupsUiEvent.OnActivityTabClicked,
            -> showComingSoon()
        }
    }

    private fun changeDisplay(display: GroupsDisplay) {
        saveLayout(uiState.value.layout.copy(display = display))
    }

    // Why: the size is disabled while the list is picked, so a late tap on it doesn't change the grid unseen.
    private fun changeGridSize(size: GroupGridSize) {
        if (!uiState.value.canPickGridSize) return
        saveLayout(uiState.value.layout.copy(gridSize = size))
    }

    // Why: the state follows what the device stored, the one place every screen reads it from.
    private fun saveLayout(layout: GroupsLayout) {
        if (layout == uiState.value.layout) return
        viewModelScope.launch { updateGroupsLayout(layout) }
    }

    private fun showComingSoon() {
        viewModelScope.launch { uiAction.emit(GroupsUiAction.ShowComingSoon) }
    }

    // Why: with no group yet "Rachar conta" makes one first, as the empty home does; picking a group (7.1i) comes later.
    private fun openSplitBill() {
        if (!isOnTop()) return
        if (uiState.value.groups.isEmpty()) navigator.navigate(Route.CreateGroup) else showComingSoon()
    }

    private fun refresh() {
        if (uiState.value.isRefreshing) return
        if (!isOnline) return
        uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            // Why: a failure shows as the sync badge's "Sem resposta do servidor", so the result is not read here.
            syncNow()
            uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == Route.Groups

    // Why: only from the groups on top, as a row's tap.
    private fun open(route: Route) {
        if (isOnTop()) navigator.navigate(route)
    }

    // Why: a request in flight keeps its group; offline the menu shows both disabled, so the controller does nothing.
    private fun askToExit(
        groupId: String,
        ask: GroupExitController.() -> Unit,
    ) {
        if (!uiState.value.exit.canLeaveOrDelete || !isOnTop()) return
        uiState.update { it.copy(exitGroupId = groupId) }
        exit.update(exitTargetOrNull(groupId))
        exit.ask()
    }

    private fun exitTargetOrNull(groupId: String?): GroupExitTarget? {
        val item = groupsWithMoney.firstOrNull { it.group.id == groupId } ?: return null
        return GroupExitTarget(
            group = item.group,
            balances = GroupBalances.of(item.expenses, item.settlements),
            billCount = item.expenses.size,
        )
    }

    private fun showNextDeletion() {
        val deletion = pendingDeletions.firstOrNull() ?: return
        if (!isOnTop() || uiState.value.deletedGroupNotice != null) return
        consumeGroupDeletion(deletion.groupId)
        pendingDeletions = pendingDeletions.drop(1)
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
