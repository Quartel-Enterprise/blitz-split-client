@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactRow
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactsViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform
import software.quare.loadable.valueOrNull

// Why: one person of Contatos. netMinor is positive when they owe you, negative when you owe them.
class JsContactRow(
    val key: String,
    val name: String,
    val initials: String,
    val color: String,
    val photoUrl: String?,
    val currency: String,
    val fractionDigits: Int,
    val netMinor: Double,
    val groupCount: Int,
    val firstGroupName: String,
    val isGuest: Boolean,
)

class JsContactsState(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: String,
    val photoUrl: String?,
    // Why: "google" or "apple", null while there is no account.
    val accountProvider: String?,
    val contacts: Array<JsContactRow>,
    val hiddenContacts: Array<JsContactRow>,
    val isHiddenExpanded: Boolean,
    // Why: "Você deve" and "Te devem", Saldos' totals in its main currency; null while they load (the shimmer).
    val total: JsBalanceTotal?,
    // Why: "Buscar…" and ⌘K only once there is a group to look in.
    val canSearch: Boolean,
    val isEmpty: Boolean,
    // Why: the skeleton rows, until the people are read and the account's copy is whole.
    val isLoading: Boolean,
    // Why: "Sem internet. Seus dados aparecem quando reconectar." under the skeleton.
    val isWaitingForInternet: Boolean,
    val showsSummary: Boolean,
    val showsOfflineNotice: Boolean,
    // Why: the person the desktop's pane beside the list shows, null for its placeholder.
    val selectedKey: String?,
    // Why: the Atividade tab's badge.
    val activityUnreadCount: Int,
)

class ContactsPresenter {
    private val holder = ViewModelHolder(ContactsViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsContactsState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsContactsState(
                displayName = state.displayName,
                avatarInitial = state.avatarInitial,
                avatarColor = state.avatarColor.code,
                photoUrl = state.photoUrl,
                accountProvider = state.accountProvider?.code,
                contacts = state.contacts.map(::toJsContactRow).toTypedArray(),
                hiddenContacts = state.hiddenContacts.map(::toJsContactRow).toTypedArray(),
                isHiddenExpanded = state.isHiddenExpanded,
                total = state.total.valueOrNull()?.let { total ->
                    JsBalanceTotal(
                        currency = total.currency.code,
                        fractionDigits = total.currency.fractionDigits,
                        netMinor = total.netMinor.toDouble(),
                        toReceiveMinor = total.toReceiveMinor.toDouble(),
                        toPayMinor = total.toPayMinor.toDouble(),
                    )
                },
                canSearch = state.canSearch,
                isEmpty = state.isEmpty,
                isLoading = state.isLoading,
                isWaitingForInternet = state.isWaitingForInternet,
                showsSummary = state.showsSummary,
                showsOfflineNotice = state.showsOfflineNotice,
                selectedKey = state.selectedKey,
                activityUnreadCount = state.activityUnreadCount,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "change-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { "change-error" }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun openContact(key: String) {
        viewModel.onEvent(ContactsUiEvent.OnContactClicked(key))
    }

    // Why: the desktop draws the profile beside the list, so a click picks instead of opening it over the list.
    fun setDetailPaneShown(isShown: Boolean) {
        viewModel.onEvent(ContactsUiEvent.OnDetailPaneChanged(isShown))
    }

    // Why: "Buscar…" in the sidebar, the phone's magnifier, or ⌘K / Ctrl+K: the home's search over Contatos.
    fun openSearch() {
        viewModel.onEvent(ContactsUiEvent.OnSearchClicked)
    }

    fun toggleHidden() {
        viewModel.onEvent(ContactsUiEvent.OnHiddenToggled)
    }

    fun showContact(key: String) {
        viewModel.onEvent(ContactsUiEvent.OnShowContactClicked(key))
    }

    fun openBalancesCard() {
        viewModel.onEvent(ContactsUiEvent.OnBalancesCardClicked)
    }

    fun openYouOwe() {
        viewModel.onEvent(ContactsUiEvent.OnYouOweClicked)
    }

    fun openOwedToYou() {
        viewModel.onEvent(ContactsUiEvent.OnOwedToYouClicked)
    }

    fun inviteWithLink() {
        viewModel.onEvent(ContactsUiEvent.OnInviteWithLinkClicked)
    }

    fun openHome() {
        viewModel.onEvent(ContactsUiEvent.OnHomeTabClicked)
    }

    // Why: the logo, which opens Home.
    fun openBrand() {
        viewModel.onEvent(ContactsUiEvent.OnBrandClicked)
    }

    fun openGroups() {
        viewModel.onEvent(ContactsUiEvent.OnGroupsTabClicked)
    }

    fun openBalances() {
        viewModel.onEvent(ContactsUiEvent.OnBalancesTabClicked)
    }

    fun openActivity() {
        viewModel.onEvent(ContactsUiEvent.OnActivityTabClicked)
    }

    fun openProfile() {
        viewModel.onEvent(ContactsUiEvent.OnProfileClicked)
    }

    fun closePane() {
        viewModel.onEvent(ContactsUiEvent.OnPaneBackClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsContactRow(row: ContactRow): JsContactRow = JsContactRow(
    key = row.key,
    name = row.name,
    initials = row.initials,
    color = row.color.code,
    photoUrl = row.photoUrl,
    currency = row.net.currency.code,
    fractionDigits = row.net.currency.fractionDigits,
    netMinor = row.net.amountMinor.toDouble(),
    groupCount = row.groupCount,
    firstGroupName = row.firstGroupName,
    isGuest = row.isGuest,
)
