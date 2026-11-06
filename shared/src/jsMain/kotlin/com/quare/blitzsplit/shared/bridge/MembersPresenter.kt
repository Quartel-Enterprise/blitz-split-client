@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.members.presentation.model.MemberNameEditor
import com.quare.blitzsplit.feature.members.presentation.model.MembersMember
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiAction
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent
import com.quare.blitzsplit.feature.members.presentation.model.PersonalInvite
import com.quare.blitzsplit.feature.members.presentation.viewmodel.MembersViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

class JsMembersMember(
    val id: String,
    val name: String,
    val initials: String,
    val color: String,
    val photoUrl: String?,
    val isMe: Boolean,
    val hasJoined: Boolean,
    val canBeRemoved: Boolean,
    val canBeInvited: Boolean,
    // Why: "Convite pessoal para Carla" stays disabled until the member's link exists.
    val isPersonalInviteReady: Boolean,
    // Why: "+R$ 223,50 a receber", "−R$ 118,00 deve" or "Quitado", null while the group has no bill.
    val balance: JsMemberBalance?,
    // Why: "Carla tem saldo em aberto" under the disabled "Remover do grupo".
    val hasOpenBalance: Boolean,
    // Why: "Liberar nome", for the group's creator on someone else who joined.
    val canBeReleased: Boolean,
)

class JsPersonalInvite(
    val memberName: String,
    val memberInitials: String,
    val memberColor: String,
    val displayLink: String,
    val isLinkCopied: Boolean,
    // Why: "e já vê a parte de R$ 300,00 na despesa lançada", left out while 0.
    val billCount: Int,
    val billShareMinor: Double,
)

// Why: "change-error" and "release-error", toasts in the page's words; "copy" and "share-personal" with the link.
class JsMembersAction(
    val type: String,
    val link: String?,
    val memberName: String?,
    val groupName: String?,
)

class JsMemberNameEditor(
    val isAdding: Boolean,
    val draft: String,
    val canSave: Boolean,
)

class JsMembersState(
    val groupName: String,
    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator.
    val currency: String,
    val fractionDigits: Int,
    // Why: the currency chips over the balances, drawn only with more than one.
    val currencies: Array<String>,
    val displayLink: String?,
    val members: Array<JsMembersMember>,
    val editor: JsMemberNameEditor?,
    // Why: the name in "Remover Carla do grupo?", null while nobody is about to be removed.
    val memberToRemoveName: String?,
    // Why: the name in "Liberar o nome Carla?", null while nobody is about to be released.
    val memberToReleaseName: String?,
    // Why: "Liberar" waits for the server with the confirmation open.
    val isReleasing: Boolean,
    // Why: the "Convite pessoal" sheet, null while closed.
    val personalInvite: JsPersonalInvite?,
    val isLoaded: Boolean,
)

// Why: Membros' content, drawn under the group's header, which GroupDetailPresenter keeps.
class MembersPresenter(
    route: JsRoute,
) {
    private val membersRoute = route.route as Route.GroupDetail
    private val holder = ViewModelHolder(MembersViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(membersRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsMembersState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsMembersState(
                groupName = state.groupName,
                currency = state.currency.code,
                fractionDigits = state.currency.fractionDigits,
                currencies = state.currencies.toCurrencyCodes(),
                displayLink = state.displayLink,
                members = state.members.map(::toJsMembersMember).toTypedArray(),
                editor = state.editor?.let(::toJsMemberNameEditor),
                memberToRemoveName = state.memberToRemove?.name,
                memberToReleaseName = state.memberToRelease?.name,
                isReleasing = state.isReleasing,
                personalInvite = state.personalInvite?.let(::toJsPersonalInvite),
                isLoaded = state.isLoaded,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeActions(onAction: (JsMembersAction) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                MembersUiAction.ShowChangeError -> JsMembersAction("change-error", null, null, null)

                MembersUiAction.ShowReleaseError -> JsMembersAction("release-error", null, null, null)

                is MembersUiAction.CopyLink -> JsMembersAction("copy", action.link, null, null)

                is MembersUiAction.SharePersonalLink -> JsMembersAction(
                    type = "share-personal",
                    link = action.link,
                    memberName = action.memberName,
                    groupName = action.groupName,
                )
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    // Why: a currency chip over the members' balances.
    fun selectCurrency(code: String) {
        val currency = GroupCurrency.fromCode(code)
        viewModel.onEvent(MembersUiEvent.OnCurrencySelected(currency))
    }

    fun share() {
        viewModel.onEvent(MembersUiEvent.OnShareClicked)
    }

    fun add() {
        viewModel.onEvent(MembersUiEvent.OnAddClicked)
    }

    // Why: "Has the app? Invite with the link" in "Add person", which opens Invite in its place.
    fun inviteInstead() {
        viewModel.onEvent(MembersUiEvent.OnInviteInsteadClicked)
    }

    // Why: the member's row, which opens their profile (Perfil for this device's user).
    fun openMember(memberId: String) {
        viewModel.onEvent(MembersUiEvent.OnMemberClicked(memberId))
    }

    fun rename(memberId: String) {
        viewModel.onEvent(MembersUiEvent.OnRenameClicked(memberId))
    }

    fun personalInvite(memberId: String) {
        viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked(memberId))
    }

    fun dismissPersonalInvite() {
        viewModel.onEvent(MembersUiEvent.OnPersonalInviteDismissed)
    }

    fun copyPersonalLink() {
        viewModel.onEvent(MembersUiEvent.OnPersonalLinkCopyClicked)
    }

    fun sharePersonalLink() {
        viewModel.onEvent(MembersUiEvent.OnPersonalLinkShareClicked)
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

    fun release(memberId: String) {
        viewModel.onEvent(MembersUiEvent.OnReleaseClicked(memberId))
    }

    fun confirmRelease() {
        viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)
    }

    fun dismissRelease() {
        viewModel.onEvent(MembersUiEvent.OnReleaseDismissed)
    }

    fun changeDraft(draft: String) {
        viewModel.onEvent(MembersUiEvent.OnDraftChanged(draft))
    }

    // Why: the ViewModel caps the draft as it handles it, so the field reads the capped text back in the same keystroke.
    fun getDraft(): String = viewModel.uiState.value.editor
        ?.draft
        .orEmpty()

    fun save() {
        viewModel.onEvent(MembersUiEvent.OnSaveClicked)
    }

    fun dismissEditor() {
        viewModel.onEvent(MembersUiEvent.OnEditorDismissed)
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
    photoUrl = member.photoUrl,
    isMe = member.isMe,
    hasJoined = member.hasJoined,
    canBeRemoved = member.canBeRemoved,
    canBeInvited = member.canBeInvited,
    isPersonalInviteReady = member.isPersonalInviteReady,
    balance = member.balance?.let(::toJsMemberBalance),
    hasOpenBalance = member.hasOpenBalance,
    canBeReleased = member.canBeReleased,
)

private fun toJsPersonalInvite(invite: PersonalInvite): JsPersonalInvite = JsPersonalInvite(
    memberName = invite.member.name,
    memberInitials = invite.member.initials,
    memberColor = invite.member.avatarColor.code,
    displayLink = invite.displayLink,
    isLinkCopied = invite.isLinkCopied,
    billCount = invite.billCount,
    billShareMinor = invite.billShareMinor.toDouble(),
)

private fun toJsMemberNameEditor(editor: MemberNameEditor): JsMemberNameEditor = JsMemberNameEditor(
    isAdding = editor.isAdding,
    draft = editor.draft,
    canSave = editor.canSave,
)
