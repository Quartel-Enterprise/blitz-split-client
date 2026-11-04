package com.quare.blitzsplit.core.provider.analytics.screen

// Why: one screen_view, GA4's event for a screen shown: its stable name and what kind of screen it is.
internal data class ScreenView(
    val name: String,
    val params: Map<String, Any>,
)
