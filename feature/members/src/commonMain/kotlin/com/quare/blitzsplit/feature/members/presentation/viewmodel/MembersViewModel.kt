package com.quare.blitzsplit.feature.members.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.CurrencyBalances
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.MemberBillParts
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.currencies
import com.quare.blitzsplit.core.expenses.domain.model.findSelected
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettlements
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.groups.domain.model.MemberNameCheck
import com.quare.blitzsplit.core.groups.domain.usecase.AddMember
import com.quare.blitzsplit.core.groups.domain.usecase.CheckMemberName
import com.quare.blitzsplit.core.groups.domain.usecase.ConsumeAddPersonRequest
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveAddPersonRequest
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.ReleaseMember
import com.quare.blitzsplit.core.groups.domain.usecase.RemoveMember
import com.quare.blitzsplit.core.groups.domain.usecase.RenameMember
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openFromGroupScreen
import com.quare.blitzsplit.core.navigation.personProfileRouteOf
import com.quare.blitzsplit.core.presentation.CopiedFeedback
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: Membros, the content under the group's header when its Membros tab is picked: who is in the group and who
 * joined, and what can be done to them. The header, the tabs and leaving the group are the group's screen's
 * (GroupDetailViewModel); route is that screen's, which this one opens Convidar over.
 */
class MembersViewModel(
    private val route: Route.GroupDetail,
    private val navigator: Navigator,
    private val checkMemberName: CheckMemberName,
    private val addMember: AddMember,
    private val renameMember: RenameMember,
    private val removeMember: RemoveMember,
    private val releaseMember: ReleaseMember,
    private val consumeAddPersonRequest: ConsumeAddPersonRequest,
    observeAddPersonRequest: ObserveAddPersonRequest,
    observeGroup: ObserveGroup,
    observeExpenses: ObserveExpenses,
    observeSettlements: ObserveSettlements,
    trackEvent: TrackEvent,
) : TrackedViewModel<MembersUiEvent>(trackEvent) {
    val uiState: StateFlow<MembersUiState>
        field = MutableStateFlow(
            MembersUiState(
                groupName = "",
                currency = GroupCurrency.BRL,
                currencies = emptyList(),
                displayLink = null,
                members = emptyList(),
                editor = null,
                memberToRemove = null,
                memberToRelease = null,
                isReleasing = false,
                personalInvite = null,
                isLoaded = false,
            ),
        )

    val uiAction: SharedFlow<MembersUiAction>
        field = MutableSharedFlow<MembersUiAction>()

    val stateWatcher: FlowWatcher<MembersUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<MembersUiAction> = FlowWatcher(uiAction)

    private var copiedReset: Job? = null

    // Why: one balance per currency the group has money in; the screen shows the one picked, and removal reads all.
    private var balances: List<CurrencyBalances> = emptyList()

    // Why: the chip picked, the group's default currency until one is.
    private var pickedCurrency: GroupCurrency? = null

    private var shownGroup: Group? = null

    private var expenses: List<Expense> = emptyList()

    // Why: "Add person" is open from Invite's ask, which goes once it closes.
    private var isEditorFromRequest = false

    // Why: the group's members as last read, whose account or name keys the profile a row opens.
    private var groupMembers: List<GroupMember> = emptyList()

    /*
     * Why: a sync may bring other members' changes, bills and payments while the screen is open. A group that goes is
     * the group's screen's to close. The bills are read once, for the balances and the personal invite alike.
     */
    init {
        combine(
            observeGroup(route.groupId),
            observeExpenses(route.groupId),
            observeSettlements(route.groupId),
        ) { group, expenses, settlements ->
            this.balances = group?.let { GroupBalances.getByCurrency(it, expenses, settlements) }.orEmpty()
            this.expenses = expenses
            if (group != null) show(group)
        }.launchIn(viewModelScope)
        /*
         * Why: Invite's "Add someone without the app". Only the group's screen on top opens it: another one of the same
         * group may still be alive under it, on a tab it is leaving. The ask stays until the dialog closes, as a notice
         * waits to be shown: a screen mounted twice (React's StrictMode) clears the first ViewModel right away.
         */
        combine(
            observeAddPersonRequest(),
            navigator.backStack,
            /*
             * Why: only a trigger, once the members are read (the name is checked against them) and no other name
             * editor is open; the state itself is read below, since combine may hand an older value.
             */
            uiState.map { it.canOpenAskedEditor }.distinctUntilChanged(),
        ) { groupId, backStack, _ ->
            val isAsked = groupId == route.groupId && backStack.lastOrNull() == route
            if (isAsked && !isEditorFromRequest && uiState.value.canOpenAskedEditor) {
                isEditorFromRequest = true
                openEditor(
                    memberId = null,
                    draft = "",
                )
            }
        }.launchIn(viewModelScope)
    }

    override fun handleEvent(event: MembersUiEvent) {
        when (event) {
            // Why: over the group, or in the place of its bill the Mac draws beside it.
            MembersUiEvent.OnShareClicked -> openInvite()

            MembersUiEvent.OnLinkCopyClicked -> copyGroupLink()

            is MembersUiEvent.OnCurrencySelected -> pickCurrency(event.currency)

            // Why: Invite opens first, so an ask still waiting doesn't open "Add person" under it as this one closes.
            MembersUiEvent.OnInviteInsteadClicked -> {
                openInvite()
                closeEditor()
            }

            MembersUiEvent.OnAddClicked -> openEditor(
                memberId = null,
                draft = "",
            )

            is MembersUiEvent.OnMemberClicked -> openProfile(event.memberId)

            is MembersUiEvent.OnRenameClicked -> openRename(event.memberId)

            is MembersUiEvent.OnPersonalInviteClicked -> openPersonalInvite(event.memberId)

            MembersUiEvent.OnPersonalInviteDismissed -> closePersonalInvite()

            MembersUiEvent.OnPersonalLinkCopyClicked -> copyPersonalLink()

            MembersUiEvent.OnPersonalLinkShareClicked -> sharePersonalLink()

            is MembersUiEvent.OnRemoveClicked -> askToRemove(event.memberId)

            MembersUiEvent.OnRemoveConfirmed -> remove()

            MembersUiEvent.OnRemoveDismissed -> dismissRemove()

            is MembersUiEvent.OnReleaseClicked -> askToRelease(event.memberId)

            MembersUiEvent.OnReleaseConfirmed -> release()

            MembersUiEvent.OnReleaseDismissed -> dismissRelease()

            is MembersUiEvent.OnDraftChanged -> changeDraft(event.draft)

            MembersUiEvent.OnSaveClicked -> save()

            MembersUiEvent.OnEditorDismissed -> closeEditor()
        }
    }

    private val MembersUiState.canOpenAskedEditor: Boolean
        get() = isLoaded && editor == null

    private fun openInvite() {
        navigator.openFromGroupScreen(
            from = route,
            groupId = route.groupId,
            next = Route.Invite(route.groupId, isNewGroup = false),
        )
    }

    // Why: the balances under the chip picked; a pick the group has no money in anymore falls back to the default.
    private fun pickCurrency(currency: GroupCurrency) {
        if (balances.none { it.currency == currency }) return
        pickedCurrency = currency
        shownGroup?.let(::show)
    }

    private fun show(group: Group) {
        shownGroup = group
        groupMembers = group.members
        val inviterCode = group.members.firstOrNull { it.isMe }?.inviteCode
        val members = group.members.map { toMembersMember(it, group, inviterCode) }
        uiState.update { state ->
            state.copy(
                groupName = group.name,
                currency = balances.findSelected(pickedCurrency)?.currency ?: group.currency,
                currencies = balances.currencies,
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
        // Why: the member being renamed may be gone.
        val editor = uiState.value.editor ?: return
        if (editor.memberId != null && uiState.value.members.none { it.id == editor.memberId }) closeEditor()
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
        photoUrl = member.photoUrl,
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
        balance = balances.findSelected(pickedCurrency)?.balances?.balanceOrNull(member.id),
        // Why: someone who owes or is owed in any currency stays, whichever chip is picked.
        hasOpenBalance = balances.any { it.balances.balanceOrNull(member.id)?.isSettled == false },
        canBeReleased = group.isCreatedByMe && member.hasJoined && !member.isMe,
    )

    private fun personalInviteOf(
        member: MembersMember,
        isLinkCopied: Boolean,
    ): PersonalInvite? {
        val link = member.personalLink ?: return null
        val currency = balances.findSelected(pickedCurrency)?.currency
        val parts = MemberBillParts.of(expenses.filter { it.currency == currency }, member.id)
        return PersonalInvite(
            member = member,
            link = link,
            displayLink = InviteLink.displayOf(link),
            isLinkCopied = isLinkCopied,
            billCount = parts.billCount,
            billShareMinor = parts.shareMinor,
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

    // Why: nothing to copy until the group's first sync brings the code the server made (`canCopyLink`).
    private fun copyGroupLink() {
        val code = shownGroup?.inviteCode ?: return
        send(MembersUiAction.CopyGroupLink(InviteLink.of(code)))
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

    // Why: over the group, or in the place of its bill the Mac draws beside it, as Convidar opens.
    private fun openProfile(memberId: String) {
        val member = groupMembers.firstOrNull { it.id == memberId } ?: return
        val next = personProfileRouteOf(PeopleBalances.keyOf(member), member.isMe) ?: return
        navigator.openFromGroupScreen(
            from = route,
            groupId = route.groupId,
            next = next,
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
                ),
            )
        }
    }

    // Why: the ask goes before the editor does, since an editor closing may open the next ask's right away.
    private fun closeEditor() {
        if (isEditorFromRequest) {
            isEditorFromRequest = false
            consumeAddPersonRequest(route.groupId)
        }
        uiState.update { it.copy(editor = null) }
    }

    // Why: leaving the group with the dialog open drops the ask too; a screen mounted again keeps it for the next one.
    override fun onCleared() {
        if (isEditorFromRequest && route !in navigator.backStack.value) consumeAddPersonRequest(route.groupId)
        super.onCleared()
    }

    private fun changeDraft(draft: String) {
        uiState.update { state ->
            state.copy(editor = state.editor?.copy(draft = GroupNameRules.toCapped(draft)))
        }
    }

    private fun save() {
        val editor = uiState.value.editor ?: return
        val check = checkMemberName(editor.draft) as? MemberNameCheck.Available ?: return
        save(editor, check.name)
    }

    // Why: the dialog closes right away; the write is local, and only a failure has something to say.
    private fun save(
        editor: MemberNameEditor,
        name: String,
    ) {
        closeEditor()
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
        trackEvent("member_remove_confirmed", emptyMap())
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

    private fun dismissRemove() {
        if (uiState.value.memberToRemove == null) return
        trackEvent("member_remove_cancelled", emptyMap())
        uiState.update { it.copy(memberToRemove = null) }
    }

    // Why: a release on its way can't be called back, so its confirmation waits for the answer.
    private fun dismissRelease() {
        val state = uiState.value
        if (state.memberToRelease == null || state.isReleasing) return
        trackEvent("member_release_cancelled", emptyMap())
        uiState.update { it.copy(memberToRelease = null) }
    }

    // Why: online only, so the confirmation stays open until the server answers, and closes either way.
    private fun release() {
        val state = uiState.value
        val member = state.memberToRelease ?: return
        if (state.isReleasing) return
        trackEvent("member_release_confirmed", emptyMap())
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
}
