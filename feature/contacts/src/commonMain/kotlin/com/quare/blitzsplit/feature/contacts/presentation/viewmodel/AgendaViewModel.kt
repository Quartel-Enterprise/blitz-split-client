package com.quare.blitzsplit.feature.contacts.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaAccess
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded
import software.quare.loadable.valueOrNull

/*
 * Why: "Convidar da agenda" (11.3). The platform reads the agenda and asks for it; the shared code decides what each
 * access shows and where an invite goes. No number leaves the device: whoever is invited gets a group's link, and
 * nobody is told who already uses the app (that needs a confirmed phone, a later flow).
 */
class AgendaViewModel(
    private val navigator: Navigator,
    trackEvent: TrackEvent,
) : TrackedViewModel<AgendaUiEvent>(trackEvent) {
    // Why: the screen these events come from, as their source param says.
    private val sourceParams: Map<String, Any> = mapOf("source" to "agenda")

    val uiState: StateFlow<AgendaUiState>
        field = MutableStateFlow(
            AgendaUiState(
                access = AgendaAccess.UNKNOWN,
                contacts = Loadable.Loading,
                query = "",
            ),
        )

    val uiAction: SharedFlow<AgendaUiAction>
        field = MutableSharedFlow<AgendaUiAction>()

    val stateWatcher: FlowWatcher<AgendaUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<AgendaUiAction> = FlowWatcher(uiAction)

    override fun handleEvent(event: AgendaUiEvent) {
        when (event) {
            is AgendaUiEvent.OnAccessChanged -> uiState.update { state ->
                val canRead = event.access == AgendaAccess.GRANTED || event.access == AgendaAccess.LIMITED
                // Why: an agenda read with this access stays until the next read; a new access waits for its own.
                state.copy(
                    access = event.access,
                    contacts = if (canRead && state.showsContacts) state.contacts else Loadable.Loading,
                )
            }

            is AgendaUiEvent.OnContactsLoaded -> uiState.update { state ->
                if (state.showsContacts) state.copy(contacts = event.contacts.toLoaded()) else state
            }

            is AgendaUiEvent.OnQueryChanged -> uiState.update { it.copy(query = event.query) }

            AgendaUiEvent.OnAllowAccessClicked -> if (uiState.value.showsAskAccess) {
                trackEvent("agenda_allow_access_clicked", emptyMap())
                emit(AgendaUiAction.RequestAccess)
            }

            AgendaUiEvent.OnOpenSettingsClicked -> if (uiState.value.showsDenied) {
                trackEvent("agenda_settings_clicked", emptyMap())
                emit(AgendaUiAction.OpenSettings)
            }

            is AgendaUiEvent.OnInviteClicked -> {
                val contact = uiState.value.contacts
                    .valueOrNull()
                    ?.firstOrNull { it.id == event.contactId } ?: return
                val invite = Route.ContactInvite(
                    personKey = null,
                    name = contact.name,
                )
                if (navigate(invite)) trackEvent("agenda_invite_clicked", emptyMap())
            }

            AgendaUiEvent.OnInviteWithLinkClicked -> {
                val invite = Route.ContactInvite(
                    personKey = null,
                    name = null,
                )
                if (navigate(invite)) trackEvent("contacts_invite_with_link_clicked", sourceParams)
            }

            AgendaUiEvent.OnBackClicked -> if (navigator.backStack.value.lastOrNull() == Route.ContactsAgenda) {
                navigator.navigateBack()
            }
        }
    }

    /*
     * Why: from its own screen, or from Contatos' pane while Contatos is on top; never under the invite it opened. Says
     * whether it opened, which Analytics counts.
     */
    private fun navigate(route: Route): Boolean {
        val top = navigator.backStack.value.lastOrNull()
        if (top != Route.ContactsAgenda && top != Route.Contacts) return false
        navigator.navigate(route)
        return true
    }

    private fun emit(action: AgendaUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }
}
