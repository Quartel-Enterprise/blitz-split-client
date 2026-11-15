@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.provider.crashlytics.attachWebCrashReporter
import com.quare.blitzsplit.core.provider.crashlytics.findKotlinClassNameOf

/*
 * Why: Sentry on the web (webApp/src/crash), attached only by a production build with its DSN. Each failure the
 * shared code records crosses as the JS error it is, already redacted to its classes and stack by CrashReporter.
 */
fun attachCrashReporter(report: (error: dynamic) -> Unit) {
    attachWebCrashReporter { throwable -> report(throwable) }
}

// Why: an uncaught Kotlin failure's JS name is shortened by the bundler; Sentry names the issue by this one instead.
fun findKotlinClassName(error: dynamic): String? = (error as? Throwable)?.let(::findKotlinClassNameOf)
