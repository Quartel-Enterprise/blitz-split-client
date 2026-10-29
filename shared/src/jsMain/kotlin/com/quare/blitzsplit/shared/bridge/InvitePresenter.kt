@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.invite.presentation.model.InviteMember
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiAction
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiEvent
import com.quare.blitzsplit.feature.invite.presentation.viewmodel.InviteViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

class JsInviteMember(
    val id: String,
    val name: String,
    val initials: String,
    val color: String,
    val isMe: Boolean,
    val hasJoined: Boolean,
)

class JsInviteState(
    val groupName: String,
    val link: String?,
    val displayLink: String?,
    // Why: the QR code's modules row by row, qrSize by qrSize; empty while the link is pending.
    val qrSize: Int,
    val qrModules: Array<Boolean>,
    val members: Array<JsInviteMember>,
    val notJoinedNames: Array<String>,
    val isOnline: Boolean,
    val isLinkCopied: Boolean,
    val isLoaded: Boolean,
    val isLinkPending: Boolean,
    val isLinkFailed: Boolean,
)

// Why: "copy" and "share" carry the link (and the group's name for the message), "coming-soon" nothing.
class JsInviteAction(
    val type: String,
    val link: String?,
    val groupName: String?,
)

class InvitePresenter(
    route: JsRoute,
) {
    private val inviteRoute = route.route as Route.Invite
    private val holder = ViewModelHolder(InviteViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(inviteRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsInviteState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsInviteState(
                groupName = state.groupName,
                link = state.link,
                displayLink = state.displayLink,
                qrSize = state.qrCode?.size ?: 0,
                qrModules = state.qrCode
                    ?.modules
                    .orEmpty()
                    .toTypedArray(),
                members = state.members.map(::toJsInviteMember).toTypedArray(),
                notJoinedNames = state.notJoinedNames.toTypedArray(),
                isOnline = state.isOnline,
                isLinkCopied = state.isLinkCopied,
                isLoaded = state.isLoaded,
                isLinkPending = state.isLinkPending,
                isLinkFailed = state.isLinkFailed,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeActions(onAction: (JsInviteAction) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                is InviteUiAction.CopyLink -> JsInviteAction("copy", action.link, null)
                is InviteUiAction.ShareLink -> JsInviteAction("share", action.link, action.groupName)
                InviteUiAction.ShowComingSoon -> JsInviteAction("coming-soon", null, null)
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun close() {
        viewModel.onEvent(InviteUiEvent.OnCloseClicked)
    }

    fun copy() {
        viewModel.onEvent(InviteUiEvent.OnCopyClicked)
    }

    fun share() {
        viewModel.onEvent(InviteUiEvent.OnShareClicked)
    }

    fun retry() {
        viewModel.onEvent(InviteUiEvent.OnRetryClicked)
    }

    fun seeWhoJoined() {
        viewModel.onEvent(InviteUiEvent.OnSeeWhoJoinedClicked)
    }

    fun goToGroup() {
        viewModel.onEvent(InviteUiEvent.OnGoToGroupClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsInviteMember(member: InviteMember): JsInviteMember = JsInviteMember(
    id = member.id,
    name = member.name,
    initials = member.initials,
    color = member.avatarColor.code,
    isMe = member.isMe,
    hasJoined = member.hasJoined,
)
