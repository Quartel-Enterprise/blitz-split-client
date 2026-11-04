package com.quare.blitzsplit.core.provider.crashlytics

import co.touchlab.crashkios.crashlytics.CrashlyticsKotlin

// Why: through CrashKiOS, so Apple's Crashlytics gets the Kotlin stack, not the Swift frames that called into it.
internal class CrashlyticsReporter : CrashReporter {
    override fun recordException(throwable: Throwable) {
        val reported = if (throwable is ReportableFailure) throwable else RedactedException(throwable)
        CrashlyticsKotlin.sendHandledException(reported)
    }
}
