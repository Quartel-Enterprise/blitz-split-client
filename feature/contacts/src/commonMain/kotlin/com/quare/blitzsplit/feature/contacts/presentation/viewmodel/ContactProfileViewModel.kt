package com.quare.blitzsplit.feature.contacts.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.ContactBookSnapshot
import com.quare.blitzsplit.core.contacts.domain.usecase.AddContactToGroup
import com.quare.blitzsplit.core.contacts.domain.usecase.CancelGuestMerge
import com.quare.blitzsplit.core.contacts.domain.usecase.ClaimGuestAsMe
import com.quare.blitzsplit.core.contacts.domain.usecase.FindPendingGuestMerge
import com.quare.blitzsplit.core.contacts.domain.usecase.ObserveContactBook
import com.quare.blitzsplit.core.contacts.domain.usecase.RequestGuestMerge
import com.quare.blitzsplit.core.contacts.domain.usecase.SetContactHidden
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.findHomeTabUnder
import com.quare.blitzsplit.core.navigation.isRightAboveHomeTab
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
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
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded
import software.quare.loadable.valueOrNull

/*
 * Why: "Perfil do contato" (11.2), over Contatos on a phone and beside it in a wide window (the same route, drawn in
 * Contatos' pane), and over Saldos for a face tapped there. Everything but Juntar works offline on the device's data; Juntar and its waiting request need the
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
    private val claimGuestAsMe: ClaimGuestAsMe,
    private val syncNow: SyncNow,
    observeContactBook: ObserveContactBook,
    connectivityObserver: NetworkConnectivityObserver,
    trackEvent: TrackEvent,
) : TrackedViewModel<ContactProfileUiEvent>(trackEvent) {
    // Why: the screen these events come from, as their source param says.
    private val sourceParams: Map<String, Any> = mapOf("source" to "contact_profile")

    // Why: "Juntar com uma conta", as its link's and retry's source say, apart from the profile's own taps.
    private val mergeParams: Map<String, Any> = mapOf("source" to "contact_merge")

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
                pendingMerge = Loadable.Loading,
                hasMergeStatusFailed = false,
                isCancellingMerge = false,
                homeTab = navigator.backStack.value.findHomeTabUnder(route),
                isOverHomeTab = navigator.backStack.value.isRightAboveHomeTab(route),
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

    override fun handleEvent(event: ContactProfileUiEvent) {
        when (event) {
            ContactProfileUiEvent.OnBackClicked -> if (isOwnRouteOnTop()) navigator.navigateBack()

            // Why: Cobrar over the profile, in the currency it shows; closing it comes back here.
            ContactProfileUiEvent.OnChargeClicked -> findContactIf { uiState.value.canCharge }?.let { contact ->
                val charge = Route.Charge(
                    personKey = contact.key,
                    currency = contact.net.currency,
                )
                if (navigate(charge)) trackEvent("charge_clicked", sourceParams)
            }

            ContactProfileUiEvent.OnReceivedClicked -> findContactIf { uiState.value.canReceive }?.let { contact ->
                if (openSettleUp(contact)) trackEvent("received_clicked", sourceParams)
            }

            ContactProfileUiEvent.OnSettleClicked -> findContactIf { uiState.value.canSettle }?.let { contact ->
                if (openSettleUp(contact)) trackEvent("settle_clicked", sourceParams)
            }

            // Why: only a group the person is still in, so a stale tap never opens one a sync just took away.
            is ContactProfileUiEvent.OnGroupClicked -> findContactIf { contact ->
                contact.groups.any { it.groupId == event.groupId }
            }?.let { if (openGroup(event.groupId)) trackEvent("group_clicked", sourceParams) }

            ContactProfileUiEvent.OnAddToGroupClicked -> openAddToGroup()

            is ContactProfileUiEvent.OnAddGroupPicked -> {
                val draft = uiState.value.addToGroup ?: return
                val option = draft.options.firstOrNull { it.groupId == event.groupId }
                if (option == null || option.isAlreadyIn) return
                if (event.groupId != draft.selectedGroupId) trackEvent("contact_add_to_group_picked", emptyMap())
                uiState.update { it.copy(addToGroup = it.addToGroup?.copy(selectedGroupId = event.groupId)) }
            }

            ContactProfileUiEvent.OnAddToGroupConfirmed -> addToGroup()

            ContactProfileUiEvent.OnAddToGroupDismissed -> uiState.update { it.copy(addToGroup = null) }

            ContactProfileUiEvent.OnHideClicked -> if (uiState.value.canHide && !uiState.value.isHideConfirmOpen) {
                trackEvent("contact_hide_clicked", emptyMap())
                uiState.update { it.copy(isHideConfirmOpen = true) }
            }

            ContactProfileUiEvent.OnHideConfirmed -> hide()

            ContactProfileUiEvent.OnHideDismissed -> if (uiState.value.isHideConfirmOpen) {
                trackEvent("contact_hide_cancelled", emptyMap())
                uiState.update { it.copy(isHideConfirmOpen = false) }
            }

            ContactProfileUiEvent.OnShowClicked -> findContactIf { uiState.value.canShow }?.let { contact ->
                trackEvent("contact_show_clicked", sourceParams)
                viewModelScope.launch {
                    setContactHidden(contact.key, isHidden = false)
                        .onFailure { emit(ContactProfileUiAction.ShowChangeError) }
                }
            }

            ContactProfileUiEvent.OnViewBalancesClicked -> if (isShown()) {
                trackEvent("contact_view_balances_clicked", emptyMap())
                navigator.openHomeTab(Route.GlobalBalances(BalancesFilter.ALL))
            }

            /*
             * Why: "Ver tudo" is Atividade on this person, over the profile, whose back returns to it; the same one
             * already under it (the profile opened from its item) is gone back to, so the stack never keeps growing.
             */
            ContactProfileUiEvent.OnSeeAllHistoryClicked -> if (isShown()) {
                trackEvent("contact_history_see_all_clicked", emptyMap())
                val history = Route.Activity(personKey = route.personKey)
                val index = navigator.backStack.value.lastIndexOf(history)
                if (index >= 0) navigator.popTo(index + 1) else navigator.navigate(history)
            }

            ContactProfileUiEvent.OnInviteGuestClicked -> findContactIf { it.isGuest }?.let { contact ->
                val invite = Route.ContactInvite(
                    personKey = contact.key,
                    name = contact.name,
                )
                if (navigate(invite)) trackEvent("contact_invite_guest_clicked", emptyMap())
            }

            ContactProfileUiEvent.OnMergeClicked -> openMerge()

            ContactProfileUiEvent.OnMergeRetryClicked -> {
                uiState.update { state -> state.copy(merge = state.merge?.copy(hasFailed = false)) }
                if (requestMergeIfDue()) trackEvent("retry_clicked", mergeParams)
            }

            ContactProfileUiEvent.OnMergeCopyClicked -> uiState.value.merge?.merge?.let { merge ->
                trackEvent("invite_link_copy_clicked", mergeParams)
                emit(ContactProfileUiAction.CopyLink(merge.link))
            }

            ContactProfileUiEvent.OnMergeShareClicked -> {
                val merge = uiState.value.merge?.merge ?: return
                val contact = uiState.value.contact ?: return
                trackEvent("invite_link_share_clicked", mergeParams)
                emit(ContactProfileUiAction.ShareLink(merge.link, contact.name))
            }

            ContactProfileUiEvent.OnMergeDismissed -> uiState.update { state ->
                // Why: a request made while the sheet was open waits for the guest from now on.
                state.copy(
                    merge = null,
                    pendingMerge = state.merge?.merge?.toLoaded() ?: state.pendingMerge,
                )
            }

            ContactProfileUiEvent.OnClaimAsMeClicked -> if (updateSheet { it.copy(isConfirmingClaim = true) }) {
                trackEvent("contact_claim_clicked", emptyMap())
            }

            ContactProfileUiEvent.OnClaimAsMeDismissed -> {
                val sheet = uiState.value.merge
                if (sheet != null && sheet.isConfirmingClaim && !sheet.isClaiming) {
                    updateSheet { it.copy(isConfirmingClaim = false) }
                    trackEvent("contact_claim_cancelled", emptyMap())
                }
            }

            ContactProfileUiEvent.OnClaimAsMeConfirmed -> claimAsMe()

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
                // Why: someone with an account has no merge to wait for; a guest keeps what the server said.
                pendingMerge = if (contact?.isGuest == true) state.pendingMerge else null.toLoaded(),
            )
        }
        // Why: a person a sync took away closes their profile; beside Contatos, the pane goes back to its placeholder.
        if (contact == null && isOwnRouteOnTop()) navigator.navigateBack()
        if (!wasLoaded && isKnownOnline == true) refreshPendingMerge()
    }

    private fun ContactProfileUiState.canHideWith(contact: Contact?): Boolean =
        contact?.isSettled == true && !contact.isHidden

    /*
     * Why: a profile opened from that same settling up's face goes back to it, keeping what was typed, rather than
     * stacking a second, equal form whose twin under it would close it once the debt settles. Says whether it opened
     * (or went back to the one under it), which Analytics counts.
     */
    private fun openSettleUp(contact: Contact): Boolean {
        val settleUp = Route.SettleUp(
            personKey = contact.key,
            currency = contact.net.currency,
        )
        val stack = navigator.backStack.value
        if (!isOwnRouteOnTop() || stack.getOrNull(stack.lastIndex - 1) != settleUp) return navigate(settleUp)
        navigator.navigateBack()
        return true
    }

    private fun openGroup(groupId: String): Boolean = navigate(
        Route.GroupDetail(
            groupId,
            joinedAs = null,
            tab = GroupTab.EXPENSES,
        ),
    )

    private fun openAddToGroup() {
        val contact = uiState.value.contact ?: return
        val snapshot = book ?: return
        if (!isShown()) return
        trackEvent("contact_add_to_group_clicked", emptyMap())
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
        trackEvent("contact_add_to_group_confirmed", mapOf("is_guest" to contact.isGuest))
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
        trackEvent("contact_hide_confirmed", emptyMap())
        uiState.update { it.copy(isHideConfirmOpen = false) }
        viewModelScope.launch {
            setContactHidden(contact.key, isHidden = true)
                .onSuccess { if (isOwnRouteOnTop()) navigator.navigateBack() }
                .onFailure { uiAction.emit(ContactProfileUiAction.ShowChangeError) }
        }
    }

    private fun openMerge() {
        if (findContactIf { it.isGuest } == null || !isShown()) return
        trackEvent("contact_merge_clicked", emptyMap())
        uiState.update { state ->
            state.copy(
                merge = GuestMergeSheet(
                    merge = state.pendingMerge.valueOrNull(),
                    isRequesting = false,
                    hasFailed = false,
                    isConfirmingClaim = false,
                    isClaiming = false,
                ),
            )
        }
        requestMergeIfDue()
    }

    // Why: the server makes the request and its link once the sheet is open and online, never twice at once.
    // Why: says whether it asked the server, which a retry's Analytics counts.
    private fun requestMergeIfDue(): Boolean {
        val state = uiState.value
        val sheet = state.merge ?: return false
        val contact = state.contact?.takeIf { it.isGuest } ?: return false
        if (!state.isOnline || sheet.merge != null || sheet.isRequesting || sheet.hasFailed) return false
        uiState.update { it.copy(merge = sheet.copy(isRequesting = true)) }
        viewModelScope.launch {
            val result = requestGuestMerge(contact)
            uiState.update { current ->
                val open =
                    current.merge
                        ?: return@update current.copy(
                            pendingMerge = result.getOrNull()?.toLoaded() ?: current.pendingMerge,
                        )
                current.copy(
                    merge = open.copy(
                        merge = result.getOrNull(),
                        isRequesting = false,
                        hasFailed = result.isFailure,
                    ),
                )
            }
        }
        return true
    }

    // Why: says whether the sheet was open, which Analytics counts.
    private fun updateSheet(change: (GuestMergeSheet) -> GuestMergeSheet): Boolean {
        if (uiState.value.merge == null) return false
        uiState.update { state -> state.copy(merge = state.merge?.let(change)) }
        return true
    }

    /*
     * Why: the server moves everything in the guest's name to the user's own member at once, and a sync brings it to the
     * device; the guest then leaves the book, which closes the profile. Online only, as only the server knows every
     * group's members.
     */
    private fun claimAsMe() {
        val state = uiState.value
        val sheet = state.merge ?: return
        val contact = state.contact?.takeIf { it.isGuest } ?: return
        if (!state.isOnline || !sheet.isConfirmingClaim || sheet.isClaiming) return
        updateSheet { it.copy(isClaiming = true) }
        trackEvent("contact_claim_confirmed", emptyMap())
        viewModelScope.launch {
            val result = claimGuestAsMe(contact)
            if (result.isSuccess) syncNow()
            updateSheet {
                it.copy(
                    isClaiming = false,
                    isConfirmingClaim = result.isFailure && it.isConfirmingClaim,
                )
            }
            result.onFailure { uiAction.emit(ContactProfileUiAction.ShowClaimError) }
        }
    }

    private fun refreshPendingMerge() {
        val state = uiState.value
        val contact = state.contact?.takeIf { it.isGuest } ?: return
        if (!state.isOnline) return
        uiState.update { it.copy(hasMergeStatusFailed = false) }
        viewModelScope.launch {
            findPendingGuestMerge(contact)
                .onSuccess { pending -> uiState.update { it.copy(pendingMerge = pending.toLoaded()) } }
                // Why: a failure keeps the status unknown, and the next time the connection comes back asks again.
                .onFailure { uiState.update { it.copy(hasMergeStatusFailed = true) } }
        }
    }

    private fun cancelMerge() {
        val state = uiState.value
        val pending = state.pendingMerge.valueOrNull() ?: return
        if (state.isCancellingMerge) return
        trackEvent("contact_merge_cancel_clicked", emptyMap())
        uiState.update { it.copy(isCancellingMerge = true) }
        viewModelScope.launch {
            val result = cancelGuestMerge(pending.id)
            uiState.update {
                it.copy(
                    isCancellingMerge = false,
                    pendingMerge = if (result.isSuccess) null.toLoaded() else it.pendingMerge,
                )
            }
            result.onFailure { uiAction.emit(ContactProfileUiAction.ShowCancelMergeError) }
        }
    }

    private fun findContactIf(rule: (Contact) -> Boolean): Contact? = uiState.value.contact?.takeIf(rule)

    // Why: says whether it opened, which Analytics counts.
    private fun navigate(route: Route): Boolean {
        if (!isShown()) return false
        navigator.navigate(route)
        return true
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
