package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

sealed interface AgendaUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            /*
             * Why: what the platform says (the access, the agenda read), a value typed letter by letter, or a back,
             * which each platform also does its own way: nobody tapped to act.
             */
            is OnAccessChanged,
            is OnContactsLoaded,
            is OnQueryChanged,
            OnBackClicked,
            -> EventAnalytics.NotTracked

            /*
             * Why: each counts only when the screen takes it (someone still listed, the screen still on top or beside
             * Contatos, a question still open), never a late tap, as the ViewModel checks before acting.
             */
            OnAllowAccessClicked -> EventAnalytics.Manual("agenda_allow_access_clicked")

            OnOpenSettingsClicked -> EventAnalytics.Manual("agenda_settings_clicked")

            is OnInviteClicked -> EventAnalytics.Manual("agenda_invite_clicked")

            OnInviteWithLinkClicked -> EventAnalytics.Manual("contacts_invite_with_link_clicked")
        }

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
