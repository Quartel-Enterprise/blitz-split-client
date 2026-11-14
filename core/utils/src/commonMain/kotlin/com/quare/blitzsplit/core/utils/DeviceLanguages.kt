package com.quare.blitzsplit.core.utils

/*
 * Why: the tags of the languages the device prefers, in its own order ("ca-ES", "es-ES"), empty when the platform
 * doesn't say. Each platform draws the app in the first of them it has strings for, so what must name the language
 * the app is speaking walks the same list.
 */
fun interface DeviceLanguages {
    operator fun invoke(): List<String>
}
