package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface AgendaUiEvent : UiEvent {
    // Why: what the platform says about the access, on opening and each time the app comes back from the settings.
    data class OnAccessChanged(
        val access: AgendaAccess,
    ) : AgendaUiEvent

    data class OnContactsLoaded(
        val contacts: List<AgendaContact>,
    ) : AgendaUiEvent

    data class OnQueryChanged(
        val query: String,
    ) : AgendaUiEvent

    // Why: "Permitir acesso à agenda", which hands the question to the system.
    data object OnAllowAccessClicked : AgendaUiEvent

    data object OnOpenSettingsClicked : AgendaUiEvent

    data class OnInviteClicked(
        val contactId: String,
    ) : AgendaUiEvent

    data object OnInviteWithLinkClicked : AgendaUiEvent

    data object OnBackClicked : AgendaUiEvent
}
