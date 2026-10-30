package com.quare.blitzsplit.feature.join.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.groups.domain.model.InviteMember
import com.quare.blitzsplit.core.groups.domain.model.InviteTarget
import com.quare.blitzsplit.core.groups.domain.model.JoinChoice
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.groups.domain.usecase.GetInvite
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.sync.domain.usecase.JoinGroup
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteStatus
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
    private val joinGroup: JoinGroup,
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
                displayLink = InviteLink.displayOf(
                    route.memberCode?.let {
                        InviteLink.personalOf(
                            inviteCode = route.code,
                            memberCode = it,
                            inviterCode = route.inviterCode,
                        )
                    } ?: InviteLink.of(route.code),
                ),
                isPersonal = route.memberCode != null,
                personalMember = null,
                inviterName = null,
                isJoining = false,
            ),
        )

    val uiAction: SharedFlow<JoinInviteUiAction>
        field = MutableSharedFlow<JoinInviteUiAction>()

    val stateWatcher: FlowWatcher<JoinInviteUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<JoinInviteUiAction> = FlowWatcher(uiAction)

    private var invite: Invite? = null

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
            JoinInviteUiEvent.OnJoinClicked -> if (uiState.value.isPersonal) joinAsPersonalMember() else pick()
            JoinInviteUiEvent.OnNotMeClicked -> pick()
            JoinInviteUiEvent.OnNotNowClicked -> leave()
            JoinInviteUiEvent.OnRetryClicked -> retry()
            JoinInviteUiEvent.OnGoHomeClicked -> if (isOnTop()) navigator.popTo(1)
            JoinInviteUiEvent.OnScanAnotherClicked -> scanAnother()
        }
    }

    // Why: what is on screen stays while the invite is read again, so coming back never flashes a loading state.
    private fun load() {
        loading?.cancel()
        loading = viewModelScope.launch {
            getInvite(
                InviteTarget(
                    code = route.code,
                    memberCode = route.memberCode,
                    inviterCode = route.inviterCode,
                ),
            ).onSuccess { invite ->
                when {
                    invite == null -> uiState.update { it.copy(status = JoinInviteStatus.INVALID) }

                    invite.isMember -> openGroup(invite.groupId)

                    route.memberCode != null && personalMemberOf(invite)?.hasJoined != false ->
                        uiState.update { it.copy(status = JoinInviteStatus.INVALID) }

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
        this.invite = invite
        uiState.update {
            it.copy(
                status = JoinInviteStatus.READY,
                groupName = invite.groupName,
                groupInitials = Initials.of(invite.groupName),
                groupColor = invite.groupColor,
                creatorName = invite.creatorName,
                members = invite.members.map { member -> member.toJoinMember() },
                personalMember = personalMemberOf(invite)?.toJoinMember(),
                inviterName = invite.inviterName,
            )
        }
    }

    // Why: a personal link names a member that may have been removed, null then.
    private fun personalMemberOf(invite: Invite): InviteMember? =
        invite.members.firstOrNull { it.id == invite.personalMemberId }

    // Why: a group joined from another device of this account may not be on this one yet, so a sync brings it first.
    private suspend fun openGroup(groupId: String) {
        if (observeGroup(groupId).first() == null) syncNow()
        if (observeGroup(groupId).first() == null) {
            uiState.update { it.copy(status = JoinInviteStatus.OFFLINE) }
        } else if (isOnTop()) {
            navigator.replaceTop(Route.Members(groupId, joinedAs = null))
        }
    }

    // Why: a join on its way may still open the group, so the list doesn't open over it.
    private fun pick() {
        val state = uiState.value
        if (state.status == JoinInviteStatus.READY && !state.isJoining && isOnTop()) {
            navigator.navigate(Route.JoinPick(route.code))
        }
    }

    private fun joinAsPersonalMember() {
        val state = uiState.value
        val invite = invite ?: return
        val member = state.personalMember ?: return
        if (state.status != JoinInviteStatus.READY || state.isJoining || !isOnTop()) return
        uiState.update { it.copy(isJoining = true) }
        viewModelScope.launch {
            joinGroup(invite, JoinChoice.AsMember(member.id))
                .onSuccess { outcome -> handle(outcome, invite, member.name) }
                .onFailure { uiAction.emit(JoinInviteUiAction.ShowJoinError) }
            uiState.update { it.copy(isJoining = false) }
        }
    }

    /*
     * Why: someone else who joined as the member first, or its removal, or a changed link, all make the link no
     * longer valid, which reading the invite again says.
     */
    private fun handle(
        outcome: JoinOutcome,
        invite: Invite,
        memberName: String,
    ) {
        when (outcome) {
            is JoinOutcome.Joined -> openJoinedGroup(outcome.groupId, joinedAs = memberName)

            is JoinOutcome.AlreadyMember -> openJoinedGroup(
                groupId = outcome.groupId,
                joinedAs = invite.members.firstOrNull { it.id == outcome.memberId }?.name,
            )

            JoinOutcome.MemberTaken,
            JoinOutcome.MemberGone,
            JoinOutcome.NameTaken,
            JoinOutcome.InvalidLink,
            -> load()
        }
    }

    private fun openJoinedGroup(
        groupId: String,
        joinedAs: String?,
    ) {
        if (isOnTop()) navigator.openJoinedGroup(groupId, joinedAs)
    }

    // Why: a link opened over the reader goes back to it, since the back stack can't hold the same route twice.
    private fun scanAnother() {
        if (uiState.value.status != JoinInviteStatus.INVALID || !isOnTop()) return
        val readerIndex = navigator.backStack.value.indexOfFirst { it is Route.JoinWithLink }
        val reader = Route.JoinWithLink(
            skippedCode = route.code,
            skippedMemberCode = route.memberCode,
        )
        if (readerIndex < 0) navigator.replaceTop(reader) else navigator.popTo(readerIndex + 1)
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }
}
