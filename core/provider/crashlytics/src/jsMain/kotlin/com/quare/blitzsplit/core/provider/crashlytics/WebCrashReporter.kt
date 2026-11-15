package com.quare.blitzsplit.core.provider.crashlytics

import co.touchlab.crashkios.crashlytics.CrashlyticsCalls
import co.touchlab.crashkios.crashlytics.CrashlyticsKotlin

/*
 * Why: Firebase has no Crashlytics for the browser, so the web app hands its own reporter (Sentry) over, which takes
 * the place of the SDK CrashKiOS calls on Android and Apple. Until then, and in a build without it, CrashKiOS' own
 * default records nothing.
 */
fun attachWebCrashReporter(report: (Throwable) -> Unit) {
    CrashlyticsKotlin.implementation = WebCrashlyticsCalls(report)
}

// Why: only handled failures cross; the page's uncaught errors are the web reporter's own to catch.
private class WebCrashlyticsCalls(
    private val report: (Throwable) -> Unit,
) : CrashlyticsCalls {
    override fun sendHandledException(throwable: Throwable) {
        report(throwable)
    }

    override fun sendFatalException(throwable: Throwable) {}

    override fun logMessage(message: String) {}

    override fun setCustomValue(
        key: String,
        value: Any,
    ) {}

    override fun setUserId(identifier: String) {}

    override fun setCollectionEnabled(enabled: Boolean) {}
}

/*
 * Why: a Kotlin failure's JS name is its constructor's, which the web app's bundler shortens to a letter or two, so
 * the web reporter asks for the class's own name. Null for an error Kotlin didn't define, whose name is already right.
 */
fun findKotlinClassNameOf(error: Throwable): String? {
    val constructor = error.asDynamic().constructor
    val isKotlinClass = constructor != null && constructor["\$metadata\$"] != null
    return if (isKotlinClass) error::class.simpleName else null
}
