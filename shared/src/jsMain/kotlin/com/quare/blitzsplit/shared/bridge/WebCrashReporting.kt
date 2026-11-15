@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.provider.crashlytics.attachWebCrashReporter

/*
 * Why: Sentry on the web (webApp/src/crash), attached only by a production build with its DSN. Each failure the
 * shared code records crosses as the JS error it is, already redacted to its classes and stack by CrashReporter.
 */
fun attachCrashReporter(report: (error: dynamic) -> Unit) {
    attachWebCrashReporter { throwable -> report(throwable) }
}
