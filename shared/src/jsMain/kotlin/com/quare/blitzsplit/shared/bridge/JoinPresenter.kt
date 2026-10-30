@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinMember
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiEvent
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinInviteViewModel
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinPickViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

class JsJoinMember(
    val id: String,
    val name: String,
    val initials: String,
    val color: String,
    val hasJoined: Boolean,
)

class JsJoinInviteState(
    // Why: "loading", "ready", "invalid" or "offline".
    val status: String,
    val groupName: String,
    val groupInitials: String,
    val groupColor: String,
    val creatorName: String?,
    // Why: the faces next to "Pierre e Ana já estão no grupo".
    val joined: Array<JsJoinMember>,
    val memberCount: Int,
    val joinedNames: Array<String>,
    val displayLink: String,
    // Why: a personal link ("Convite pessoal"), which joins as personalMember with "Entrar como Carla".
    val isPersonal: Boolean,
    val personalMember: JsJoinMember?,
    // Why: "Ana te chamou como Carla": whoever shared the link, null when the link doesn't say.
    val inviterName: String?,
    val isJoining: Boolean,
)

class JoinInvitePresenter(
    route: JsRoute,
) {
    private val inviteRoute = route.route as Route.JoinInvite
    private val holder = ViewModelHolder(JoinInviteViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(inviteRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsJoinInviteState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsJoinInviteState(
                status = state.status.name.lowercase(),
                groupName = state.groupName,
                groupInitials = state.groupInitials,
                groupColor = state.groupColor.code,
                creatorName = state.creatorName,
                joined = state.joined.map(::toJsJoinMember).toTypedArray(),
                memberCount = state.memberCount,
                joinedNames = state.joinedNames.toTypedArray(),
                displayLink = state.displayLink,
                isPersonal = state.isPersonal,
                personalMember = state.personalMember?.let(::toJsJoinMember),
                inviterName = state.inviterName,
                isJoining = state.isJoining,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "join-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                JoinInviteUiAction.ShowJoinError -> "join-error"
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun join() {
        viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)
    }

    // Why: "Não sou Carla", the whole list.
    fun notMe() {
        viewModel.onEvent(JoinInviteUiEvent.OnNotMeClicked)
    }

    fun notNow() {
        viewModel.onEvent(JoinInviteUiEvent.OnNotNowClicked)
    }

    fun retry() {
        viewModel.onEvent(JoinInviteUiEvent.OnRetryClicked)
    }

    fun goHome() {
        viewModel.onEvent(JoinInviteUiEvent.OnGoHomeClicked)
    }

    // Why: "Escanear outro QR", with the invite no longer valid.
    fun scanAnother() {
        viewModel.onEvent(JoinInviteUiEvent.OnScanAnotherClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

class JsNewMemberDraft(
    val name: String,
    val isNameTaken: Boolean,
)

class JsJoinPickState(
    val isLoaded: Boolean,
    val notJoined: Array<JsJoinMember>,
    val joined: Array<JsJoinMember>,
    val selectedMemberId: String?,
    val selectedName: String?,
    val takenName: String?,
    val creatorName: String?,
    val newMember: JsNewMemberDraft?,
    val isJoining: Boolean,
    val canJoin: Boolean,
    val canJoinAsNewMember: Boolean,
)

class JoinPickPresenter(
    route: JsRoute,
) {
    private val pickRoute = route.route as Route.JoinPick
    private val holder = ViewModelHolder(JoinPickViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(pickRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsJoinPickState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsJoinPickState(
                isLoaded = state.isLoaded,
                notJoined = state.notJoined.map(::toJsJoinMember).toTypedArray(),
                joined = state.joined.map(::toJsJoinMember).toTypedArray(),
                selectedMemberId = state.selectedMemberId,
                selectedName = state.selectedName,
                takenName = state.takenName,
                creatorName = state.creatorName,
                newMember = state.newMember?.let { JsNewMemberDraft(it.name, it.isNameTaken) },
                isJoining = state.isJoining,
                canJoin = state.canJoin,
                canJoinAsNewMember = state.canJoinAsNewMember,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "join-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                JoinPickUiAction.ShowJoinError -> "join-error"
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(JoinPickUiEvent.OnBackClicked)
    }

    fun select(memberId: String) {
        viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(memberId))
    }

    fun join() {
        viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)
    }

    fun notInList() {
        viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
    }

    fun changeNewName(name: String) {
        viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged(name))
    }

    // Why: the ViewModel caps the name as it handles it, so the field reads the capped text back in the same keystroke.
    fun getNewName(): String = viewModel.uiState.value.newMember
        ?.name
        .orEmpty()

    fun joinAsNewMember() {
        viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)
    }

    fun dismissNewMember() {
        viewModel.onEvent(JoinPickUiEvent.OnNewMemberDismissed)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsJoinMember(member: JoinMember): JsJoinMember = JsJoinMember(
    id = member.id,
    name = member.name,
    initials = member.initials,
    color = member.avatarColor.code,
    hasJoined = member.hasJoined,
)
