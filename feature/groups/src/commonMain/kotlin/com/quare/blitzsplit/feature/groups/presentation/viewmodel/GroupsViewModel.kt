package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.account.domain.usecase.ObserveRecoveryCodePrompt
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
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
    observeProfile: ObserveProfile,
    observeAccountStatus: ObserveAccountStatus,
    observeRecoveryCodePrompt: ObserveRecoveryCodePrompt,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<GroupsUiState>
        field = MutableStateFlow(
            GroupsUiState(
                avatarInitial = "",
                avatarColor = AvatarColor.default,
                syncBadge = SyncBadge.SYNCING,
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
            connectivityObserver.observe(),
        ) { profile, status, prompt, isOnline ->
            GroupsUiState(
                avatarInitial = profile
                    ?.displayName
                    ?.trim()
                    ?.firstOrNull()
                    ?.uppercase()
                    .orEmpty(),
                avatarColor = profile?.avatarColor ?: AvatarColor.default,
                syncBadge = when {
                    !isOnline -> SyncBadge.OFFLINE
                    status is AccountStatus.LocalOnly -> SyncBadge.SYNCING
                    else -> SyncBadge.SYNCED
                },
                showsRecoveryCodePrompt = prompt == RecoveryCodePrompt.DUE,
            )
        }.onEach { state -> uiState.value = state }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: GroupsUiEvent) {
        when (event) {
            GroupsUiEvent.OnCreateGroupClicked,
            GroupsUiEvent.OnJoinWithLinkClicked,
            GroupsUiEvent.OnProfileClicked,
            GroupsUiEvent.OnBalancesTabClicked,
            GroupsUiEvent.OnContactsTabClicked,
            GroupsUiEvent.OnActivityTabClicked,
            -> viewModelScope.launch { uiAction.emit(GroupsUiAction.ShowComingSoon) }
        }
    }
}
