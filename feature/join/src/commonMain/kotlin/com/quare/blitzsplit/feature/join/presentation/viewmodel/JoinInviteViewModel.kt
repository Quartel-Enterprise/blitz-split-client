package com.quare.blitzsplit.feature.join.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.groups.domain.usecase.GetInvite
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteStatus
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class JoinInviteViewModel(
    private val route: Route.JoinInvite,
    private val navigator: Navigator,
    private val getInvite: GetInvite,
    private val observeGroup: ObserveGroup,
    private val syncNow: SyncNow,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<JoinInviteUiState>
        field = MutableStateFlow(
            JoinInviteUiState(
                status = JoinInviteStatus.LOADING,
                groupName = "",
                groupInitials = "",
                groupColor = AvatarColor.default,
                creatorName = null,
                members = emptyList(),
                displayLink = InviteLink.of(route.code).substringAfter("://"),
            ),
        )

    val stateWatcher: FlowWatcher<JoinInviteUiState> = FlowWatcher(uiState)

    private var loading: Job? = null
    private var isCovered = false

    /*
     * Why: offline, the invite opens by itself once the device is online. Back from "Quem é você no grupo?" it is read
     * again, since someone may have joined or the link changed meanwhile.
     */
    init {
        load()
        connectivityObserver
            .observe()
            // Why: the first value is the connection as it is now, which the first load already covers.
            .distinctUntilChanged()
            .drop(1)
            .onEach { isOnline -> if (isOnline && uiState.value.status == JoinInviteStatus.OFFLINE) load() }
            .launchIn(viewModelScope)
        navigator.backStack
            .onEach { backStack ->
                val isOnTop = backStack.lastOrNull() == route
                if (isOnTop && isCovered) load()
                isCovered = !isOnTop && route in backStack
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: JoinInviteUiEvent) {
        when (event) {
            JoinInviteUiEvent.OnJoinClicked -> pick()
            JoinInviteUiEvent.OnNotNowClicked -> leave()
            JoinInviteUiEvent.OnRetryClicked -> retry()
            JoinInviteUiEvent.OnGoHomeClicked -> if (isOnTop()) navigator.popTo(1)
        }
    }

    // Why: what is on screen stays while the invite is read again, so coming back never flashes a loading state.
    private fun load() {
        loading?.cancel()
        loading = viewModelScope.launch {
            getInvite(route.code)
                .onSuccess { invite ->
                    when {
                        invite == null -> uiState.update { it.copy(status = JoinInviteStatus.INVALID) }
                        invite.isMember -> openGroup(invite.groupId)
                        else -> show(invite)
                    }
                }.onFailure { uiState.update { it.copy(status = JoinInviteStatus.OFFLINE) } }
        }
    }

    private fun retry() {
        if (uiState.value.status != JoinInviteStatus.OFFLINE) return
        uiState.update { it.copy(status = JoinInviteStatus.LOADING) }
        load()
    }

    private fun show(invite: Invite) {
        uiState.update {
            it.copy(
                status = JoinInviteStatus.READY,
                groupName = invite.groupName,
                groupInitials = Initials.of(invite.groupName),
                groupColor = invite.groupColor,
                creatorName = invite.creatorName,
                members = invite.members.map { member -> member.toJoinMember() },
            )
        }
    }

    // Why: a group joined from another device of this account may not be on this one yet, so a sync brings it first.
    private suspend fun openGroup(groupId: String) {
        if (observeGroup(groupId).first() == null) syncNow()
        if (observeGroup(groupId).first() == null) {
            uiState.update { it.copy(status = JoinInviteStatus.OFFLINE) }
        } else if (isOnTop()) {
            navigator.replaceTop(Route.Members(groupId, joinedAs = null))
        }
    }

    private fun pick() {
        if (uiState.value.status == JoinInviteStatus.READY && isOnTop()) navigator.navigate(Route.JoinPick(route.code))
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }
}
