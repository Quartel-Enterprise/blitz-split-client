package com.quare.blitzsplit.core.provider.crashlytics

/*
 * Why: a failure the app handles but nobody expects, recorded as a non-fatal in Crashlytics. Nothing is sent until
 * the app starts crash reporting (startCrashReporting), so a build without Firebase, the web and the tests record
 * nothing.
 */
fun interface CrashReporter {
    fun recordException(throwable: Throwable)
}
