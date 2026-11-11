package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface ProfileLinkUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back or a close, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            // Why: each counts only when the screen takes it, never a late tap, as the ViewModel checks before acting.
            OnRetryClicked -> EventAnalytics.Manual("retry_clicked")

            OnInviteClicked -> EventAnalytics.Manual("profile_link_invite_clicked")
        }

    data object OnBackClicked : ProfileLinkUiEvent

    // Why: "Try again", while the server couldn't be reached.
    data object OnRetryClicked : ProfileLinkUiEvent

    // Why: "Invite Ana to a group", someone this device shares no group with: Contacts' invite with a group's link.
    data object OnInviteClicked : ProfileLinkUiEvent
}
