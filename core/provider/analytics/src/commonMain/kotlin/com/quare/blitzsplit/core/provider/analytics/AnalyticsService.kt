package com.quare.blitzsplit.core.provider.analytics

/*
 * Why: where each platform sends an event, Firebase Analytics through its own SDK: Android's here, Apple's in Swift,
 * the web's in TypeScript. A value is a String, a Long or a Double, as GA4 takes them.
 */
interface AnalyticsService {
    fun logEvent(
        name: String,
        params: Map<String, Any>,
    )

    // Why: Profile's "Share usage data"; off, the SDK also stops what it collects on its own (first_open, sessions).
    fun setCollectionEnabled(isEnabled: Boolean)

    // Why: the account's id, so one person's events join across their devices; null without an account.
    fun setUserId(userId: String?)
}
