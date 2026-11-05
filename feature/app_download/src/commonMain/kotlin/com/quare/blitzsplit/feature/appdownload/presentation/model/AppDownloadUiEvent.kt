package com.quare.blitzsplit.feature.appdownload.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface AppDownloadUiEvent : UiEvent {
    // Why: Close, ✕, Esc or a click outside.
    data object OnCloseClicked : AppDownloadUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.NotTracked
    }

    // Why: "I downloaded it", which dismisses the invite for good, wherever it shows, and closes the code.
    data object OnDownloadedClicked : AppDownloadUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Manual("app_invite_downloaded_clicked")
    }

    // Why: the copy button beside the link.
    data object OnCopyLinkClicked : AppDownloadUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Automatic("app_download_link_copied")
    }
}
