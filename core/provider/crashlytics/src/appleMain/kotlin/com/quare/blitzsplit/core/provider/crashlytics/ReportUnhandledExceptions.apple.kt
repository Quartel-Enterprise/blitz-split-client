package com.quare.blitzsplit.core.provider.crashlytics

import co.touchlab.crashkios.crashlytics.setCrashlyticsUnhandledExceptionHook

// Why: a Kotlin exception nobody catches terminates the app; without the hook Crashlytics shows only the abort.
internal actual fun reportUnhandledExceptions() {
    setCrashlyticsUnhandledExceptionHook()
}
