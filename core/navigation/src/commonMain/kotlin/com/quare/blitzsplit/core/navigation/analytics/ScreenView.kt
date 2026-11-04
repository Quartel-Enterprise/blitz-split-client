package com.quare.blitzsplit.core.navigation.analytics

// Why: one screen_view, GA4's event for a screen shown: its stable name and what kind of visit it was.
internal data class ScreenView(
    val name: String,
    val params: Map<String, Any> = emptyMap(),
)
