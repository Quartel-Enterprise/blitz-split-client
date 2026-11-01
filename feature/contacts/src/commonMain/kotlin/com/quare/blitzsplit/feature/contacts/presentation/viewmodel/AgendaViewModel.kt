package com.quare.blitzsplit.feature.contacts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
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

/*
 * Why: "Convidar da agenda" (11.3). The platform reads the agenda and asks for it; the shared code decides what each
 * access shows and where an invite goes. No number leaves the device: whoever is invited gets a group's link, and
 * nobody is told who already uses the app (that needs a confirmed phone, a later flow).
 */
class AgendaViewModel(
    private val navigator: Navigator,
) : ViewModel() {
    val uiState: StateFlow<AgendaUiState>
        field = MutableStateFlow(
            AgendaUiState(
                access = AgendaAccess.UNKNOWN,
                contacts = emptyList(),
                query = "",
            ),
        )

    val uiAction: SharedFlow<AgendaUiAction>
        field = MutableSharedFlow<AgendaUiAction>()

    val stateWatcher: FlowWatcher<AgendaUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<AgendaUiAction> = FlowWatcher(uiAction)

    fun onEvent(event: AgendaUiEvent) {
        when (event) {
            is AgendaUiEvent.OnAccessChanged -> uiState.update { state ->
                val canRead = event.access == AgendaAccess.GRANTED || event.access == AgendaAccess.LIMITED
                state.copy(
                    access = event.access,
                    contacts = if (canRead) state.contacts else emptyList(),
                )
            }

            is AgendaUiEvent.OnContactsLoaded -> uiState.update { state ->
                if (state.showsContacts) state.copy(contacts = event.contacts) else state
            }

            is AgendaUiEvent.OnQueryChanged -> uiState.update { it.copy(query = event.query) }

            AgendaUiEvent.OnAllowAccessClicked -> if (uiState.value.showsAskAccess) emit(AgendaUiAction.RequestAccess)

            AgendaUiEvent.OnOpenSettingsClicked -> if (uiState.value.showsDenied) emit(AgendaUiAction.OpenSettings)

            is AgendaUiEvent.OnInviteClicked -> {
                val contact = uiState.value.contacts.firstOrNull { it.id == event.contactId } ?: return
                navigate(
                    Route.ContactInvite(
                        personKey = null,
                        name = contact.name,
                    ),
                )
            }

            AgendaUiEvent.OnInviteWithLinkClicked -> navigate(
                Route.ContactInvite(
                    personKey = null,
                    name = null,
                ),
            )

            AgendaUiEvent.OnBackClicked -> if (navigator.backStack.value.lastOrNull() == Route.ContactsAgenda) {
                navigator.navigateBack()
            }
        }
    }

    // Why: from its own screen, or from Contatos' pane while Contatos is on top; never under the invite it opened.
    private fun navigate(route: Route) {
        val top = navigator.backStack.value.lastOrNull()
        if (top == Route.ContactsAgenda || top == Route.Contacts) navigator.navigate(route)
    }

    private fun emit(action: AgendaUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }
}
