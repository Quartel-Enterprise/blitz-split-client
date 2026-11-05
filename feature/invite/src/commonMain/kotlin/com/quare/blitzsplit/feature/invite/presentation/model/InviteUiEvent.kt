package com.quare.blitzsplit.feature.invite.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "invite"

sealed interface InviteUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            /*
             * Why: a close, which each platform also does its own way (a swipe, back): where it went is its screen
             * view.
             */
            OnCloseClicked -> EventAnalytics.NotTracked

            OnCopyClicked -> EventAnalytics.Automatic("invite_link_copy_clicked", mapOf("source" to SOURCE))

            OnShareClicked -> EventAnalytics.Automatic("invite_link_share_clicked", mapOf("source" to SOURCE))

            OnSeeWhoJoinedClicked -> EventAnalytics.Automatic("invite_see_who_joined_clicked")

            OnRetryClicked -> EventAnalytics.Automatic("retry_clicked", mapOf("source" to SOURCE))

            OnAddPersonClicked -> EventAnalytics.Automatic("invite_add_person_clicked")

            OnGoToGroupClicked -> EventAnalytics.Automatic("invite_go_to_group_clicked")

            OnChangeLinkClicked -> EventAnalytics.Automatic("invite_link_change_clicked")

            // Why: only an answer to a question still open, as its cancellation.
            OnChangeLinkConfirmed -> EventAnalytics.Manual("invite_link_change_confirmed")

            // Why: only a question still open, since Apple's alert also closes itself after Trocar.
            OnChangeLinkDismissed -> EventAnalytics.Manual("invite_link_change_cancelled")
        }

    data object OnCloseClicked : InviteUiEvent

    data object OnCopyClicked : InviteUiEvent

    // Why: "Compartilhar link", the system's share sheet.
    data object OnShareClicked : InviteUiEvent

    // Why: "Ver quem já entrou", the group's "Membros".
    data object OnSeeWhoJoinedClicked : InviteUiEvent

    // Why: "Add someone without the app", the group's Members with "Add person" open.
    data object OnAddPersonClicked : InviteUiEvent

    // Why: "Tentar de novo", while the link failed to come.
    data object OnRetryClicked : InviteUiEvent

    // Why: "Ir para o grupo", offered while the link is pending.
    data object OnGoToGroupClicked : InviteUiEvent

    // Why: "Trocar link", which asks "Trocar o link do convite?" first.
    data object OnChangeLinkClicked : InviteUiEvent

    data object OnChangeLinkConfirmed : InviteUiEvent

    data object OnChangeLinkDismissed : InviteUiEvent
}
