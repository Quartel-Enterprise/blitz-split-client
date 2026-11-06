package com.quare.blitzsplit.feature.join.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteTarget
import com.quare.blitzsplit.core.groups.domain.model.JoinChoice
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.groups.domain.model.MemberNameCheck
import com.quare.blitzsplit.core.groups.domain.usecase.CheckMemberName
import com.quare.blitzsplit.core.groups.domain.usecase.GetInvite
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.domain.usecase.JoinGroup
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiState
import com.quare.blitzsplit.feature.join.presentation.model.JoinTarget
import com.quare.blitzsplit.feature.join.presentation.model.NewMemberDraft
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JoinPickViewModel(
    private val route: Route.JoinPick,
    private val navigator: Navigator,
    private val getInvite: GetInvite,
    private val joinGroup: JoinGroup,
    private val checkMemberName: CheckMemberName,
    observeAccountStatus: ObserveAccountStatus,
    observeProfile: ObserveProfile,
    trackEvent: TrackEvent,
) : TrackedViewModel<JoinPickUiEvent>(trackEvent) {
    val uiState: StateFlow<JoinPickUiState>
        field = MutableStateFlow(
            JoinPickUiState(
                isLoaded = false,
                groupName = "",
                members = emptyList(),
                selectedMemberId = null,
                takenName = null,
                creatorName = null,
                newMember = null,
                joining = null,
            ),
        )

    val uiAction: SharedFlow<JoinPickUiAction>
        field = MutableSharedFlow<JoinPickUiAction>()

    val stateWatcher: FlowWatcher<JoinPickUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<JoinPickUiAction> = FlowWatcher(uiAction)

    private var invite: Invite? = null

    // Why: the profile's name while a Google or Apple account is signed in, which "Não estou na lista" joins as.
    private val accountNames = combine(observeAccountStatus(), observeProfile()) { status, profile ->
        profile?.displayName?.takeIf { status is AccountStatus.Connected }
    }

    // Why: the account is read at the tap, so a second tap meanwhile opens or joins nothing twice.
    private var isOpeningNewMember = false

    init {
        viewModelScope.launch { load() }
    }

    override fun handleEvent(event: JoinPickUiEvent) {
        when (event) {
            JoinPickUiEvent.OnBackClicked -> leave()
            is JoinPickUiEvent.OnMemberSelected -> select(event.memberId)
            JoinPickUiEvent.OnJoinClicked -> joinAsSelected()
            JoinPickUiEvent.OnNotInListClicked -> openNewMember()
            is JoinPickUiEvent.OnNewNameChanged -> changeNewName(event.name)
            JoinPickUiEvent.OnNewMemberConfirmed -> joinAsNewMember()
            JoinPickUiEvent.OnNewMemberDismissed -> dismissNewMember()
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
                groupName = invite.groupName,
                members = members,
                // Why: a member someone else joined as meanwhile can't stay picked.
                selectedMemberId = state.selectedMemberId?.takeIf { id ->
                    members.any { it.id == id && !it.hasJoined }
                },
                creatorName = invite.creatorName,
            )
        }
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

    /*
     * Why: a Google or Apple account already has a name and a photo, so it joins as a new member with them at once
     * (beta feedback 3, item 22). An anonymous or local only one, or a profile name too short for a member, picks a
     * name in "Entrar com outro nome".
     */
    private fun openNewMember() {
        if (uiState.value.isJoining || isOpeningNewMember) return
        isOpeningNewMember = true
        viewModelScope.launch {
            val name = accountNames.first()?.let(::availableNameOf)?.let { GroupNameRules.toCapped(it).trimEnd() }
            isOpeningNewMember = false
            if (name != null) {
                join(JoinChoice.AsNewMember(name))
            } else {
                uiState.update { it.copy(newMember = NewMemberDraft(name = "")) }
            }
        }
    }

    // Why: the name the sheet is joining as stays put until the server answers.
    private fun changeNewName(name: String) {
        if (uiState.value.isJoining) return
        uiState.update { state ->
            state.copy(newMember = state.newMember?.copy(name = GroupNameRules.toCapped(name)))
        }
    }

    // Why: a join on its way keeps its sheet, whose button shows it, until the server answers.
    private fun dismissNewMember() {
        if (!uiState.value.isJoining) uiState.update { it.copy(newMember = null) }
    }

    // Why: the name may repeat a member's (beta feedback 3, item 29): people are told apart by their ids.
    private fun joinAsNewMember() {
        val draft = uiState.value.newMember ?: return
        val name = availableNameOf(draft.name) ?: return
        if (uiState.value.canJoinAsNewMember) join(JoinChoice.AsNewMember(name))
    }

    private fun availableNameOf(name: String): String? = (checkMemberName(name) as? MemberNameCheck.Available)?.name

    private fun join(choice: JoinChoice) {
        val invite = invite ?: return
        uiState.update {
            it.copy(
                joining = when (choice) {
                    is JoinChoice.AsMember -> JoinTarget.MEMBER
                    is JoinChoice.AsNewMember -> JoinTarget.NEW_MEMBER
                },
            )
        }
        viewModelScope.launch {
            joinGroup(invite, choice)
                .onSuccess { outcome -> handle(outcome, choice) }
                .onFailure { uiAction.emit(JoinPickUiAction.ShowJoinError) }
            uiState.update { it.copy(joining = null) }
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

    private fun openGroup(
        groupId: String,
        joinedAs: String?,
    ) {
        if (isOnTop()) navigator.openJoinedGroup(groupId, joinedAs)
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }
}
