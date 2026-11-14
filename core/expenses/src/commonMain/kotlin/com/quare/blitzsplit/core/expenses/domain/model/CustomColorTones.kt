package com.quare.blitzsplit.core.expenses.domain.model

import kotlin.math.abs
import kotlin.math.pow

/*
 * Why: a custom color is a base, never drawn as is: any color may be picked, a pale yellow or a near black, and the badge's
 * text and the chart's marks must still read (docs/product/general.md: text 4.5:1, a mark 3:1). So, as the named colors
 * do, the badge is a light tint of its hue in light and a deep one in dark, its content the opposite end, and the data tone
 * the hue at the lightness that reads on a card. Each lightness is searched for its contrast, never guessed.
 */
internal object CustomColorTones {
    // Why: the badge's text is at least this far from its badge, well above 4.5:1, as the named colors are (13:1 and up).
    private const val CONTENT_CONTRAST = 7.0

    // Why: the named colors' data tones read at 4:1 on a light card and 7:1 on a dark one; a custom one reads at least so.
    private const val LIGHT_DATA_CONTRAST = 4.5
    private const val DARK_DATA_CONTRAST = 7.0

    private const val LIGHT_CONTAINER_LIGHTNESS = 0.9
    private const val DARK_CONTAINER_LIGHTNESS = 0.3
    private const val LIGHT_CONTENT_LIGHTNESS = 0.12

    // Why: a container's tint stays a tint, never a neon, even for a fully saturated base.
    private const val MAX_CONTAINER_SATURATION = 0.85

    // Why: the cards the marks sit on: white in light, the darkest surface in dark.
    private const val LIGHT_CARD = 0xFFFFFF
    private const val DARK_CARD = 0x1D1B20

    private const val SEARCH_STEPS = 24

    fun of(color: CustomColor): CategoryTones {
        val hsl = Hsl.of(color)
        val containerSaturation = minOf(hsl.saturation, MAX_CONTAINER_SATURATION)
        val lightContainer = hsl
            .copy(
                saturation = containerSaturation,
                lightness = LIGHT_CONTAINER_LIGHTNESS,
            ).toRgb()
        // Why: the content darkens until it reads on its badge; dark content on a light tint always gets there.
        val lightContent = findLightest(hsl.hue, hsl.saturation, 0.0, LIGHT_CONTENT_LIGHTNESS) {
            getContrast(it, lightContainer) >= CONTENT_CONTRAST
        }
        // Why: dark's content is light's container, as for the named colors, so the badge darkens until it reads under it.
        val darkContainer = findLightest(hsl.hue, containerSaturation, 0.0, DARK_CONTAINER_LIGHTNESS) {
            getContrast(it, lightContainer) >= CONTENT_CONTRAST
        }
        val lightData = findLightest(hsl.hue, hsl.saturation, 0.0, minOf(hsl.lightness, 0.6)) {
            getContrast(it, LIGHT_CARD) >= LIGHT_DATA_CONTRAST
        }
        val darkData = findDarkest(hsl.hue, hsl.saturation, maxOf(hsl.lightness, 0.4), 1.0) {
            getContrast(it, DARK_CARD) >= DARK_DATA_CONTRAST
        }
        return CategoryTones(
            lightContainer = lightContainer,
            lightContent = lightContent,
            darkContainer = darkContainer,
            darkContent = lightContainer,
            lightData = lightData,
            darkData = darkData,
        )
    }

    // Why: the WCAG ratio of two colors, 1 to 21.
    fun getContrast(
        first: Int,
        second: Int,
    ): Double {
        val a = getLuminance(first)
        val b = getLuminance(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    // Why: luminance grows with lightness at a fixed hue and saturation, so the closest that passes is a bisection away.
    private fun findLightest(
        hue: Double,
        saturation: Double,
        low: Double,
        high: Double,
        passes: (Int) -> Boolean,
    ): Int {
        val top = Hsl(hue, saturation, high).toRgb()
        if (passes(top)) return top
        var lo = low
        var hi = high
        repeat(SEARCH_STEPS) {
            val mid = (lo + hi) / 2
            if (passes(Hsl(hue, saturation, mid).toRgb())) lo = mid else hi = mid
        }
        return Hsl(hue, saturation, lo).toRgb()
    }

    private fun findDarkest(
        hue: Double,
        saturation: Double,
        low: Double,
        high: Double,
        passes: (Int) -> Boolean,
    ): Int {
        val bottom = Hsl(hue, saturation, low).toRgb()
        if (passes(bottom)) return bottom
        var lo = low
        var hi = high
        repeat(SEARCH_STEPS) {
            val mid = (lo + hi) / 2
            if (passes(Hsl(hue, saturation, mid).toRgb())) hi = mid else lo = mid
        }
        return Hsl(hue, saturation, hi).toRgb()
    }

    private fun getLuminance(rgb: Int): Double {
        fun toLinear(channel: Int): Double {
            val c = channel / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val color = CustomColor(rgb)
        return 0.2126 * toLinear(color.red) + 0.7152 * toLinear(color.green) + 0.0722 * toLinear(color.blue)
    }

    private data class Hsl(
        val hue: Double,
        val saturation: Double,
        val lightness: Double,
    ) {
        fun toRgb(): Int {
            val chroma = (1 - abs(2 * lightness - 1)) * saturation
            val v = lightness + chroma / 2
            val hsvSaturation = if (v == 0.0) 0.0 else 2 * (1 - lightness / v)
            return CustomColor.fromHsv(hue, hsvSaturation, v).rgb
        }

        companion object {
            fun of(color: CustomColor): Hsl {
                val hsv = color.toHsv()
                val lightness = hsv.value * (1 - hsv.saturation / 2)
                val saturation = if (lightness == 0.0 || lightness == 1.0) {
                    0.0
                } else {
                    (hsv.value - lightness) / minOf(lightness, 1 - lightness)
                }
                return Hsl(hsv.hue, saturation, lightness)
            }
        }
    }
}
