package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.account.domain.usecase.ObserveRecoveryCodePrompt
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
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
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class GroupsViewModel(
    private val retrySync: RetrySync,
    observeProfile: ObserveProfile,
    observeAccountStatus: ObserveAccountStatus,
    observeRecoveryCodePrompt: ObserveRecoveryCodePrompt,
    observeSyncState: ObserveSyncState,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<GroupsUiState>
        field = MutableStateFlow(
            GroupsUiState(
                displayName = "",
                avatarInitial = "",
                avatarColor = AvatarColor.default,
                photo = null,
                accountProvider = null,
                syncBadge = null,
                showsRecoveryCodePrompt = false,
            ),
        )

    val uiAction: SharedFlow<GroupsUiAction>
        field = MutableSharedFlow<GroupsUiAction>()

    val stateWatcher: FlowWatcher<GroupsUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<GroupsUiAction> = FlowWatcher(uiAction)

    init {
        combine(
            observeProfile(),
            observeAccountStatus(),
            observeRecoveryCodePrompt(),
            observeSyncState(),
            connectivityObserver.observe(),
        ) { profile, accountStatus, prompt, syncState, isOnline ->
            val displayName = profile?.displayName?.trim().orEmpty()
            GroupsUiState(
                displayName = displayName,
                avatarInitial = displayName.firstOrNull()?.uppercase().orEmpty(),
                avatarColor = profile?.avatarColor ?: AvatarColor.default,
                photo = profile?.photo,
                accountProvider = (accountStatus as? AccountStatus.Connected)?.provider,
                syncBadge = syncBadgeOf(syncState = syncState, isOnline = isOnline),
                showsRecoveryCodePrompt = prompt == RecoveryCodePrompt.DUE,
            )
        }.onEach { state -> uiState.value = state }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: GroupsUiEvent) {
        when (event) {
            GroupsUiEvent.OnRetrySyncClicked -> if (uiState.value.canRetrySync) retrySync()

            GroupsUiEvent.OnCreateGroupClicked,
            GroupsUiEvent.OnJoinWithLinkClicked,
            GroupsUiEvent.OnProfileClicked,
            GroupsUiEvent.OnBalancesTabClicked,
            GroupsUiEvent.OnContactsTabClicked,
            GroupsUiEvent.OnActivityTabClicked,
            -> viewModelScope.launch { uiAction.emit(GroupsUiAction.ShowComingSoon) }
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
