@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactInviteUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactInviteUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.InviteGroupOption
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactInviteViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

class JsInviteGroupOption(
    val groupId: String,
    val name: String,
    val initials: String,
    val color: String,
)

class JsContactInviteState(
    // Why: "Convidar Diego", null for "Convidar com link ou QR".
    val name: String?,
    // Why: a guest's personal link, which joins straight in as them; no "Novo grupo" then.
    val isPersonal: Boolean,
    val groups: Array<JsInviteGroupOption>,
    val selectedGroupId: String?,
    val selectedGroupName: String?,
    val link: String?,
    val displayLink: String?,
    // Why: the QR code's modules row by row, qrSize by qrSize; empty while the link is pending.
    val qrSize: Int,
    val qrModules: Array<Boolean>,
    val isLinkCopied: Boolean,
    val isLoaded: Boolean,
    val hasNoGroups: Boolean,
    val showsOfflineNotice: Boolean,
    val isLinkPending: Boolean,
)

// Why: "copy" and "share" carry the link, and "share" the group's name and whom it is for, for the message.
class JsContactInviteAction(
    val type: String,
    val link: String,
    val groupName: String?,
    val name: String?,
)

class ContactInvitePresenter(
    route: JsRoute,
) {
    private val inviteRoute = route.route as Route.ContactInvite
    private val holder = ViewModelHolder(ContactInviteViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(inviteRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsContactInviteState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsContactInviteState(
                name = state.name,
                isPersonal = state.isPersonal,
                groups = state.groups.map(::toJsInviteGroupOption).toTypedArray(),
                selectedGroupId = state.selectedGroupId,
                selectedGroupName = state.selectedGroup?.name,
                link = state.link,
                displayLink = state.displayLink,
                qrSize = state.qrCode?.size ?: 0,
                qrModules = state.qrCode
                    ?.modules
                    .orEmpty()
                    .toTypedArray(),
                isLinkCopied = state.isLinkCopied,
                isLoaded = state.isLoaded,
                hasNoGroups = state.hasNoGroups,
                showsOfflineNotice = state.showsOfflineNotice,
                isLinkPending = state.isLinkPending,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeActions(onAction: (JsContactInviteAction) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                is ContactInviteUiAction.CopyLink -> JsContactInviteAction("copy", action.link, null, null)

                is ContactInviteUiAction.ShareLink -> JsContactInviteAction(
                    "share",
                    action.link,
                    action.groupName,
                    action.name,
                )
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun pickGroup(groupId: String) {
        viewModel.onEvent(ContactInviteUiEvent.OnGroupPicked(groupId))
    }

    fun newGroup() {
        viewModel.onEvent(ContactInviteUiEvent.OnNewGroupClicked)
    }

    fun copy() {
        viewModel.onEvent(ContactInviteUiEvent.OnCopyClicked)
    }

    fun share() {
        viewModel.onEvent(ContactInviteUiEvent.OnShareClicked)
    }

    fun close() {
        viewModel.onEvent(ContactInviteUiEvent.OnCloseClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsInviteGroupOption(option: InviteGroupOption): JsInviteGroupOption = JsInviteGroupOption(
    groupId = option.groupId,
    name = option.name,
    initials = option.initials,
    color = option.color.code,
)
