package com.quare.blitzsplit.core.settings.domain.usecase

/*
 * Why: hands the device's "Share usage data" to Analytics for the app's whole life, so turning it off stops the events
 * at once and a device that turned it off sends nothing from its next launch on. Suspends until its scope ends.
 */
fun interface FollowUsageDataSharing {
    suspend operator fun invoke()
}
