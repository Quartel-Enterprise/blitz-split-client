package com.quare.blitzsplit.feature.connectwatch.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface ConnectWatchUiEvent : UiEvent {
    // Why: the code field's text as typed or pasted.
    data class OnCodeChanged(
        val text: String,
    ) : ConnectWatchUiEvent {
        override val analytics: EventAnalytics
            get() = EventAnalytics.NotTracked
    }

    // Why: "Connect", with the server's answer as its result.
    data object OnConnectClicked : ConnectWatchUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Manual("connect_watch_clicked")
    }

    // Why: ✕ on top, or Close once connected.
    data object OnCloseClicked : ConnectWatchUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Automatic("connect_watch_closed")
    }
}
