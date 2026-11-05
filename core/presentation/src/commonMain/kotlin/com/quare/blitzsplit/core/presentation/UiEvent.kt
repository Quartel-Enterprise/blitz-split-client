package com.quare.blitzsplit.core.presentation

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

/*
 * Why: the user interactions every ViewModel receives through onEvent, each declaring what it sends to Analytics, so
 * the compiler asks for that decision on every new one (docs/analytics/README.md) and no user action ships without it.
 */
interface UiEvent {
    val analytics: EventAnalytics
}
