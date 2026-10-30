package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.Initials
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
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiState
import com.quare.blitzsplit.feature.groups.presentation.model.MemberFace
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
    private val navigator: Navigator,
    observeProfile: ObserveProfile,
    observeAccount: ObserveAccount,
    observeSyncState: ObserveSyncState,
    observeGroups: ObserveGroups,
    connectivityObserver: NetworkConnectivityObserver,
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
                showsRecoveryCodePrompt = false,
                groups = emptyList(),
                hasLoadedGroups = false,
            ),
        )

    val uiAction: SharedFlow<GroupsUiAction>
        field = MutableSharedFlow<GroupsUiAction>()

    val stateWatcher: FlowWatcher<GroupsUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<GroupsUiAction> = FlowWatcher(uiAction)

    init {
        combine(
            observeProfile(),
            observeAccount(),
            observeSyncState(),
            connectivityObserver.observe(),
        ) { profile, account, syncState, isOnline ->
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
        observeGroups()
            .onEach { groups ->
                uiState.update {
                    it.copy(
                        groups = groups.map(::toRow),
                        hasLoadedGroups = true,
                    )
                }
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

            GroupsUiEvent.OnBalancesTabClicked,
            GroupsUiEvent.OnContactsTabClicked,
            GroupsUiEvent.OnActivityTabClicked,
            -> viewModelScope.launch { uiAction.emit(GroupsUiAction.ShowComingSoon) }
        }
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == Route.Groups

    private fun toRow(group: Group): GroupRow = GroupRow(
        id = group.id,
        name = group.name,
        initials = Initials.of(group.name),
        color = group.color,
        faces = group.members.take(MAX_FACES).map {
            MemberFace(
                initial = Initials.firstOf(it.name),
                color = it.avatarColor,
            )
        },
        extraMemberCount = (group.members.size - MAX_FACES).coerceAtLeast(0),
        memberCount = group.members.size,
        isPending = group.isPending,
    )

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

    private companion object {
        const val MAX_FACES = 3
    }
}
