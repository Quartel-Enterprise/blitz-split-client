package com.quare.blitzsplit.android.storescreenshots

import androidx.compose.ui.unit.TextUnit

/**
 * The banner's font sizes on a canvas wider than tall. Under a frame of its own the library writes the banner at 30sp
 * and 16sp whatever the canvas, which on the iPad lying down is small beside a device 1000dp wide; the iPad's slots
 * and Play's tablets name larger ones, in step with their canvas, and the description that takes two lines on a
 * phone fits in one.
 */
internal data class BannerFontSizes(
    val title: TextUnit,
    val description: TextUnit,
)
