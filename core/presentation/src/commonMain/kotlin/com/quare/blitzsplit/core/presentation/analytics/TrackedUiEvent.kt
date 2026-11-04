package com.quare.blitzsplit.core.presentation.analytics

import com.quare.blitzsplit.core.presentation.UiEvent

/*
 * Why: a UiEvent that declares what it sends to Analytics. Every feature's UiEvent moves to it; once all have, it
 * merges into UiEvent, so no user action can ship without the decision.
 */
interface TrackedUiEvent : UiEvent {
    val analytics: EventAnalytics
}
