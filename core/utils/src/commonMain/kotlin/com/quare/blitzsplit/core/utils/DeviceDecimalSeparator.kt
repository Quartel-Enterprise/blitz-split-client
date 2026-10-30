package com.quare.blitzsplit.core.utils

/*
 * Why: the decimal separator of the device's language ("," for pt-BR, "." for en-US), so an amount the app fills in
 * a field reads as one the person would type. Each platform reads it.
 */
fun interface DeviceDecimalSeparator {
    operator fun invoke(): Char
}
