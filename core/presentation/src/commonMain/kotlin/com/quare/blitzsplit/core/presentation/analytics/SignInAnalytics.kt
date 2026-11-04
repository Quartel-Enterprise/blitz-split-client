package com.quare.blitzsplit.core.presentation.analytics

import com.quare.blitzsplit.core.model.AccountProvider

/*
 * Why: Google's and Apple's sign in is the same on every screen that offers it, so its events are named once, source
 * naming the screen. The native sheet failing or closed ends the funnel the tap began, so those results count too.
 */
object SignInAnalytics {
    fun buildProviderClicked(
        provider: AccountProvider,
        source: String,
    ): EventAnalytics = EventAnalytics.Automatic(
        "sign_in_provider_clicked",
        mapOf("provider" to provider, "source" to source),
    )

    fun buildNativeSignInFailed(source: String): EventAnalytics =
        EventAnalytics.Automatic("native_sign_in_failed", mapOf("source" to source))

    fun buildNativeSignInCancelled(source: String): EventAnalytics =
        EventAnalytics.Automatic("native_sign_in_cancelled", mapOf("source" to source))
}
