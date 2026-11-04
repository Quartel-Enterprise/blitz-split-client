package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

sealed interface ContactsUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            /*
             * Why: the window deciding the layout, a back that closes the pane, or a home tab (bar, rail, sidebar) each
             * platform routes through the screen in front: nobody tapped to act here, and where it went is its screen view.
             */
            is OnDetailPaneChanged,
            OnActivityTabClicked,
            OnBalancesTabClicked,
            OnGroupsTabClicked,
            OnHomeTabClicked,
            OnPaneBackClicked,
            -> EventAnalytics.NotTracked

            /*
             * Why: each counts only when Contatos takes it (someone still listed, someone hidden to show, Contatos on top
             * or the pane beside it), never a late tap, as the ViewModel checks before acting.
             */
            is OnContactClicked -> EventAnalytics.Manual("contact_clicked")

            OnSearchClicked -> EventAnalytics.Manual("search_opened")

            OnHiddenToggled -> EventAnalytics.Manual("contacts_hidden_toggled")

            is OnShowContactClicked -> EventAnalytics.Manual("contact_show_clicked")

            OnBalancesCardClicked,
            OnYouOweClicked,
            OnOwedToYouClicked,
            -> EventAnalytics.Manual("contacts_balances_clicked")

            OnAgendaClicked -> EventAnalytics.Manual("contacts_agenda_clicked")

            OnInviteWithLinkClicked -> EventAnalytics.Manual("contacts_invite_with_link_clicked")

            OnProfileClicked -> EventAnalytics.Manual("profile_avatar_clicked")
        }

    // Why: a row tapped: its profile, beside the list where the UI draws the pane, over it everywhere else.
    data class OnContactClicked(
        val key: String,
    ) : ContactsUiEvent

    // Why: whether the UI draws the pane beside the list, which changes what a tap does.
    data class OnDetailPaneChanged(
        val isShown: Boolean,
    ) : ContactsUiEvent

    // Why: "Buscar" (5.2), the same search as Grupos', over Contatos: the magnifier, the web's ⌘K.
    data object OnSearchClicked : ContactsUiEvent

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

    // Why: "Convidar com link ou QR" (11.4), from the list or the empty state.
    data object OnInviteWithLinkClicked : ContactsUiEvent

    data object OnHomeTabClicked : ContactsUiEvent

    data object OnGroupsTabClicked : ContactsUiEvent

    data object OnBalancesTabClicked : ContactsUiEvent

    data object OnActivityTabClicked : ContactsUiEvent

    data object OnProfileClicked : ContactsUiEvent

    // Why: back from the pane, which closes it to its placeholder.
    data object OnPaneBackClicked : ContactsUiEvent
}
