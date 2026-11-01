package com.quare.blitzsplit.feature.contacts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.contacts.domain.model.ContactBookSnapshot
import com.quare.blitzsplit.core.contacts.domain.usecase.ObserveContactBook
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.groups.domain.usecase.EncodeQrCode
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.CopiedFeedbackTimer
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactInviteUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactInviteUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactInviteUiState
import com.quare.blitzsplit.feature.contacts.presentation.model.InviteGroupOption
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

// Why: "Convidar com link ou QR" (11.4). Works offline once the group has its link: the link already exists.
class ContactInviteViewModel(
    private val route: Route.ContactInvite,
    private val navigator: Navigator,
    private val encodeQrCode: EncodeQrCode,
    observeContactBook: ObserveContactBook,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<ContactInviteUiState>
        field = MutableStateFlow(
            ContactInviteUiState(
                name = route.name,
                isPersonal = route.personKey != null,
                groups = emptyList(),
                selectedGroupId = null,
                link = null,
                qrCode = null,
                isOnline = true,
                isLinkCopied = false,
                isLoaded = false,
            ),
        )

    val uiAction: SharedFlow<ContactInviteUiAction>
        field = MutableSharedFlow<ContactInviteUiAction>()

    val stateWatcher: FlowWatcher<ContactInviteUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<ContactInviteUiAction> = FlowWatcher(uiAction)

    private var book: ContactBookSnapshot? = null
    private val copiedFeedback = CopiedFeedbackTimer(viewModelScope) { isCopied ->
        uiState.update { it.copy(isLinkCopied = isCopied) }
    }

    init {
        observeContactBook()
            .onEach { snapshot ->
                book = snapshot
                show(snapshot)
            }.launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ContactInviteUiEvent) {
        when (event) {
            is ContactInviteUiEvent.OnGroupPicked -> if (uiState.value.groups.any { it.groupId == event.groupId }) {
                uiState.update {
                    it.copy(
                        selectedGroupId = event.groupId,
                        isLinkCopied = false,
                    )
                }
                book?.let(::show)
            }

            ContactInviteUiEvent.OnNewGroupClicked -> if (isOnTop()) {
                navigator.navigateBack()
                navigator.navigate(Route.CreateGroup)
            }

            ContactInviteUiEvent.OnCopyClicked -> copy()

            ContactInviteUiEvent.OnShareClicked -> share()

            ContactInviteUiEvent.OnCloseClicked -> leave()
        }
    }

    private fun show(snapshot: ContactBookSnapshot) {
        val personKey = route.personKey
        val links: List<Pair<InviteGroupOption, String?>> = if (personKey == null) {
            snapshot.groups.map { group ->
                InviteGroupOption(group.id, group.name, Initials.of(group.name), group.color) to
                    group.inviteCode?.let(InviteLink::of)
            }
        } else {
            // Why: a guest a sync took away has nothing left to be invited to.
            val contact = snapshot.findOrNull(personKey)?.takeIf { it.isGuest } ?: return leave()
            contact.groups.map { group ->
                val link = if (group.groupInviteCode != null && group.memberInviteCode != null) {
                    InviteLink.personalOf(group.groupInviteCode!!, group.memberInviteCode!!, group.meInviteCode)
                } else {
                    null
                }
                InviteGroupOption(group.groupId, group.groupName, group.groupInitials, group.groupColor) to link
            }
        }
        val current = uiState.value
        // Why: the most recently active group comes marked, until another is picked or the marked one is gone.
        val selected = links.firstOrNull { it.first.groupId == current.selectedGroupId } ?: links.firstOrNull()
        val link = selected?.second
        val qrCode = if (link == current.link) current.qrCode else link?.let { encodeQrCode(it) }
        uiState.update { state ->
            state.copy(
                groups = links.map { it.first },
                selectedGroupId = selected?.first?.groupId,
                link = link,
                qrCode = qrCode,
                isLinkCopied = state.isLinkCopied && link == state.link,
                isLoaded = true,
            )
        }
    }

    private fun copy() {
        val link = uiState.value.link ?: return
        send(ContactInviteUiAction.CopyLink(link))
        copiedFeedback.start()
    }

    private fun share() {
        val state = uiState.value
        val link = state.link ?: return
        val group = state.selectedGroup ?: return
        send(
            ContactInviteUiAction.ShareLink(
                link = link,
                groupName = group.name,
                name = state.name,
            ),
        )
    }

    private fun send(action: ContactInviteUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: only while it is still on top: a native back (a swipe, a sheet dismissed) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }
}
