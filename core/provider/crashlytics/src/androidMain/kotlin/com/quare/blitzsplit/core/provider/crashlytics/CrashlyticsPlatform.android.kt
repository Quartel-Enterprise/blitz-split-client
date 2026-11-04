package com.quare.blitzsplit.core.provider.crashlytics

// Why: Crashlytics catches every uncaught exception of the JVM by itself.
internal actual fun reportUnhandledExceptions() {}

internal actual fun Throwable.adoptStackTrace(from: Throwable) {
    stackTrace = from.stackTrace
}
