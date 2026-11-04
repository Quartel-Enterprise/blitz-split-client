package com.quare.blitzsplit.core.settings.domain.model

// Why: only Android 12 and later draw a scheme from the wallpaper (Material You); each platform says whether it can.
fun interface DynamicColorSupport {
    operator fun invoke(): Boolean
}
