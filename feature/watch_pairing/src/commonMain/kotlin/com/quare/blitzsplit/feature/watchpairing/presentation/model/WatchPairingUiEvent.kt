package com.quare.blitzsplit.feature.watchpairing.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface WatchPairingUiEvent : UiEvent {
    // Why: "Connect" on the first launch (15.9).
    data object OnConnectClicked : WatchPairingUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Automatic("watch_connect_clicked")
    }

    // Why: "Connect again" after the phone signed out (15.13).
    data object OnConnectAgainClicked : WatchPairingUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Automatic("watch_connect_again_clicked")
    }

    // Why: the platform's answer to SendCodeToPhone: the phone's reply, or no phone to send it to.
    data class OnPhoneReplied(
        val reply: PhoneReply,
    ) : WatchPairingUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.NotTracked
    }

    // Why: "Try again" with the phone out of reach (15.11c).
    data object OnTryAgainClicked : WatchPairingUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Automatic("watch_connect_try_again_clicked")
    }

    // Why: "Use code" with the phone out of reach (15.11c).
    data object OnUseCodeClicked : WatchPairingUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Automatic("watch_use_code_clicked")
    }

    // Why: "Cancel" under the code (15.12), back to the phone out of reach.
    data object OnCodeCancelled : WatchPairingUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Automatic("watch_code_cancelled")
    }

    // Why: "OK" on the phone's local only profile (15.11b).
    data object OnLocalOnlyAcknowledged : WatchPairingUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Automatic("watch_local_only_acknowledged")
    }

    // Why: "Continue" once connected (15.11), to the balances.
    data object OnContinueClicked : WatchPairingUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Automatic("watch_connected_continue_clicked")
    }
}
