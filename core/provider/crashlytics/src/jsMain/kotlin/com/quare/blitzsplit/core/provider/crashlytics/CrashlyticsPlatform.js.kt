package com.quare.blitzsplit.core.provider.crashlytics

private const val V8_FRAME = "at "

// Why: the browser's uncaught errors are caught by the web app's own reporter (webApp/src/crash).
internal actual fun reportUnhandledExceptions() {}

/*
 * Why: a JS error's stack is a string made where the error is, so the report takes the failure's own. V8 starts it
 * with the error's name and message, which is dropped: only the frames go.
 */
internal actual fun Throwable.adoptStackTrace(from: Throwable) {
    val stack = from.asDynamic().stack as? String ?: return
    val frames = stack.lines().dropWhile { !it.trimStart().startsWith(V8_FRAME) }
    asDynamic().stack = if (frames.isEmpty()) stack else frames.joinToString(separator = "\n")
}
