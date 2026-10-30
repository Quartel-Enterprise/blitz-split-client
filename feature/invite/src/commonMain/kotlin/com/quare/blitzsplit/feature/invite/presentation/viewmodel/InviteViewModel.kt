package com.quare.blitzsplit.feature.invite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.groups.domain.usecase.EncodeQrCode
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.feature.invite.presentation.model.InviteMember
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiAction
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiEvent
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.time.Duration.Companion.seconds

class InviteViewModel(
    private val route: Route.Invite,
    private val navigator: Navigator,
    private val encodeQrCode: EncodeQrCode,
    private val retrySync: RetrySync,
    observeGroup: ObserveGroup,
    observeSyncState: ObserveSyncState,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<InviteUiState>
        field = MutableStateFlow(
            InviteUiState(
                groupName = "",
                link = null,
                qrCode = null,
                members = emptyList(),
                isOnline = true,
                isSyncFailed = false,
                isLinkCopied = false,
                isLoaded = false,
            ),
        )

    val uiAction: SharedFlow<InviteUiAction>
        field = MutableSharedFlow<InviteUiAction>()

    val stateWatcher: FlowWatcher<InviteUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<InviteUiAction> = FlowWatcher(uiAction)

    private val copiedDuration = 2.seconds
    private var copiedReset: Job? = null

    /*
     * Why: the link shows up here as soon as a sync brings it, also while the screen is open. Nothing is drawn before
     * the connectivity is known too, so an offline device never flashes the online copy of a pending link.
     */
    init {
        combine(
            observeGroup(route.groupId),
            connectivityObserver.observe(),
            observeSyncState(),
        ) { group, isOnline, syncState ->
            if (group == null) {
                leave()
            } else {
                show(
                    group = group,
                    isOnline = isOnline,
                    isSyncFailed = syncState == SyncState.FAILED,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: InviteUiEvent) {
        when (event) {
            InviteUiEvent.OnCloseClicked -> leave()
            InviteUiEvent.OnCopyClicked -> copy()
            InviteUiEvent.OnShareClicked -> share()
            InviteUiEvent.OnRetryClicked -> if (uiState.value.isLinkFailed) retrySync()
            InviteUiEvent.OnSeeWhoJoinedClicked, InviteUiEvent.OnGoToGroupClicked -> openMembers()
        }
    }

    private fun show(
        group: Group,
        isOnline: Boolean,
        isSyncFailed: Boolean,
    ) {
        val link = group.inviteCode?.let(InviteLink::of)
        uiState.update { state ->
            state.copy(
                groupName = group.name,
                link = link,
                // Why: encoded again only when the link changes, not on every edit of the group.
                qrCode = if (link == state.link) state.qrCode else link?.let { encodeQrCode(it) },
                members = group.members.map(::toInviteMember),
                isOnline = isOnline,
                isSyncFailed = isSyncFailed,
                isLoaded = true,
            )
        }
    }

    private fun toInviteMember(member: GroupMember): InviteMember = InviteMember(
        id = member.id,
        name = member.name,
        initials = if (member.isMe) Initials.firstOf(member.name) else Initials.of(member.name),
        avatarColor = member.avatarColor,
        isMe = member.isMe,
        hasJoined = member.hasJoined,
    )

    // Why: a second tap keeps "Copiado" for two seconds from that tap.
    private fun copy() {
        val link = uiState.value.link ?: return
        send(InviteUiAction.CopyLink(link))
        uiState.update { it.copy(isLinkCopied = true) }
        copiedReset?.cancel()
        copiedReset = viewModelScope.launch {
            delay(copiedDuration)
            uiState.update { it.copy(isLinkCopied = false) }
        }
    }

    private fun share() {
        val state = uiState.value
        val link = state.link ?: return
        send(
            InviteUiAction.ShareLink(
                link = link,
                groupName = state.groupName,
            ),
        )
    }

    private fun send(action: InviteUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }

    /*
     * Why: "Membros" stands in for the group's own screen (flow 6). When it opened "Convidar", going back to it keeps
     * the two from stacking; right after "Novo grupo", it takes this screen's place, so back goes to the home.
     */
    private fun openMembers() {
        val backStack = navigator.backStack.value
        if (backStack.lastOrNull() != route) return
        val members = Route.Members(route.groupId, joinedAs = null)
        val below = backStack.getOrNull(backStack.lastIndex - 1)
        if (below == members) {
            navigator.navigateBack()
        } else {
            navigator.replaceTop(members)
        }
    }

    // Why: only while it is still on top: a native back (a swipe, the browser's, a sheet dismissed) may have closed it.
    private fun leave() {
        if (navigator.backStack.value.lastOrNull() == route) navigator.navigateBack()
    }
}
