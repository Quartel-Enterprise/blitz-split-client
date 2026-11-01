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
import com.quare.blitzsplit.core.navigation.openGroupTab
import com.quare.blitzsplit.core.presentation.CopiedFeedback
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.sync.domain.usecase.ChangeInviteLink
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

class InviteViewModel(
    private val route: Route.Invite,
    private val navigator: Navigator,
    private val encodeQrCode: EncodeQrCode,
    private val retrySync: RetrySync,
    private val changeInviteLink: ChangeInviteLink,
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
                isCreatedByMe = false,
                isNewGroup = route.isNewGroup,
                isConfirmingLinkChange = false,
                isChangingLink = false,
            ),
        )

    val uiAction: SharedFlow<InviteUiAction>
        field = MutableSharedFlow<InviteUiAction>()

    val stateWatcher: FlowWatcher<InviteUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<InviteUiAction> = FlowWatcher(uiAction)

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

            InviteUiEvent.OnSeeWhoJoinedClicked -> navigator.openGroupTab(
                from = route,
                tab = Route.Members(route.groupId),
            )

            InviteUiEvent.OnGoToGroupClicked -> navigator.openGroupTab(
                from = route,
                tab = Route.GroupDetail(route.groupId, joinedAs = null),
            )

            InviteUiEvent.OnChangeLinkClicked -> askToChangeLink()

            InviteUiEvent.OnChangeLinkConfirmed -> changeLink()

            InviteUiEvent.OnChangeLinkDismissed -> uiState.update { it.copy(isConfirmingLinkChange = false) }
        }
    }

    private fun show(
        group: Group,
        isOnline: Boolean,
        isSyncFailed: Boolean,
    ) {
        val link = group.inviteCode?.let(InviteLink::of)
        /*
         * Why: encoded again only when the link changes, not on every edit of the group, and outside update, which
         * may run its block again.
         */
        val current = uiState.value
        val qrCode = if (link == current.link) current.qrCode else link?.let { encodeQrCode(it) }
        uiState.update { state ->
            val isSameLink = link == state.link
            val shown = state.copy(
                groupName = group.name,
                link = link,
                qrCode = qrCode,
                members = group.members.map(::toInviteMember),
                isOnline = isOnline,
                isSyncFailed = isSyncFailed,
                // Why: "Copiado" was about the old link, not the new one a change brought.
                isLinkCopied = state.isLinkCopied && isSameLink,
                isLoaded = true,
                isCreatedByMe = group.isCreatedByMe,
            )
            // Why: going offline, or losing the right to change it, closes the question with the button.
            shown.copy(isConfirmingLinkChange = shown.isConfirmingLinkChange && shown.showsChangeLink)
        }
    }

    private fun toInviteMember(member: GroupMember): InviteMember = InviteMember(
        id = member.id,
        name = member.name,
        initials = Initials.ofMember(member.name, member.isMe),
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
            delay(CopiedFeedback.duration)
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

    private fun askToChangeLink() {
        val state = uiState.value
        if (state.showsChangeLink && !state.isChangingLink) uiState.update { it.copy(isConfirmingLinkChange = true) }
    }

    // Why: the new link shows once the use case wrote it to Room; a failure keeps the old one, which still works.
    private fun changeLink() {
        if (!uiState.value.isConfirmingLinkChange) return
        uiState.update {
            it.copy(
                isConfirmingLinkChange = false,
                isChangingLink = true,
            )
        }
        viewModelScope.launch {
            changeInviteLink(route.groupId).onFailure { uiAction.emit(InviteUiAction.ShowChangeLinkFailed) }
            uiState.update { it.copy(isChangingLink = false) }
        }
    }

    private fun send(action: InviteUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }

    // Why: only while it is still on top: a native back (a swipe, the browser's, a sheet dismissed) may have closed it.
    private fun leave() {
        if (navigator.backStack.value.lastOrNull() == route) navigator.navigateBack()
    }
}
