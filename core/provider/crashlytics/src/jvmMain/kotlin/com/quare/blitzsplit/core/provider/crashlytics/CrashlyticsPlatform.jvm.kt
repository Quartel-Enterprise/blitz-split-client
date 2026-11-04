package com.quare.blitzsplit.core.provider.crashlytics

// Why: the jvm target only runs the unit tests.
internal actual fun reportUnhandledExceptions() {}

internal actual fun Throwable.adoptStackTrace(from: Throwable) {
    stackTrace = from.stackTrace
}
