package com.quare.blitzsplit.core.provider.analytics

// Why: an event tracked before a service attached, kept as TrackEvent normalized it.
internal data class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any>,
)
