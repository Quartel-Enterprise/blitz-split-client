package com.quare.blitzsplit.core.provider.crashlytics

/*
 * Why: a failure the app handles but nobody expects, recorded as a non-fatal in Crashlytics. Nothing is sent until
 * the app starts crash reporting (startCrashReporting), so a build without Firebase, the web and the tests record
 * nothing. Only the names of the failure's classes go, never their messages, which may hold names, amounts and ids
 * (a request's URL, PostgREST's details), unless the failure is a ReportableFailure.
 */
fun interface CrashReporter {
    fun recordException(throwable: Throwable)
}
