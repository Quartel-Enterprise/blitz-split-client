package com.quare.blitzsplit.core.provider.crashlytics

/*
 * Why: a failure the app handles but nobody expects, recorded as a non-fatal in Crashlytics, or in Sentry on the web.
 * Nothing is sent until the app starts crash reporting (startCrashReporting, or attachWebCrashReporter on the web),
 * so a build without Firebase or without Sentry's DSN and the tests record nothing. Only the names of the failure's classes go, never their messages, which may hold names, amounts and ids
 * (a request's URL, PostgREST's details), unless the failure is a ReportableFailure.
 */
fun interface CrashReporter {
    fun recordException(throwable: Throwable)
}
