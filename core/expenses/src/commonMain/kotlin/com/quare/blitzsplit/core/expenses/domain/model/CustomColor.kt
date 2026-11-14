package com.quare.blitzsplit.core.expenses.domain.model

import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Why: a color the person created with "+" in a category's color picker, from the gradient or a hex or RGB code, kept
 * in their palette (synced with the account) and given to any category. Stored everywhere as its hex, `#RRGGBB` in
 * uppercase, the one shape Room, the server and the three UIs share. It is only a base: the badge, its text and the
 * mark in a chart are tones derived from it (toTones), as each named color has its own.
 */
data class CustomColor(
    // Why: 0xRRGGBB, no alpha.
    val rgb: Int,
) {
    init {
        require(rgb in 0..MAX_RGB) { "Not an RGB color: $rgb" }
    }

    val red: Int
        get() = (rgb shr 16) and CHANNEL_MAX

    val green: Int
        get() = (rgb shr 8) and CHANNEL_MAX

    val blue: Int
        get() = rgb and CHANNEL_MAX

    val hex: String
        get() = "#" + rgb.toString(HEX_RADIX).uppercase().padStart(HEX_DIGITS, '0')

    // Why: the gradient's position (hue 0 to 360, saturation and value 0 to 1) for a color typed as a code.
    fun toHsv(): Hsv {
        val r = red / CHANNEL_MAX_D
        val g = green / CHANNEL_MAX_D
        val b = blue / CHANNEL_MAX_D
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val hue = when {
            delta == 0.0 -> 0.0
            max == r -> HUE_SECTOR * (((g - b) / delta).mod(SECTORS_IN_RED))
            max == g -> HUE_SECTOR * ((b - r) / delta + 2)
            else -> HUE_SECTOR * ((r - g) / delta + 4)
        }
        return Hsv(
            hue = hue,
            saturation = if (max == 0.0) 0.0 else delta / max,
            value = max,
        )
    }

    // Why: worked out once per color, as every badge, row and slice asks for it on each draw.
    private val tones: CategoryTones by lazy { CustomColorTones.of(this) }

    // Why: the badge, its text and the chart's mark, in light and dark, derived so each reads as the named ones do.
    fun toTones(): CategoryTones = tones

    companion object {
        private const val MAX_RGB = 0xFFFFFF
        private const val CHANNEL_MAX = 0xFF
        private const val CHANNEL_MAX_D = 255.0
        private const val HEX_RADIX = 16
        private const val HEX_DIGITS = 6
        private const val HUE_SECTOR = 60.0
        private const val SECTORS_IN_RED = 6.0
        private const val FULL_TURN = 360.0

        /*
         * Why: what the hex field takes: six hex digits with or without "#", any case, spaces around ignored. Three
         * digits (#FA0) are not taken, so a code is never read as another while it's being typed.
         */
        fun fromHexOrNull(text: String?): CustomColor? {
            val digits = text?.trim()?.removePrefix("#") ?: return null
            if (digits.length != HEX_DIGITS || !digits.all { it.isHexDigit() }) return null
            return CustomColor(digits.toInt(HEX_RADIX))
        }

        // Why: the RGB fields, each channel 0 to 255.
        fun fromRgbOrNull(
            red: Int,
            green: Int,
            blue: Int,
        ): CustomColor? {
            if (listOf(red, green, blue).any { it !in 0..CHANNEL_MAX }) return null
            return CustomColor((red shl 16) or (green shl 8) or blue)
        }

        // Why: the gradient: a hue out of range turns around the circle, saturation and value are clamped.
        fun fromHsv(
            hue: Double,
            saturation: Double,
            value: Double,
        ): CustomColor {
            val h = hue.mod(FULL_TURN)
            val s = saturation.coerceIn(0.0, 1.0)
            val v = value.coerceIn(0.0, 1.0)
            val chroma = v * s
            val x = chroma * (1 - abs((h / HUE_SECTOR).mod(2.0) - 1))
            val m = v - chroma
            val (r, g, b) = when ((h / HUE_SECTOR).toInt()) {
                0 -> Triple(chroma, x, 0.0)
                1 -> Triple(x, chroma, 0.0)
                2 -> Triple(0.0, chroma, x)
                3 -> Triple(0.0, x, chroma)
                4 -> Triple(x, 0.0, chroma)
                else -> Triple(chroma, 0.0, x)
            }
            return CustomColor(
                (channelOf(r + m) shl 16) or (channelOf(g + m) shl 8) or channelOf(b + m),
            )
        }

        private fun channelOf(fraction: Double): Int = (fraction * CHANNEL_MAX_D).roundToInt().coerceIn(0, CHANNEL_MAX)
    }
}

// Why: what a hex code is made of, the one rule CustomColor and the picker's field both read.
internal fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
