package com.quare.blitzsplit.feature.join.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteTarget
import com.quare.blitzsplit.core.groups.domain.model.JoinChoice
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.groups.domain.model.MemberNameCheck
import com.quare.blitzsplit.core.groups.domain.usecase.CheckMemberName
import com.quare.blitzsplit.core.groups.domain.usecase.GetInvite
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.sync.domain.usecase.JoinGroup
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiState
import com.quare.blitzsplit.feature.join.presentation.model.NewMemberDraft
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JoinPickViewModel(
    private val route: Route.JoinPick,
    private val navigator: Navigator,
    private val getInvite: GetInvite,
    private val joinGroup: JoinGroup,
    private val checkMemberName: CheckMemberName,
) : ViewModel() {
    val uiState: StateFlow<JoinPickUiState>
        field = MutableStateFlow(
            JoinPickUiState(
                isLoaded = false,
                members = emptyList(),
                selectedMemberId = null,
                takenName = null,
                creatorName = null,
                newMember = null,
                isJoining = false,
            ),
        )

    val uiAction: SharedFlow<JoinPickUiAction>
        field = MutableSharedFlow<JoinPickUiAction>()

    val stateWatcher: FlowWatcher<JoinPickUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<JoinPickUiAction> = FlowWatcher(uiAction)

    private var invite: Invite? = null

    // Why: a name the server found taken stays taken, even where its comparison differs from this device's.
    private var rejectedName: String? = null

    init {
        viewModelScope.launch { load() }
    }

    fun onEvent(event: JoinPickUiEvent) {
        when (event) {
            JoinPickUiEvent.OnBackClicked -> leave()
            is JoinPickUiEvent.OnMemberSelected -> select(event.memberId)
            JoinPickUiEvent.OnJoinClicked -> joinAsSelected()
            JoinPickUiEvent.OnNotInListClicked -> openNewMember()
            is JoinPickUiEvent.OnNewNameChanged -> changeNewName(event.name)
            JoinPickUiEvent.OnNewMemberConfirmed -> joinAsNewMember()
            JoinPickUiEvent.OnNewMemberDismissed -> uiState.update { it.copy(newMember = null) }
        }
    }

    /*
     * Why: without the invite there is nothing to pick, so a link no longer valid, or a device gone offline, goes back
     * to the invite, which reads it again and says why.
     */
    private suspend fun load() {
        val invite = getInvite(
            InviteTarget(
                code = route.code,
                memberCode = null,
                inviterCode = null,
            ),
        ).getOrNull()
        if (invite == null) {
            leave()
            return
        }
        this.invite = invite
        val members = invite.members.map { it.toJoinMember() }
        uiState.update { state ->
            state.copy(
                isLoaded = true,
                members = members,
                // Why: a member someone else joined as meanwhile can't stay picked.
                selectedMemberId = state.selectedMemberId?.takeIf { id ->
                    members.any { it.id == id && !it.hasJoined }
                },
                creatorName = invite.creatorName,
            )
        }
        checkNewName()
    }

    // Why: not while a join is on its way, or a taken member's notice would name the newly picked one.
    private fun select(memberId: String) {
        val state = uiState.value
        val member = state.members.firstOrNull { it.id == memberId } ?: return
        if (!member.hasJoined && !state.isJoining) {
            uiState.update {
                it.copy(
                    selectedMemberId = memberId,
                    takenName = null,
                )
            }
        }
    }

    private fun joinAsSelected() {
        val state = uiState.value
        val memberId = state.selectedMemberId ?: return
        if (state.canJoin) join(JoinChoice.AsMember(memberId))
    }

    private fun openNewMember() {
        uiState.update {
            it.copy(
                newMember = NewMemberDraft(
                    name = "",
                    isNameTaken = false,
                ),
            )
        }
    }

    private fun changeNewName(name: String) {
        uiState.update { state ->
            state.copy(newMember = state.newMember?.copy(name = GroupNameRules.toCapped(name)))
        }
        checkNewName()
    }

    private fun checkNewName() {
        val draft = uiState.value.newMember ?: return
        val check = checkOf(draft.name)
        val isTaken =
            check is MemberNameCheck.Taken ||
                (rejectedName != null && (check as? MemberNameCheck.Available)?.name == rejectedName)
        uiState.update { state -> state.copy(newMember = state.newMember?.copy(isNameTaken = isTaken)) }
    }

    private fun joinAsNewMember() {
        val draft = uiState.value.newMember ?: return
        val check = checkOf(draft.name) as? MemberNameCheck.Available ?: return
        if (uiState.value.canJoinAsNewMember) join(JoinChoice.AsNewMember(check.name))
    }

    private fun checkOf(name: String): MemberNameCheck = checkMemberName(
        name = name,
        takenNames = uiState.value.members.map { it.name },
        adderName = "",
    )

    private fun join(choice: JoinChoice) {
        val invite = invite ?: return
        uiState.update { it.copy(isJoining = true) }
        viewModelScope.launch {
            joinGroup(invite, choice)
                .onSuccess { outcome -> handle(outcome, choice) }
                .onFailure { uiAction.emit(JoinPickUiAction.ShowJoinError) }
            uiState.update { it.copy(isJoining = false) }
        }
    }

    private suspend fun handle(
        outcome: JoinOutcome,
        choice: JoinChoice,
    ) {
        when (outcome) {
            is JoinOutcome.Joined -> openGroup(outcome.groupId, joinedAs = nameOf(choice))

            is JoinOutcome.AlreadyMember -> openGroup(
                outcome.groupId,
                joinedAs = findJoinedName(outcome.memberId, choice),
            )

            JoinOutcome.MemberTaken -> {
                uiState.update {
                    it.copy(
                        takenName = it.selectedName,
                        selectedMemberId = null,
                    )
                }
                load()
            }

            JoinOutcome.MemberGone -> {
                uiState.update { it.copy(selectedMemberId = null) }
                load()
            }

            JoinOutcome.NameTaken -> {
                rejectedName = (choice as? JoinChoice.AsNewMember)?.name
                uiState.update { state -> state.copy(newMember = state.newMember?.copy(isNameTaken = true)) }
                load()
            }

            JoinOutcome.InvalidLink -> leave()
        }
    }

    /*
     * Why: the member the server says the account is, usually a retry after the join went through but its sync
     * failed. A new member the list doesn't hold yet is the name this device sent.
     */
    private fun findJoinedName(
        memberId: String?,
        choice: JoinChoice,
    ): String? = uiState.value.members
        .firstOrNull { it.id == memberId }
        ?.name ?: (choice as? JoinChoice.AsNewMember)?.name

    private fun nameOf(choice: JoinChoice): String? = when (choice) {
        is JoinChoice.AsMember ->
            uiState.value.members
                .firstOrNull { it.id == choice.memberId }
                ?.name

        is JoinChoice.AsNewMember -> choice.name
    }

    /*
     * Why: the group opens over the home, whatever was under the invite: the welcome too, since joining without a
     * profile made one from the group.
     */
    private fun openGroup(
        groupId: String,
        joinedAs: String?,
    ) {
        if (!isOnTop()) return
        navigator.replaceAll(Route.Groups)
        navigator.navigate(
            Route.Members(
                groupId = groupId,
                joinedAs = joinedAs,
            ),
        )
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }
}
