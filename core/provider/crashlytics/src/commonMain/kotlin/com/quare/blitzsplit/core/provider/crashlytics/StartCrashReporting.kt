package com.quare.blitzsplit.core.provider.crashlytics

import co.touchlab.crashkios.crashlytics.CrashlyticsKotlin
import co.touchlab.crashkios.crashlytics.enableCrashlytics

/*
 * Why: called by each app once Firebase is set up, never before, since Crashlytics needs its FirebaseApp. A debug
 * build sends nothing: the setting persists on the device, so every launch sets it again, and a release installed
 * over a debug build sends again.
 */
fun startCrashReporting(isDebug: Boolean) {
    enableCrashlytics()
    CrashlyticsKotlin.setCollectionEnabled(!isDebug)
    reportUnhandledExceptions()
}

internal expect fun reportUnhandledExceptions()
