package com.quare.blitzsplit.core.provider.crashlytics

// Why: Crashlytics catches every uncaught exception of the JVM by itself.
internal actual fun reportUnhandledExceptions() {}
