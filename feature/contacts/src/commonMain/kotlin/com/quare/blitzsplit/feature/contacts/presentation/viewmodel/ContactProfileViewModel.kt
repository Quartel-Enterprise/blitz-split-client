package com.quare.blitzsplit.feature.contacts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.ContactBookSnapshot
import com.quare.blitzsplit.core.contacts.domain.usecase.AddContactToGroup
import com.quare.blitzsplit.core.contacts.domain.usecase.CancelGuestMerge
import com.quare.blitzsplit.core.contacts.domain.usecase.FindPendingGuestMerge
import com.quare.blitzsplit.core.contacts.domain.usecase.ObserveContactBook
import com.quare.blitzsplit.core.contacts.domain.usecase.RequestGuestMerge
import com.quare.blitzsplit.core.contacts.domain.usecase.SetContactHidden
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.feature.contacts.presentation.model.AddGroupOption
import com.quare.blitzsplit.feature.contacts.presentation.model.AddToGroupDraft
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiState
import com.quare.blitzsplit.feature.contacts.presentation.model.GuestMergeSheet
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

/*
 * Why: "Perfil do contato" (11.2), over Contatos on a phone and beside it in a wide window (the same route, drawn in
 * Contatos' pane). Everything but Juntar works offline on the device's data; Juntar and its waiting request need the
 * server.
 */
class ContactProfileViewModel(
    private val route: Route.ContactProfile,
    private val navigator: Navigator,
    private val setContactHidden: SetContactHidden,
    private val addContactToGroup: AddContactToGroup,
    private val requestGuestMerge: RequestGuestMerge,
    private val findPendingGuestMerge: FindPendingGuestMerge,
    private val cancelGuestMerge: CancelGuestMerge,
    observeContactBook: ObserveContactBook,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<ContactProfileUiState>
        field = MutableStateFlow(
            ContactProfileUiState(
                contact = null,
                history = emptyList(),
                hasMoreHistory = false,
                isOnline = true,
                isLoaded = false,
                isHideConfirmOpen = false,
                addToGroup = null,
                merge = null,
                pendingMerge = null,
                isCancellingMerge = false,
            ),
        )

    val uiAction: SharedFlow<ContactProfileUiAction>
        field = MutableSharedFlow<ContactProfileUiAction>()

    val stateWatcher: FlowWatcher<ContactProfileUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<ContactProfileUiAction> = FlowWatcher(uiAction)

    // Why: what Room said last, which "Adicionar a um grupo" lists the groups from.
    private var book: ContactBookSnapshot? = null

    // Why: null until the connectivity is known, so a profile opened offline never asks the server.
    private var isKnownOnline: Boolean? = null

    init {
        observeContactBook()
            .onEach { snapshot ->
                book = snapshot
                show(snapshot)
            }.launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .distinctUntilChanged()
            .onEach { isOnline ->
                isKnownOnline = isOnline
                uiState.update { it.copy(isOnline = isOnline) }
                if (isOnline) {
                    refreshPendingMerge()
                    requestMergeIfDue()
                }
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: ContactProfileUiEvent) {
        when (event) {
            ContactProfileUiEvent.OnBackClicked -> if (isOwnRouteOnTop()) navigator.navigateBack()

            ContactProfileUiEvent.OnChargeClicked -> findContactIf { uiState.value.canCharge }?.let { contact ->
                navigate(Route.GlobalBalances(BalancesFilter.OWES_YOU, chargePersonKey = contact.key))
            }

            ContactProfileUiEvent.OnReceivedClicked -> findContactIf { uiState.value.canReceive }?.let(::openSettleUp)

            ContactProfileUiEvent.OnSettleClicked -> findContactIf { uiState.value.canSettle }?.let(::openSettleUp)

            ContactProfileUiEvent.OnAddToGroupClicked -> openAddToGroup()

            is ContactProfileUiEvent.OnAddGroupPicked -> uiState.update { state ->
                val draft = state.addToGroup ?: return@update state
                val option = draft.options.firstOrNull { it.groupId == event.groupId }
                if (option == null ||
                    option.isAlreadyIn
                ) {
                    state
                } else {
                    state.copy(addToGroup = draft.copy(selectedGroupId = event.groupId))
                }
            }

            ContactProfileUiEvent.OnAddToGroupConfirmed -> addToGroup()

            ContactProfileUiEvent.OnAddToGroupDismissed -> uiState.update { it.copy(addToGroup = null) }

            ContactProfileUiEvent.OnHideClicked -> if (uiState.value.canHide) {
                uiState.update { it.copy(isHideConfirmOpen = true) }
            }

            ContactProfileUiEvent.OnHideConfirmed -> hide()

            ContactProfileUiEvent.OnHideDismissed -> uiState.update { it.copy(isHideConfirmOpen = false) }

            ContactProfileUiEvent.OnShowClicked -> findContactIf { uiState.value.canShow }?.let { contact ->
                viewModelScope.launch {
                    setContactHidden(contact.key, isHidden = false)
                        .onFailure { emit(ContactProfileUiAction.ShowChangeError) }
                }
            }

            ContactProfileUiEvent.OnViewBalancesClicked -> if (isShown()) {
                navigator.openHomeTab(Route.GlobalBalances(BalancesFilter.ALL, chargePersonKey = null))
            }

            ContactProfileUiEvent.OnSeeAllHistoryClicked -> emit(ContactProfileUiAction.ShowComingSoon)

            ContactProfileUiEvent.OnInviteGuestClicked -> findContactIf { it.isGuest }?.let { contact ->
                navigate(
                    Route.ContactInvite(
                        personKey = contact.key,
                        name = contact.name,
                    ),
                )
            }

            ContactProfileUiEvent.OnMergeClicked -> openMerge()

            ContactProfileUiEvent.OnMergeRetryClicked -> {
                uiState.update { state -> state.copy(merge = state.merge?.copy(hasFailed = false)) }
                requestMergeIfDue()
            }

            ContactProfileUiEvent.OnMergeCopyClicked -> uiState.value.merge?.merge?.let { merge ->
                emit(ContactProfileUiAction.CopyLink(merge.link))
            }

            ContactProfileUiEvent.OnMergeShareClicked -> {
                val merge = uiState.value.merge?.merge ?: return
                val contact = uiState.value.contact ?: return
                emit(ContactProfileUiAction.ShareLink(merge.link, contact.name))
            }

            ContactProfileUiEvent.OnMergeDismissed -> uiState.update { state ->
                // Why: a request made while the sheet was open waits for the guest from now on.
                state.copy(
                    merge = null,
                    pendingMerge = state.merge?.merge ?: state.pendingMerge,
                )
            }

            ContactProfileUiEvent.OnAskAppUserClicked -> emit(ContactProfileUiAction.ShowComingSoon)

            ContactProfileUiEvent.OnCancelMergeClicked -> cancelMerge()
        }
    }

    private fun show(snapshot: ContactBookSnapshot) {
        val contact = snapshot.findOrNull(route.personKey)
        val history = contact?.let(snapshot::historyOf).orEmpty()
        val wasLoaded = uiState.value.isLoaded
        uiState.update { state ->
            state.copy(
                contact = contact,
                history = history.take(HISTORY_SIZE),
                hasMoreHistory = history.size > HISTORY_SIZE,
                isLoaded = true,
                isHideConfirmOpen = state.isHideConfirmOpen && state.canHideWith(contact),
                addToGroup = state.addToGroup?.takeIf { contact != null }?.let { draft ->
                    draftOf(contact!!, snapshot, draft.selectedGroupId)
                },
                merge = state.merge?.takeIf { contact?.isGuest == true },
                pendingMerge = state.pendingMerge?.takeIf { contact?.isGuest == true },
            )
        }
        // Why: a person a sync took away closes their profile; beside Contatos, the pane goes back to its placeholder.
        if (contact == null && isOwnRouteOnTop()) navigator.navigateBack()
        if (!wasLoaded && isKnownOnline == true) refreshPendingMerge()
    }

    private fun ContactProfileUiState.canHideWith(contact: Contact?): Boolean =
        contact?.isSettled == true && !contact.isHidden

    private fun openSettleUp(contact: Contact) {
        navigate(
            Route.SettleUp(
                personKey = contact.key,
                currency = contact.net.currency,
            ),
        )
    }

    private fun openAddToGroup() {
        val contact = uiState.value.contact ?: return
        val snapshot = book ?: return
        if (!isShown()) return
        uiState.update { it.copy(addToGroup = draftOf(contact, snapshot, selectedGroupId = null)) }
    }

    // Why: the user's groups as the home lists them; the first one the person isn't in comes marked.
    private fun draftOf(
        contact: Contact,
        snapshot: ContactBookSnapshot,
        selectedGroupId: String?,
    ): AddToGroupDraft {
        val sharedIds = contact.groups.map { it.groupId }.toSet()
        val options = snapshot.groups.map { group ->
            AddGroupOption(
                groupId = group.id,
                name = group.name,
                initials = Initials.of(group.name),
                color = group.color,
                memberCount = group.members.size,
                isAlreadyIn = group.id in sharedIds,
            )
        }
        val selectable = options.filterNot { it.isAlreadyIn }
        return AddToGroupDraft(
            options = options,
            selectedGroupId = selectedGroupId?.takeIf { id -> selectable.any { it.groupId == id } }
                ?: selectable.firstOrNull()?.groupId,
        )
    }

    private fun addToGroup() {
        val state = uiState.value
        val draft = state.addToGroup?.takeIf { it.canAdd } ?: return
        val contact = state.contact ?: return
        val groupId = draft.selectedGroupId ?: return
        val groupName = draft.options.first { it.groupId == groupId }.name
        uiState.update { it.copy(addToGroup = null) }
        viewModelScope.launch {
            addContactToGroup(contact, groupId)
                .onSuccess { uiAction.emit(ContactProfileUiAction.ShowAddedToGroup(contact.name, groupName)) }
                .onFailure { uiAction.emit(ContactProfileUiAction.ShowChangeError) }
        }
    }

    // Why: hidden, the person leaves Contatos; a profile of its own goes back to the list, a pane keeps showing them.
    private fun hide() {
        val contact = findContactIf { uiState.value.canHide && uiState.value.isHideConfirmOpen } ?: return
        uiState.update { it.copy(isHideConfirmOpen = false) }
        viewModelScope.launch {
            setContactHidden(contact.key, isHidden = true)
                .onSuccess { if (isOwnRouteOnTop()) navigator.navigateBack() }
                .onFailure { uiAction.emit(ContactProfileUiAction.ShowChangeError) }
        }
    }

    private fun openMerge() {
        if (findContactIf { it.isGuest } == null || !isShown()) return
        uiState.update { state ->
            state.copy(
                merge = GuestMergeSheet(
                    merge = state.pendingMerge,
                    isRequesting = false,
                    hasFailed = false,
                ),
            )
        }
        requestMergeIfDue()
    }

    // Why: the server makes the request and its link once the sheet is open and online, never twice at once.
    private fun requestMergeIfDue() {
        val state = uiState.value
        val sheet = state.merge ?: return
        val contact = state.contact?.takeIf { it.isGuest } ?: return
        if (!state.isOnline || sheet.merge != null || sheet.isRequesting || sheet.hasFailed) return
        uiState.update { it.copy(merge = sheet.copy(isRequesting = true)) }
        viewModelScope.launch {
            val result = requestGuestMerge(contact)
            uiState.update { current ->
                val open =
                    current.merge
                        ?: return@update current.copy(pendingMerge = result.getOrNull() ?: current.pendingMerge)
                current.copy(
                    merge = open.copy(
                        merge = result.getOrNull(),
                        isRequesting = false,
                        hasFailed = result.isFailure,
                    ),
                )
            }
        }
    }

    private fun refreshPendingMerge() {
        val state = uiState.value
        val contact = state.contact?.takeIf { it.isGuest } ?: return
        if (!state.isOnline) return
        viewModelScope.launch {
            findPendingGuestMerge(contact).onSuccess { pending ->
                uiState.update { it.copy(pendingMerge = pending) }
            }
        }
    }

    private fun cancelMerge() {
        val state = uiState.value
        val pending = state.pendingMerge ?: return
        if (state.isCancellingMerge) return
        uiState.update { it.copy(isCancellingMerge = true) }
        viewModelScope.launch {
            val result = cancelGuestMerge(pending.id)
            uiState.update {
                it.copy(
                    isCancellingMerge = false,
                    pendingMerge = if (result.isSuccess) null else it.pendingMerge,
                )
            }
            result.onFailure { uiAction.emit(ContactProfileUiAction.ShowCancelMergeError) }
        }
    }

    private fun findContactIf(rule: (Contact) -> Boolean): Contact? = uiState.value.contact?.takeIf(rule)

    private fun navigate(route: Route) {
        if (isShown()) navigator.navigate(route)
    }

    private fun emit(action: ContactProfileUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }

    // Why: its own screen on top, or beside Contatos on top; never under a sheet it opened, so a late tap stacks nothing.
    private fun isShown(): Boolean = navigator.backStack.value
        .lastOrNull()
        .let { it == route || it == Route.Contacts }

    private fun isOwnRouteOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    private companion object {
        const val HISTORY_SIZE = 4
    }
}
