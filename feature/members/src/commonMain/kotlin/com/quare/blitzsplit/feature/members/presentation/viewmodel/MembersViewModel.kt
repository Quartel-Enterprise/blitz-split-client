package com.quare.blitzsplit.feature.members.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.expenses.domain.RecordedPaymentNotices
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveRecordedPayment
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.groups.domain.model.MemberNameCheck
import com.quare.blitzsplit.core.groups.domain.usecase.AddMember
import com.quare.blitzsplit.core.groups.domain.usecase.CheckMemberName
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.ReleaseMember
import com.quare.blitzsplit.core.groups.domain.usecase.RemoveMember
import com.quare.blitzsplit.core.groups.domain.usecase.RenameMember
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openGroupTab
import com.quare.blitzsplit.core.presentation.CopiedFeedback
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.feature.members.presentation.model.MemberNameEditor
import com.quare.blitzsplit.feature.members.presentation.model.MembersMember
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiAction
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiState
import com.quare.blitzsplit.feature.members.presentation.model.PersonalInvite
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
    private val releaseMember: ReleaseMember,
    consumeRecordedPayment: ConsumeRecordedPayment,
    deleteSettlement: DeleteSettlement,
    observeGroup: ObserveGroup,
    observeGroupBalances: ObserveGroupBalances,
    observeRecordedPayment: ObserveRecordedPayment,
    observeAccount: ObserveAccount,
) : ViewModel() {
    val uiState: StateFlow<MembersUiState>
        field = MutableStateFlow(
            MembersUiState(
                groupName = "",
                currency = GroupCurrency.BRL,
                displayLink = null,
                members = emptyList(),
                editor = null,
                memberToRemove = null,
                memberToRelease = null,
                isReleasing = false,
                personalInvite = null,
                myBalance = null,
                isEveryoneSettled = false,
                recordedPayment = null,
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

    private var copiedReset: Job? = null

    private val notices = RecordedPaymentNotices(consumeRecordedPayment, deleteSettlement)

    private var group: Group? = null
    private var balances = GroupBalances(
        debts = emptyList(),
        hasBills = false,
    )

    /*
     * Why: a sync may bring other members' changes, bills and payments while the screen is open, a removal of the
     * whole group included.
     */
    init {
        combine(observeGroup(route.groupId), observeGroupBalances(route.groupId), ::Pair)
            .onEach { (group, balances) ->
                this.group = group
                this.balances = balances
                isGroupGone = group == null
                if (group == null) leave() else show(group)
            }.launchIn(viewModelScope)
        notices.watch(
            scope = viewModelScope,
            recorded = observeRecordedPayment(route.groupId),
            backStack = navigator.backStack,
            isLoaded = uiState.map { it.isLoaded },
            isOnTop = ::isOnTop,
            meId = { group?.members?.firstOrNull { it.isMe }?.id },
            nameOf = { id -> group?.members?.firstOrNull { it.id == id }?.name },
            show = { notice -> uiState.update { it.copy(recordedPayment = notice) } },
        )
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

            MembersUiEvent.OnShareClicked -> if (isOnTop()) {
                navigator.navigate(
                    Route.Invite(route.groupId, isNewGroup = false),
                )
            }

            MembersUiEvent.OnExpensesTabClicked -> navigator.openGroupTab(
                from = route,
                tab = Route.GroupDetail(route.groupId),
            )

            MembersUiEvent.OnBalancesTabClicked -> navigator.openGroupTab(
                from = route,
                tab = Route.Balances(route.groupId),
            )

            MembersUiEvent.OnAddClicked -> openEditor(
                memberId = null,
                draft = "",
            )

            is MembersUiEvent.OnRenameClicked -> openRename(event.memberId)

            is MembersUiEvent.OnPersonalInviteClicked -> openPersonalInvite(event.memberId)

            MembersUiEvent.OnPersonalInviteDismissed -> closePersonalInvite()

            MembersUiEvent.OnPersonalLinkCopyClicked -> copyPersonalLink()

            MembersUiEvent.OnPersonalLinkShareClicked -> sharePersonalLink()

            is MembersUiEvent.OnRemoveClicked -> askToRemove(event.memberId)

            MembersUiEvent.OnRemoveConfirmed -> remove()

            MembersUiEvent.OnRemoveDismissed -> uiState.update { it.copy(memberToRemove = null) }

            is MembersUiEvent.OnReleaseClicked -> askToRelease(event.memberId)

            MembersUiEvent.OnReleaseConfirmed -> release()

            MembersUiEvent.OnReleaseDismissed -> dismissRelease()

            is MembersUiEvent.OnDraftChanged -> changeDraft(event.draft)

            is MembersUiEvent.OnSuggestionClicked -> saveSuggestion(event.name)

            MembersUiEvent.OnSaveClicked -> save()

            MembersUiEvent.OnEditorDismissed -> uiState.update { it.copy(editor = null) }

            MembersUiEvent.OnSettleClicked -> openSettle()

            MembersUiEvent.OnUndoPaymentClicked -> undoRecordedPayment()

            MembersUiEvent.OnRecordedPaymentNoticeShown -> uiState.update { it.copy(recordedPayment = null) }

            MembersUiEvent.OnProtectAccessClicked -> if (isOnTop()) navigator.navigate(Route.Profile)
        }
    }

    private fun show(group: Group) {
        val inviterCode = group.members.firstOrNull { it.isMe }?.inviteCode
        val members = group.members.map { toMembersMember(it, group, inviterCode) }
        val meId = group.members.firstOrNull { it.isMe }?.id
        uiState.update { state ->
            state.copy(
                groupName = group.name,
                currency = group.currency,
                myBalance = balances.balanceOrNull(meId),
                isEveryoneSettled = balances.isEveryoneSettled,
                displayLink = group.inviteCode?.let(InviteLink::of)?.let(InviteLink::displayOf),
                members = members,
                // Why: the member about to be removed may be gone, or have joined, by now.
                memberToRemove = state.memberToRemove?.let { asked ->
                    members.firstOrNull { it.id == asked.id && it.canBeRemoved }
                },
                // Why: the same for a release, unless it is already on its way: its answer closes the confirmation.
                memberToRelease = state.memberToRelease?.let { asked ->
                    if (state.isReleasing) asked else members.firstOrNull { it.id == asked.id && it.canBeReleased }
                },
                // Why: the member invited may be gone, or have joined, by now, or have another name.
                personalInvite = state.personalInvite?.let { open ->
                    members
                        .firstOrNull { it.id == open.member.id && it.isPersonalInviteReady }
                        ?.let { member -> personalInviteOf(member, open.isLinkCopied) }
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

    private fun toMembersMember(
        member: GroupMember,
        group: Group,
        inviterCode: String?,
    ): MembersMember = MembersMember(
        id = member.id,
        name = member.name,
        initials = Initials.ofMember(member.name, member.isMe),
        avatarColor = member.avatarColor,
        isMe = member.isMe,
        hasJoined = member.hasJoined,
        personalLink = member.inviteCode?.let { memberCode ->
            group.inviteCode?.let {
                InviteLink.personalOf(
                    inviteCode = it,
                    memberCode = memberCode,
                    inviterCode = inviterCode,
                )
            }
        },
        balance = balances.balanceOrNull(member.id),
        canBeReleased = group.isCreatedByMe && member.hasJoined && !member.isMe,
    )

    // Why: "Acertar" on the balance card: this person pays whoever they owe the most.
    private fun openSettle() {
        val meId = group?.members?.firstOrNull { it.isMe }?.id ?: return
        val creditorId = balances.findLargestCreditorOrNull(meId) ?: return
        if (isOnTop()) navigator.navigate(Route.RegisterPayment(route.groupId, meId, creditorId))
    }

    private fun undoRecordedPayment() {
        val notice = uiState.value.recordedPayment ?: return
        uiState.update { it.copy(recordedPayment = null) }
        notices.delete(viewModelScope, notice.settlementId) { uiAction.emit(MembersUiAction.ShowChangeError) }
    }

    private fun personalInviteOf(
        member: MembersMember,
        isLinkCopied: Boolean,
    ): PersonalInvite? {
        val link = member.personalLink ?: return null
        return PersonalInvite(
            member = member,
            link = link,
            displayLink = InviteLink.displayOf(link),
            isLinkCopied = isLinkCopied,
        )
    }

    private fun openPersonalInvite(memberId: String) {
        val member = uiState.value.members.firstOrNull { it.id == memberId && it.isPersonalInviteReady } ?: return
        copiedReset?.cancel()
        uiState.update { it.copy(personalInvite = personalInviteOf(member, isLinkCopied = false)) }
    }

    private fun closePersonalInvite() {
        copiedReset?.cancel()
        uiState.update { it.copy(personalInvite = null) }
    }

    private fun copyPersonalLink() {
        val invite = uiState.value.personalInvite ?: return
        send(MembersUiAction.CopyLink(invite.link))
        uiState.update { state -> state.copy(personalInvite = state.personalInvite?.copy(isLinkCopied = true)) }
        copiedReset?.cancel()
        copiedReset = viewModelScope.launch {
            delay(CopiedFeedback.duration)
            uiState.update { state -> state.copy(personalInvite = state.personalInvite?.copy(isLinkCopied = false)) }
        }
    }

    private fun sharePersonalLink() {
        val state = uiState.value
        val invite = state.personalInvite ?: return
        send(
            MembersUiAction.SharePersonalLink(
                link = invite.link,
                memberName = invite.member.name,
                groupName = state.groupName,
            ),
        )
    }

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

    /*
     * Why: one release at a time. Apple's alert is gone while one is on its way, so another member's "Liberar nome"
     * can be tapped, and would otherwise take the place of the one the answer closes.
     */
    private fun askToRelease(memberId: String) {
        val member = uiState.value.members.firstOrNull { it.id == memberId } ?: return
        if (member.canBeReleased) uiState.update { if (it.isReleasing) it else it.copy(memberToRelease = member) }
    }

    // Why: a release on its way can't be called back, so its confirmation waits for the answer.
    private fun dismissRelease() {
        uiState.update { if (it.isReleasing) it else it.copy(memberToRelease = null) }
    }

    // Why: online only, so the confirmation stays open until the server answers, and closes either way.
    private fun release() {
        val state = uiState.value
        val member = state.memberToRelease ?: return
        if (state.isReleasing) return
        uiState.update { it.copy(isReleasing = true) }
        viewModelScope.launch {
            val result = releaseMember(member.id)
            uiState.update {
                it.copy(
                    memberToRelease = null,
                    isReleasing = false,
                )
            }
            result.onFailure { uiAction.emit(MembersUiAction.ShowReleaseError) }
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
