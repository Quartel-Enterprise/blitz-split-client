package com.quare.blitzsplit.feature.members.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.groups.domain.model.MemberNameCheck
import com.quare.blitzsplit.core.groups.domain.usecase.AddMember
import com.quare.blitzsplit.core.groups.domain.usecase.CheckMemberName
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.RemoveMember
import com.quare.blitzsplit.core.groups.domain.usecase.RenameMember
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.feature.members.presentation.model.MemberNameEditor
import com.quare.blitzsplit.feature.members.presentation.model.MembersMember
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiAction
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MembersViewModel(
    private val route: Route.Members,
    private val navigator: Navigator,
    private val checkMemberName: CheckMemberName,
    private val addMember: AddMember,
    private val renameMember: RenameMember,
    private val removeMember: RemoveMember,
    observeGroup: ObserveGroup,
    observeAccount: ObserveAccount,
) : ViewModel() {
    val uiState: StateFlow<MembersUiState>
        field = MutableStateFlow(
            MembersUiState(
                groupName = "",
                displayLink = null,
                members = emptyList(),
                editor = null,
                memberToRemove = null,
                isLoaded = false,
                joinedAs = route.joinedAs,
                isAccountUnprotected = false,
            ),
        )

    val uiAction: SharedFlow<MembersUiAction>
        field = MutableSharedFlow<MembersUiAction>()

    val stateWatcher: FlowWatcher<MembersUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<MembersUiAction> = FlowWatcher(uiAction)

    // Why: the group went while another screen covered this one, which then leaves as soon as it is on top again.
    private var isGroupGone = false

    // Why: a sync may bring other members' changes while the screen is open, a removal of the whole group included.
    init {
        observeGroup(route.groupId)
            .onEach { group ->
                isGroupGone = group == null
                if (group == null) leave() else show(group)
            }.launchIn(viewModelScope)
        navigator.backStack
            .onEach { if (isGroupGone) leave() }
            .launchIn(viewModelScope)
        if (route.joinedAs != null) {
            observeAccount()
                .onEach { account ->
                    uiState.update { it.copy(isAccountUnprotected = account.status !is AccountStatus.Connected) }
                }.launchIn(viewModelScope)
        }
    }

    fun onEvent(event: MembersUiEvent) {
        when (event) {
            MembersUiEvent.OnBackClicked -> leave()

            MembersUiEvent.OnShareClicked -> if (isOnTop()) navigator.navigate(Route.Invite(route.groupId))

            MembersUiEvent.OnOtherTabClicked -> send(MembersUiAction.ShowComingSoon)

            MembersUiEvent.OnAddClicked -> openEditor(
                memberId = null,
                draft = "",
            )

            is MembersUiEvent.OnRenameClicked -> openRename(event.memberId)

            is MembersUiEvent.OnPersonalInviteClicked -> send(MembersUiAction.ShowComingSoon)

            is MembersUiEvent.OnRemoveClicked -> askToRemove(event.memberId)

            MembersUiEvent.OnRemoveConfirmed -> remove()

            MembersUiEvent.OnRemoveDismissed -> uiState.update { it.copy(memberToRemove = null) }

            is MembersUiEvent.OnDraftChanged -> changeDraft(event.draft)

            is MembersUiEvent.OnSuggestionClicked -> saveSuggestion(event.name)

            MembersUiEvent.OnSaveClicked -> save()

            MembersUiEvent.OnEditorDismissed -> uiState.update { it.copy(editor = null) }

            MembersUiEvent.OnProtectAccessClicked -> if (isOnTop()) navigator.navigate(Route.Profile)
        }
    }

    private fun show(group: Group) {
        val members = group.members.map(::toMembersMember)
        uiState.update { state ->
            state.copy(
                groupName = group.name,
                displayLink = group.inviteCode?.let(InviteLink::of)?.substringAfter("://"),
                members = members,
                // Why: the member about to be removed may be gone, or have joined, by now.
                memberToRemove = state.memberToRemove?.let { asked ->
                    members.firstOrNull { it.id == asked.id && it.canBeRemoved }
                },
                isLoaded = true,
            )
        }
        // Why: the member being renamed may be gone, and a name the draft took may be free or taken by now.
        val editor = uiState.value.editor ?: return
        if (editor.memberId != null && uiState.value.members.none { it.id == editor.memberId }) {
            uiState.update { it.copy(editor = null) }
        } else {
            checkDraft()
        }
    }

    private fun toMembersMember(member: GroupMember): MembersMember = MembersMember(
        id = member.id,
        name = member.name,
        initials = if (member.isMe) Initials.firstOf(member.name) else Initials.of(member.name),
        avatarColor = member.avatarColor,
        isMe = member.isMe,
        hasJoined = member.hasJoined,
    )

    private fun openRename(memberId: String) {
        val member = uiState.value.members.firstOrNull { it.id == memberId } ?: return
        openEditor(
            memberId = memberId,
            draft = member.name,
        )
    }

    private fun openEditor(
        memberId: String?,
        draft: String,
    ) {
        uiState.update {
            it.copy(
                editor = MemberNameEditor(
                    memberId = memberId,
                    draft = draft,
                    takenName = null,
                    nicknameSuggestions = emptyList(),
                ),
            )
        }
        checkDraft()
    }

    private fun changeDraft(draft: String) {
        uiState.update { state ->
            state.copy(editor = state.editor?.copy(draft = GroupNameRules.toCapped(draft)))
        }
        checkDraft()
    }

    private fun checkDraft() {
        val editor = uiState.value.editor ?: return
        val taken = checkOf(editor.draft, editor.memberId) as? MemberNameCheck.Taken
        uiState.update { state ->
            state.copy(
                editor = state.editor?.copy(
                    takenName = taken?.existingName,
                    nicknameSuggestions = taken?.suggestions.orEmpty(),
                ),
            )
        }
    }

    // Why: a member's own name is free for them, and the nickname's initial ("Ana P.") is this device's user's.
    private fun checkOf(
        name: String,
        memberId: String?,
    ): MemberNameCheck {
        val members = uiState.value.members
        return checkMemberName(
            name = name,
            takenNames = members.filterNot { it.id == memberId }.map(MembersMember::name),
            adderName = members.firstOrNull { it.isMe }?.name.orEmpty(),
        )
    }

    private fun save() {
        val editor = uiState.value.editor ?: return
        val check = checkOf(editor.draft, editor.memberId) as? MemberNameCheck.Available ?: return
        save(editor, check.name)
    }

    // Why: a suggestion is free when offered, but the members may have changed since.
    private fun saveSuggestion(name: String) {
        val editor = uiState.value.editor ?: return
        if (name !in editor.nicknameSuggestions) return
        val check = checkOf(name, editor.memberId) as? MemberNameCheck.Available ?: return
        save(editor, check.name)
    }

    // Why: the dialog closes right away; the write is local, and only a failure has something to say.
    private fun save(
        editor: MemberNameEditor,
        name: String,
    ) {
        uiState.update { it.copy(editor = null) }
        val memberId = editor.memberId
        val current = uiState.value.members.firstOrNull { it.id == memberId }
        if (memberId != null && current?.name == name) return
        viewModelScope.launch {
            val result = if (memberId == null) {
                addMember(
                    groupId = route.groupId,
                    name = name,
                )
            } else {
                renameMember(
                    memberId = memberId,
                    name = name,
                )
            }
            result.onFailure { uiAction.emit(MembersUiAction.ShowChangeError) }
        }
    }

    private fun askToRemove(memberId: String) {
        val member = uiState.value.members.firstOrNull { it.id == memberId } ?: return
        if (member.canBeRemoved) uiState.update { it.copy(memberToRemove = member) }
    }

    // Why: the confirmation closes right away; the write is local, and only a failure has something to say.
    private fun remove() {
        val member = uiState.value.memberToRemove ?: return
        uiState.update { it.copy(memberToRemove = null) }
        viewModelScope.launch {
            removeMember(member.id).onFailure { uiAction.emit(MembersUiAction.ShowChangeError) }
        }
    }

    private fun send(action: MembersUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }
}
