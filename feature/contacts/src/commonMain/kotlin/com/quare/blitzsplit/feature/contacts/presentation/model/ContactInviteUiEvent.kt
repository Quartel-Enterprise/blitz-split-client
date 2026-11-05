package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface ContactInviteUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a close, which each platform also does its own way: where it went is its screen view.
            OnCloseClicked -> EventAnalytics.NotTracked

            /*
             * Why: each counts only when the invite takes it (a group still listed, a link there, the invite on top),
             * never a late tap, as the ViewModel checks before acting.
             */
            is OnGroupPicked -> EventAnalytics.Manual("contact_invite_group_picked")

            OnNewGroupClicked -> EventAnalytics.Manual("create_group_clicked")

            OnCopyClicked -> EventAnalytics.Manual("invite_link_copy_clicked")

            OnShareClicked -> EventAnalytics.Manual("invite_link_share_clicked")
        }

    data class OnGroupPicked(
        val groupId: String,
    ) : ContactInviteUiEvent

    // Why: the "Novo grupo" chip, which opens Novo grupo in the invite's place.
    data object OnNewGroupClicked : ContactInviteUiEvent

    data object OnCopyClicked : ContactInviteUiEvent

    data object OnShareClicked : ContactInviteUiEvent

    data object OnCloseClicked : ContactInviteUiEvent
}
