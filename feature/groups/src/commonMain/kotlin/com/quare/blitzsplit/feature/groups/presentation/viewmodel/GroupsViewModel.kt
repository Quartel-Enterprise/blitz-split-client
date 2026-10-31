package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ConsumeGroupDeletion
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroupDeletions
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.groups.presentation.mapper.ActivityClock
import com.quare.blitzsplit.feature.groups.presentation.mapper.GroupWithMoney
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class GroupsViewModel(
    private val retrySync: RetrySync,
    private val syncNow: SyncNow,
    private val navigator: Navigator,
    private val consumeGroupDeletion: ConsumeGroupDeletion,
    currentTimeMillis: CurrentTimeMillis,
    currentTimeZone: CurrentTimeZone,
    observeProfile: ObserveProfile,
    observeAccount: ObserveAccount,
    observeSyncState: ObserveSyncState,
    observeGroups: ObserveGroups,
    observeGroupsMoney: ObserveGroupsMoney,
    connectivityObserver: NetworkConnectivityObserver,
    observeGroupDeletions: ObserveGroupDeletions,
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

    init {
        combine(
            observeProfile(),
            observeAccount(),
            observeSyncState(),
            connectivityObserver.observe(),
        ) { profile, account, syncState, isOnline ->
            this.isOnline = isOnline
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
        // Why: each group's bills and payments, again whenever any of them changes here or through a sync.
        combine(observeGroups(), observeGroupsMoney()) { groups, money ->
            groups.map { group ->
                val ofGroup = money[group.id] ?: GroupMoney.NONE
                GroupWithMoney(
                    group = group,
                    expenses = ofGroup.expenses,
                    settlements = ofGroup.settlements,
                )
            }
        }.onEach { groups ->
            val rows = rowsOf(
                groups = groups,
                clock = ActivityClock(
                    nowMillis = currentTimeMillis(),
                    timeZone = currentTimeZone(),
                ),
            )
            uiState.update {
                it.copy(
                    groups = rows,
                    hasLoadedGroups = true,
                    summary = summaryOf(rows),
                    pendingChangeCount = pendingChangeCountOf(groups),
                )
            }
        }.launchIn(viewModelScope)
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

            GroupsUiEvent.OnBalancesTabClicked,
            GroupsUiEvent.OnContactsTabClicked,
            GroupsUiEvent.OnActivityTabClicked,
            -> showComingSoon()
        }
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
