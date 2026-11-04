package com.quare.blitzsplit.core.provider.crashlytics

// Why: what Crashlytics gets of a failure that wasn't written to be reported: its classes, cause by cause, and its stack.
internal class RedactedException(
    original: Throwable,
) : Exception(classesOf(original)) {
    init {
        adoptStackTrace(from = original)
    }
}

private fun classesOf(original: Throwable): String = generateSequence(original) { it.cause }
    .joinToString(separator = " ← ") { it::class.simpleName ?: "Throwable" }

internal expect fun Throwable.adoptStackTrace(from: Throwable)
