package com.quare.blitzsplit.feature.contacts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.contacts.domain.usecase.ObserveContactBook
import com.quare.blitzsplit.core.contacts.domain.usecase.SetContactHidden
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.navigation.popToGroups
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.feature.contacts.presentation.mapper.rowOf
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

/*
 * Why: Contatos (flow 11), the home's tab over the groups. Everything comes from the device, offline too: the people,
 * their nets (the same as Saldos'), the hidden list and the search, which also looks in the agenda the platform read.
 */
class ContactsViewModel(
    private val navigator: Navigator,
    private val setContactHidden: SetContactHidden,
    observeContactBook: ObserveContactBook,
    observePeopleBalances: ObservePeopleBalances,
    observeProfile: ObserveProfile,
    observeAccount: ObserveAccount,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<ContactsUiState>
        field = MutableStateFlow(
            ContactsUiState(
                displayName = "",
                avatarInitial = "",
                avatarColor = AvatarColor.default,
                photoUrl = null,
                accountProvider = null,
                contacts = emptyList(),
                hiddenContacts = emptyList(),
                isHiddenExpanded = false,
                total = zeroTotalOf(GroupCurrency.fallback),
                isSearchOpen = false,
                query = "",
                agenda = emptyList(),
                isOnline = true,
                isLoaded = false,
                isDetailPaneShown = false,
                pickedKey = null,
                isAgendaPicked = false,
            ),
        )

    val uiAction: SharedFlow<ContactsUiAction>
        field = MutableSharedFlow<ContactsUiAction>()

    val stateWatcher: FlowWatcher<ContactsUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<ContactsUiAction> = FlowWatcher(uiAction)

    init {
        observeProfile()
            .onEach { profile ->
                val name = profile?.displayName?.trim().orEmpty()
                uiState.update { state ->
                    state.copy(
                        displayName = name,
                        avatarInitial = Initials.firstOf(name),
                        avatarColor = profile?.avatarColor ?: AvatarColor.default,
                        photoUrl = profile?.photo?.toImageUrl(),
                    )
                }
            }.launchIn(viewModelScope)
        observeAccount()
            .onEach { account ->
                val provider = (account.status as? AccountStatus.Connected)?.provider
                uiState.update { state -> state.copy(accountProvider = provider) }
            }.launchIn(viewModelScope)
        observeContactBook()
            .onEach { book ->
                val (hidden, shown) = book.contacts.partition { it.isHidden }
                uiState.update {
                    it.copy(
                        contacts = shown.map(::rowOf),
                        hiddenContacts = hidden.map(::rowOf),
                        // Why: the section closes once nobody is left in it, so a new hidden one starts closed.
                        isHiddenExpanded = it.isHiddenExpanded && hidden.isNotEmpty(),
                        isLoaded = true,
                    )
                }
            }.launchIn(viewModelScope)
        observePeopleBalances()
            .onEach { people ->
                uiState.update { it.copy(total = people.totals.firstOrNull() ?: zeroTotalOf(GroupCurrency.fallback)) }
            }.launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ContactsUiEvent) {
        when (event) {
            is ContactsUiEvent.OnContactClicked -> openContact(event.key)

            is ContactsUiEvent.OnDetailPaneChanged -> uiState.update { it.copy(isDetailPaneShown = event.isShown) }

            ContactsUiEvent.OnSearchOpened -> uiState.update { it.copy(isSearchOpen = true) }

            is ContactsUiEvent.OnQueryChanged -> uiState.update { it.copy(query = event.query) }

            ContactsUiEvent.OnSearchClosed -> uiState.update {
                it.copy(
                    isSearchOpen = false,
                    query = "",
                )
            }

            ContactsUiEvent.OnHiddenToggled -> uiState.update {
                it.copy(isHiddenExpanded = !it.isHiddenExpanded && it.hiddenContacts.isNotEmpty())
            }

            is ContactsUiEvent.OnShowContactClicked -> show(event.key)

            ContactsUiEvent.OnBalancesCardClicked -> openBalances(BalancesFilter.ALL)

            ContactsUiEvent.OnYouOweClicked -> if (uiState.value.total.toPayMinor > 0) {
                openBalances(BalancesFilter.YOU_OWE)
            }

            ContactsUiEvent.OnOwedToYouClicked -> if (uiState.value.total.toReceiveMinor > 0) {
                openBalances(BalancesFilter.OWES_YOU)
            }

            ContactsUiEvent.OnAgendaClicked -> openAgenda()

            ContactsUiEvent.OnInviteWithLinkClicked -> openFromList(
                Route.ContactInvite(
                    personKey = null,
                    name = null,
                ),
            )

            is ContactsUiEvent.OnAgendaContactInviteClicked -> {
                val contact = uiState.value.agenda.firstOrNull { it.id == event.contactId } ?: return
                openFromList(
                    Route.ContactInvite(
                        personKey = null,
                        name = contact.name,
                    ),
                )
            }

            is ContactsUiEvent.OnAgendaLoaded -> uiState.update { it.copy(agenda = event.contacts) }

            ContactsUiEvent.OnGroupsTabClicked -> if (isTabShown()) navigator.popToGroups()

            ContactsUiEvent.OnBalancesTabClicked -> if (isTabShown()) {
                navigator.openHomeTab(Route.GlobalBalances(BalancesFilter.ALL, charge = null))
            }

            ContactsUiEvent.OnActivityTabClicked -> if (isTabShown()) emit(ContactsUiAction.ShowComingSoon)

            ContactsUiEvent.OnProfileClicked -> if (isTabShown()) {
                closeOverTab()
                navigator.navigate(Route.Profile)
            }

            ContactsUiEvent.OnPaneBackClicked -> uiState.update {
                it.copy(
                    pickedKey = null,
                    isAgendaPicked = false,
                )
            }
        }
    }

    // Why: beside the list where the UI draws the pane, the person takes the place of the one shown; else over it.
    private fun openContact(key: String) {
        val state = uiState.value
        if (state.contacts.none { it.key == key } && state.hiddenContacts.none { it.key == key }) return
        if (state.isDetailPaneShown) {
            uiState.update {
                it.copy(
                    pickedKey = key,
                    isAgendaPicked = false,
                )
            }
        } else {
            openFromList(Route.ContactProfile(key))
        }
    }

    private fun openAgenda() {
        if (uiState.value.isDetailPaneShown) {
            uiState.update {
                it.copy(
                    pickedKey = null,
                    isAgendaPicked = true,
                )
            }
        } else {
            openFromList(Route.ContactsAgenda)
        }
    }

    // Why: only Contatos on top takes taps, so a late tap never stacks a second screen.
    private fun openFromList(route: Route) {
        if (isOnTop()) navigator.navigate(route)
    }

    private fun openBalances(filter: BalancesFilter) {
        if (isOnTop()) navigator.openHomeTab(Route.GlobalBalances(filter, charge = null))
    }

    private fun show(key: String) {
        if (uiState.value.hiddenContacts.none { it.key == key }) return
        viewModelScope.launch {
            setContactHidden(key, isHidden = false).onFailure { uiAction.emit(ContactsUiAction.ShowChangeError) }
        }
    }

    private fun emit(action: ContactsUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == Route.Contacts

    /*
     * Why: the tab bar (or rail, or sidebar) stays in sight over a profile the web and iOS draw in the tab, so its taps
     * count while Contatos is the tab, closing what it opened first, as a tab bar does.
     */
    private fun isTabShown(): Boolean {
        val stack = navigator.backStack.value
        return stack.getOrNull(stack.indexOf(Route.Groups) + 1) == Route.Contacts && Route.Groups in stack
    }

    private fun closeOverTab() {
        navigator.popTo(navigator.backStack.value.indexOf(Route.Contacts) + 1)
    }

    private fun zeroTotalOf(currency: GroupCurrency): PeopleTotal = PeopleTotal(
        currency = currency,
        netMinor = 0L,
        toReceiveMinor = 0L,
        toPayMinor = 0L,
        toReceiveCount = 0,
        toPayCount = 0,
    )
}
