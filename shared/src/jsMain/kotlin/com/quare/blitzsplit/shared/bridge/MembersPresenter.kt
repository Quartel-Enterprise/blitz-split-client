@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.members.presentation.model.MemberNameEditor
import com.quare.blitzsplit.feature.members.presentation.model.MembersMember
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiAction
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent
import com.quare.blitzsplit.feature.members.presentation.viewmodel.MembersViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

class JsMembersMember(
    val id: String,
    val name: String,
    val initials: String,
    val color: String,
    val isMe: Boolean,
    val hasJoined: Boolean,
    val canBeRemoved: Boolean,
    val canBeInvited: Boolean,
)

class JsMemberNameEditor(
    val isAdding: Boolean,
    val draft: String,
    val takenName: String?,
    val nicknameSuggestions: Array<String>,
    val canSave: Boolean,
)

class JsMembersState(
    val groupName: String,
    val displayLink: String?,
    val members: Array<JsMembersMember>,
    val memberCount: Int,
    val joinedCount: Int,
    val editor: JsMemberNameEditor?,
    // Why: the name in "Remover Carla do grupo?", null while nobody is about to be removed.
    val memberToRemoveName: String?,
    val isLoaded: Boolean,
    // Why: "Você entrou como Bruno", null unless the group was just joined through its link.
    val joinedAs: String?,
    val showsProtectAccess: Boolean,
)

class MembersPresenter(
    route: JsRoute,
) {
    private val membersRoute = route.route as Route.Members
    private val holder = ViewModelHolder(MembersViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(membersRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsMembersState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsMembersState(
                groupName = state.groupName,
                displayLink = state.displayLink,
                members = state.members.map(::toJsMembersMember).toTypedArray(),
                memberCount = state.memberCount,
                joinedCount = state.joinedCount,
                editor = state.editor?.let(::toJsMemberNameEditor),
                memberToRemoveName = state.memberToRemove?.name,
                isLoaded = state.isLoaded,
                joinedAs = state.joinedAs,
                showsProtectAccess = state.showsProtectAccess,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "coming-soon" and "change-error", each a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                MembersUiAction.ShowComingSoon -> "coming-soon"
                MembersUiAction.ShowChangeError -> "change-error"
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(MembersUiEvent.OnBackClicked)
    }

    fun share() {
        viewModel.onEvent(MembersUiEvent.OnShareClicked)
    }

    fun add() {
        viewModel.onEvent(MembersUiEvent.OnAddClicked)
    }

    fun rename(memberId: String) {
        viewModel.onEvent(MembersUiEvent.OnRenameClicked(memberId))
    }

    fun personalInvite(memberId: String) {
        viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked(memberId))
    }

    fun remove(memberId: String) {
        viewModel.onEvent(MembersUiEvent.OnRemoveClicked(memberId))
    }

    fun confirmRemove() {
        viewModel.onEvent(MembersUiEvent.OnRemoveConfirmed)
    }

    fun dismissRemove() {
        viewModel.onEvent(MembersUiEvent.OnRemoveDismissed)
    }

    fun changeDraft(draft: String) {
        viewModel.onEvent(MembersUiEvent.OnDraftChanged(draft))
    }

    // Why: the ViewModel caps the draft as it handles it, so the field reads the capped text back in the same keystroke.
    fun getDraft(): String = viewModel.uiState.value.editor
        ?.draft
        .orEmpty()

    fun useSuggestion(name: String) {
        viewModel.onEvent(MembersUiEvent.OnSuggestionClicked(name))
    }

    fun save() {
        viewModel.onEvent(MembersUiEvent.OnSaveClicked)
    }

    fun dismissEditor() {
        viewModel.onEvent(MembersUiEvent.OnEditorDismissed)
    }

    fun protectAccess() {
        viewModel.onEvent(MembersUiEvent.OnProtectAccessClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsMembersMember(member: MembersMember): JsMembersMember = JsMembersMember(
    id = member.id,
    name = member.name,
    initials = member.initials,
    color = member.avatarColor.code,
    isMe = member.isMe,
    hasJoined = member.hasJoined,
    canBeRemoved = member.canBeRemoved,
    canBeInvited = member.canBeInvited,
)

private fun toJsMemberNameEditor(editor: MemberNameEditor): JsMemberNameEditor = JsMemberNameEditor(
    isAdding = editor.isAdding,
    draft = editor.draft,
    takenName = editor.takenName,
    nicknameSuggestions = editor.nicknameSuggestions.toTypedArray(),
    canSave = editor.canSave,
)
