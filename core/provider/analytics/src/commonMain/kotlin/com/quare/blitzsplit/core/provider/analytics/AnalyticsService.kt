package com.quare.blitzsplit.core.provider.analytics

/*
 * Why: where each platform sends an event, Firebase Analytics through its own SDK: Android's here, Apple's in Swift,
 * the web's in TypeScript. A value is a String, a Long or a Double, as GA4 takes them.
 */
fun interface AnalyticsService {
    fun logEvent(
        name: String,
        params: Map<String, Any>,
    )
}
