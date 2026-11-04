package com.quare.blitzsplit.core.provider.crashlytics

// Why: Firebase has no Crashlytics for the web.
internal actual fun reportUnhandledExceptions() {}

// Why: nothing is reported from the web.
internal actual fun Throwable.adoptStackTrace(from: Throwable) {}
