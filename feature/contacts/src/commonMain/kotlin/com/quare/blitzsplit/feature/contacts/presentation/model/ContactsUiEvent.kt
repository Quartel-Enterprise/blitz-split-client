package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface ContactsUiEvent : UiEvent {
    // Why: a row tapped: its profile, beside the list where the UI draws the pane, over it everywhere else.
    data class OnContactClicked(
        val key: String,
    ) : ContactsUiEvent

    // Why: whether the UI draws the pane beside the list, which changes what a tap does.
    data class OnDetailPaneChanged(
        val isShown: Boolean,
    ) : ContactsUiEvent

    data object OnSearchOpened : ContactsUiEvent

    data class OnQueryChanged(
        val query: String,
    ) : ContactsUiEvent

    // Why: back, ✕ or Esc: the search forgets what was typed.
    data object OnSearchClosed : ContactsUiEvent

    // Why: "Ocultos · 1" opened or closed.
    data object OnHiddenToggled : ContactsUiEvent

    // Why: "Mostrar" on someone hidden.
    data class OnShowContactClicked(
        val key: String,
    ) : ContactsUiEvent

    // Why: the summary card: "Ver Saldos", "Você deve" or "Te devem".
    data object OnBalancesCardClicked : ContactsUiEvent

    data object OnYouOweClicked : ContactsUiEvent

    data object OnOwedToYouClicked : ContactsUiEvent

    // Why: "Convidar da agenda" (11.3), not on the web.
    data object OnAgendaClicked : ContactsUiEvent

    // Why: "Convidar com link ou QR" (11.4), from the list, the empty state or a search with no result.
    data object OnInviteWithLinkClicked : ContactsUiEvent

    // Why: "Convidar" on someone of the agenda the search found.
    data class OnAgendaContactInviteClicked(
        val contactId: String,
    ) : ContactsUiEvent

    // Why: the platform read the agenda, with the access it already had (it never asks from here).
    data class OnAgendaLoaded(
        val contacts: List<AgendaContact>,
    ) : ContactsUiEvent

    data object OnGroupsTabClicked : ContactsUiEvent

    data object OnBalancesTabClicked : ContactsUiEvent

    data object OnActivityTabClicked : ContactsUiEvent

    data object OnProfileClicked : ContactsUiEvent

    // Why: back from the pane, which closes it to its placeholder.
    data object OnPaneBackClicked : ContactsUiEvent
}
