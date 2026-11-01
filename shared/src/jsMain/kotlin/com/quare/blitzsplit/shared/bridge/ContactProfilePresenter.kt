@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.ContactGroup
import com.quare.blitzsplit.core.contacts.domain.model.ContactHistoryItem
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.contacts.presentation.model.AddGroupOption
import com.quare.blitzsplit.feature.contacts.presentation.model.AddToGroupDraft
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiState
import com.quare.blitzsplit.feature.contacts.presentation.model.GuestMergeSheet
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactProfileViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

// Why: one of "Grupos em comum"; amountMinor is positive when they owe you in that group.
class JsContactGroup(
    val groupId: String,
    val name: String,
    val initials: String,
    val color: String,
    val currency: String,
    val fractionDigits: Int,
    val amountMinor: Double,
)

class JsContact(
    val key: String,
    val name: String,
    val initials: String,
    val color: String,
    val isGuest: Boolean,
    val currency: String,
    val fractionDigits: Int,
    // Why: positive when they owe you, negative when you owe them, in the first currency with something open.
    val netMinor: Double,
    val groups: Array<JsContactGroup>,
)

/*
 * Why: one of "Histórico com Carla". type is "bill" or "payment"; a bill fills description, payerName, isPaidByMe,
 * payerCount, totalMinor and effectMinor (positive when it puts it on them), a payment isFromMe, amountMinor and
 * method (the SettlementMethod's code).
 */
class JsContactHistoryItem(
    val id: String,
    val type: String,
    val groupName: String,
    val currency: String,
    val fractionDigits: Int,
    val at: Double,
    val description: String,
    val payerName: String?,
    val isPaidByMe: Boolean,
    val payerCount: Int,
    val totalMinor: Double,
    val effectMinor: Double,
    val isFromMe: Boolean,
    val amountMinor: Double,
    val method: String,
)

class JsAddGroupOption(
    val groupId: String,
    val name: String,
    val initials: String,
    val color: String,
    val memberCount: Int,
    val isAlreadyIn: Boolean,
)

class JsAddToGroup(
    val options: Array<JsAddGroupOption>,
    val selectedGroupId: String?,
    val canAdd: Boolean,
)

// Why: "Juntar Diego com a conta dele": the personal link once the server made it.
class JsGuestMergeSheet(
    val displayLink: String?,
    val isRequesting: Boolean,
    val hasFailed: Boolean,
)

class JsContactProfileState(
    val isLoaded: Boolean,
    // Why: null until Room is read, and once a sync took the person away.
    val contact: JsContact?,
    val history: Array<JsContactHistoryItem>,
    val hasMoreHistory: Boolean,
    val canCharge: Boolean,
    val canReceive: Boolean,
    val canSettle: Boolean,
    val canHide: Boolean,
    val canShow: Boolean,
    val canMerge: Boolean,
    val showsGuestCard: Boolean,
    val showsPendingMerge: Boolean,
    // Why: "Convite pessoal enviado hoje, 14:20", in epoch millis; null while nothing waits.
    val pendingMergeRequestedAt: Double?,
    val isCancellingMerge: Boolean,
    val isNetOfSeveralGroups: Boolean,
    val showsOfflineNotice: Boolean,
    val isOnline: Boolean,
    val isHideConfirmOpen: Boolean,
    val addToGroup: JsAddToGroup?,
    val merge: JsGuestMergeSheet?,
)

/*
 * Why: "coming-soon", "copy" and "share" (with the link, and the person's name for the message), "added" (the name
 * and the group's), "change-error" or "cancel-merge-error".
 */
class JsContactProfileAction(
    val type: String,
    val link: String?,
    val name: String?,
    val groupName: String?,
)

/*
 * Why: built from the person's key rather than a JsRoute, since the desktop draws the profile beside Contatos without
 * pushing its route; the ViewModel gets the same Route.ContactProfile either way.
 */
class ContactProfilePresenter(
    personKey: String,
) {
    private val profileRoute = Route.ContactProfile(personKey)
    private val holder = ViewModelHolder(ContactProfileViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(profileRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsContactProfileState) -> Unit): JsWatch {
        val states = viewModel.uiState.map(::toJsContactProfileState)
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeActions(onAction: (JsContactProfileAction) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                ContactProfileUiAction.ShowComingSoon -> JsContactProfileAction("coming-soon", null, null, null)

                is ContactProfileUiAction.CopyLink -> JsContactProfileAction("copy", action.link, null, null)

                is ContactProfileUiAction.ShareLink -> JsContactProfileAction("share", action.link, action.name, null)

                is ContactProfileUiAction.ShowAddedToGroup -> JsContactProfileAction(
                    "added",
                    null,
                    action.name,
                    action.groupName,
                )

                ContactProfileUiAction.ShowChangeError -> JsContactProfileAction("change-error", null, null, null)

                ContactProfileUiAction.ShowCancelMergeError -> JsContactProfileAction(
                    "cancel-merge-error",
                    null,
                    null,
                    null,
                )
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(ContactProfileUiEvent.OnBackClicked)
    }

    fun charge() {
        viewModel.onEvent(ContactProfileUiEvent.OnChargeClicked)
    }

    fun received() {
        viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)
    }

    fun settle() {
        viewModel.onEvent(ContactProfileUiEvent.OnSettleClicked)
    }

    fun addToGroup() {
        viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)
    }

    fun pickAddGroup(groupId: String) {
        viewModel.onEvent(ContactProfileUiEvent.OnAddGroupPicked(groupId))
    }

    fun confirmAddToGroup() {
        viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupConfirmed)
    }

    fun dismissAddToGroup() {
        viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupDismissed)
    }

    fun hide() {
        viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)
    }

    fun confirmHide() {
        viewModel.onEvent(ContactProfileUiEvent.OnHideConfirmed)
    }

    fun dismissHide() {
        viewModel.onEvent(ContactProfileUiEvent.OnHideDismissed)
    }

    fun show() {
        viewModel.onEvent(ContactProfileUiEvent.OnShowClicked)
    }

    fun viewBalances() {
        viewModel.onEvent(ContactProfileUiEvent.OnViewBalancesClicked)
    }

    fun seeAllHistory() {
        viewModel.onEvent(ContactProfileUiEvent.OnSeeAllHistoryClicked)
    }

    fun inviteGuest() {
        viewModel.onEvent(ContactProfileUiEvent.OnInviteGuestClicked)
    }

    fun merge() {
        viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
    }

    fun retryMerge() {
        viewModel.onEvent(ContactProfileUiEvent.OnMergeRetryClicked)
    }

    fun copyMerge() {
        viewModel.onEvent(ContactProfileUiEvent.OnMergeCopyClicked)
    }

    fun shareMerge() {
        viewModel.onEvent(ContactProfileUiEvent.OnMergeShareClicked)
    }

    fun dismissMerge() {
        viewModel.onEvent(ContactProfileUiEvent.OnMergeDismissed)
    }

    fun askAppUser() {
        viewModel.onEvent(ContactProfileUiEvent.OnAskAppUserClicked)
    }

    fun cancelMerge() {
        viewModel.onEvent(ContactProfileUiEvent.OnCancelMergeClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsContactProfileState(state: ContactProfileUiState): JsContactProfileState = JsContactProfileState(
    isLoaded = state.isLoaded,
    contact = state.contact?.let { toJsContact(it, state.initials) },
    history = state.history.map(::toJsContactHistoryItem).toTypedArray(),
    hasMoreHistory = state.hasMoreHistory,
    canCharge = state.canCharge,
    canReceive = state.canReceive,
    canSettle = state.canSettle,
    canHide = state.canHide,
    canShow = state.canShow,
    canMerge = state.canMerge,
    showsGuestCard = state.showsGuestCard,
    showsPendingMerge = state.showsPendingMerge,
    pendingMergeRequestedAt = state.pendingMerge?.requestedAt?.toDouble(),
    isCancellingMerge = state.isCancellingMerge,
    isNetOfSeveralGroups = state.isNetOfSeveralGroups,
    showsOfflineNotice = state.showsOfflineNotice,
    isOnline = state.isOnline,
    isHideConfirmOpen = state.isHideConfirmOpen,
    addToGroup = state.addToGroup?.let(::toJsAddToGroup),
    merge = state.merge?.let(::toJsGuestMergeSheet),
)

private fun toJsContact(
    contact: Contact,
    initials: String,
): JsContact = JsContact(
    key = contact.key,
    name = contact.name,
    initials = initials,
    color = contact.color.code,
    isGuest = contact.isGuest,
    currency = contact.net.currency.code,
    fractionDigits = contact.net.currency.fractionDigits,
    netMinor = contact.net.amountMinor
        .toDouble(),
    groups = contact.groups.map(::toJsContactGroup).toTypedArray(),
)

private fun toJsContactGroup(group: ContactGroup): JsContactGroup = JsContactGroup(
    groupId = group.groupId,
    name = group.groupName,
    initials = group.groupInitials,
    color = group.groupColor.code,
    currency = group.currency.code,
    fractionDigits = group.currency.fractionDigits,
    amountMinor = group.amountMinor.toDouble(),
)

private fun toJsContactHistoryItem(item: ContactHistoryItem): JsContactHistoryItem = when (item) {
    is ContactHistoryItem.Bill -> JsContactHistoryItem(
        id = item.id,
        type = "bill",
        groupName = item.groupName,
        currency = item.currency.code,
        fractionDigits = item.currency.fractionDigits,
        at = item.at.toDouble(),
        description = item.description,
        payerName = item.payerName,
        isPaidByMe = item.isPaidByMe,
        payerCount = item.payerCount,
        totalMinor = item.totalMinor.toDouble(),
        effectMinor = item.effectMinor.toDouble(),
        isFromMe = false,
        amountMinor = 0.0,
        method = "",
    )

    is ContactHistoryItem.Payment -> JsContactHistoryItem(
        id = item.id,
        type = "payment",
        groupName = item.groupName,
        currency = item.currency.code,
        fractionDigits = item.currency.fractionDigits,
        at = item.at.toDouble(),
        description = "",
        payerName = null,
        isPaidByMe = false,
        payerCount = 0,
        totalMinor = 0.0,
        effectMinor = 0.0,
        isFromMe = item.isFromMe,
        amountMinor = item.amountMinor.toDouble(),
        method = item.method.code,
    )
}

private fun toJsAddToGroup(draft: AddToGroupDraft): JsAddToGroup = JsAddToGroup(
    options = draft.options.map(::toJsAddGroupOption).toTypedArray(),
    selectedGroupId = draft.selectedGroupId,
    canAdd = draft.canAdd,
)

private fun toJsAddGroupOption(option: AddGroupOption): JsAddGroupOption = JsAddGroupOption(
    groupId = option.groupId,
    name = option.name,
    initials = option.initials,
    color = option.color.code,
    memberCount = option.memberCount,
    isAlreadyIn = option.isAlreadyIn,
)

private fun toJsGuestMergeSheet(sheet: GuestMergeSheet): JsGuestMergeSheet = JsGuestMergeSheet(
    displayLink = sheet.displayLink,
    isRequesting = sheet.isRequesting,
    hasFailed = sheet.hasFailed,
)
