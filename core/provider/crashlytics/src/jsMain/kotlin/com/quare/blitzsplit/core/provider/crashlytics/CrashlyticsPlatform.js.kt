package com.quare.blitzsplit.core.provider.crashlytics

private val v8Frame = Regex("""^\s*at .*:\d+:\d+\)?$""")

// Why: the browser's uncaught errors are caught by the web app's own reporter (webApp/src/crash).
internal actual fun reportUnhandledExceptions() {}

/*
 * Why: a JS error's stack is a string made where the error is, so the report takes the failure's own. V8 starts it
 * with the error's name and message, so only the lines that are frames go. A stack with none of V8's frames is
 * Firefox's or Safari's, which carry no message, unless the message is in it: then nothing is taken.
 */
internal actual fun Throwable.adoptStackTrace(from: Throwable) {
    val stack = from.asDynamic().stack as? String ?: return
    val frames = stack.lines().filter(v8Frame::matches)
    val message = from.message.orEmpty()
    asDynamic().stack = when {
        frames.isNotEmpty() -> frames.joinToString(separator = "\n")
        message.isNotEmpty() && message in stack -> ""
        else -> stack
    }
}
