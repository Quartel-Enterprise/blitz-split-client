package com.quare.blitzsplit.core.provider.crashlytics

import co.touchlab.crashkios.crashlytics.setCrashlyticsUnhandledExceptionHook

// Why: a Kotlin exception nobody catches terminates the app; without the hook Crashlytics shows only the abort.
internal actual fun reportUnhandledExceptions() {
    setCrashlyticsUnhandledExceptionHook()
}

// Why: Kotlin/Native keeps a throwable's stack from where it was made, with no way to set another.
internal actual fun Throwable.adoptStackTrace(from: Throwable) {}
