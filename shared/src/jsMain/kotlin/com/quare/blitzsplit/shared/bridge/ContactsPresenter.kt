@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactRow
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactsViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

/*
 * Why: one person of Contatos. netMinor is positive when they owe you, negative when you owe them; matchStart and
 * matchEnd mark where the search found the name, -1 outside the search.
 */
class JsContactRow(
    val key: String,
    val name: String,
    val initials: String,
    val color: String,
    val currency: String,
    val fractionDigits: Int,
    val netMinor: Double,
    val groupCount: Int,
    val firstGroupName: String,
    val isGuest: Boolean,
    val matchStart: Int,
    val matchEnd: Int,
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
    // Why: "Você deve" and "Te devem", Saldos' totals in its main currency.
    val total: JsBalanceTotal,
    val query: String,
    val isSearching: Boolean,
    // Why: "Nos seus grupos", hidden ones included; the web has no agenda to look in.
    val groupResults: Array<JsContactRow>,
    val showsNoResults: Boolean,
    val isEmpty: Boolean,
    val isLoading: Boolean,
    val showsSummary: Boolean,
    val showsOfflineNotice: Boolean,
    // Why: the person the desktop's pane beside the list shows, null for its placeholder.
    val selectedKey: String?,
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
                total = JsBalanceTotal(
                    currency = state.total.currency.code,
                    fractionDigits = state.total.currency.fractionDigits,
                    netMinor = state.total.netMinor.toDouble(),
                    toReceiveMinor = state.total.toReceiveMinor.toDouble(),
                    toPayMinor = state.total.toPayMinor.toDouble(),
                ),
                query = state.query,
                isSearching = state.isSearching,
                groupResults = state.groupResults.map(::toJsContactRow).toTypedArray(),
                showsNoResults = state.showsNoResults,
                isEmpty = state.isEmpty,
                isLoading = state.isLoading,
                showsSummary = state.showsSummary,
                showsOfflineNotice = state.showsOfflineNotice,
                selectedKey = state.selectedKey,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "coming-soon" or "change-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                ContactsUiAction.ShowComingSoon -> "coming-soon"
                ContactsUiAction.ShowChangeError -> "change-error"
            }
        }
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

    // Why: the field is always there on the web; typing opens the search, clearing it closes it.
    fun search(query: String) {
        if (query.isEmpty()) {
            viewModel.onEvent(ContactsUiEvent.OnSearchClosed)
            return
        }
        viewModel.onEvent(ContactsUiEvent.OnSearchOpened)
        viewModel.onEvent(ContactsUiEvent.OnQueryChanged(query))
    }

    fun closeSearch() {
        viewModel.onEvent(ContactsUiEvent.OnSearchClosed)
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
    currency = row.net.currency.code,
    fractionDigits = row.net.currency.fractionDigits,
    netMinor = row.net.amountMinor.toDouble(),
    groupCount = row.groupCount,
    firstGroupName = row.firstGroupName,
    isGuest = row.isGuest,
    matchStart = row.match?.start ?: -1,
    matchEnd = row.match?.end ?: -1,
)
