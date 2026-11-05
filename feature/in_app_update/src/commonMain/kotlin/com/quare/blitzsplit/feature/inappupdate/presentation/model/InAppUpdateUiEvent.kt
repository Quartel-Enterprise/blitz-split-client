package com.quare.blitzsplit.feature.inappupdate.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface InAppUpdateUiEvent : UiEvent {
    // Why: "Abrir na App Store"; what offered it comes from the route, so the ViewModel sends it.
    data object OnUpdateClicked : InAppUpdateUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Manual("update_accepted")
    }

    // Why: "Agora não" or Esc, the sheet's only other way out.
    data object OnDismissed : InAppUpdateUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Manual("update_dismissed")
    }
}
