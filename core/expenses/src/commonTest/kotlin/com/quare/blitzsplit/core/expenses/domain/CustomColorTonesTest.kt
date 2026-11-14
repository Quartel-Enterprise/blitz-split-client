package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTones
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Why: a custom color may be any color, so its tones are checked for a spread, the contrast computed here (WCAG 2).
class CustomColorTonesTest {
    private val spread = mapOf(
        "white" to 0xFFFFFF,
        "black" to 0x000000,
        "pure yellow" to 0xFFFF00,
        "pale yellow" to 0xFFF59D,
        "pure blue" to 0x0000FF,
        "mid gray" to 0x808080,
        "dark navy" to 0x1A2340,
        "pure red" to 0xFF0000,
        "lime green" to 0x00FF00,
    )

    @Test
    fun `GIVEN any base color WHEN deriving its tones THEN the light content reads at 7 to 1 on the light badge`() {
        // Given
        val colors = spread

        // When
        val tones = colors.mapValues { (_, rgb) -> CustomColor(rgb).toTones() }

        // Then
        tones.forEach { (name, it) -> assertAtLeast(7.0, it.lightContent, it.lightContainer, "$name light content") }
    }

    @Test
    fun `GIVEN any base color WHEN deriving its tones THEN the dark content reads at 7 to 1 on the dark badge`() {
        // Given
        val colors = spread

        // When
        val tones = colors.mapValues { (_, rgb) -> CustomColor(rgb).toTones() }

        // Then
        tones.forEach { (name, it) -> assertAtLeast(7.0, it.darkContent, it.darkContainer, "$name dark content") }
    }

    @Test
    fun `GIVEN any base color WHEN deriving its tones THEN the dark content is the light badge`() {
        // Given
        val colors = spread

        // When
        val tones = colors.values.map { CustomColor(it).toTones() }

        // Then
        tones.forEach { assertEquals(it.lightContainer, it.darkContent) }
    }

    @Test
    fun `GIVEN any base color WHEN deriving its tones THEN the light chart mark reads at 4_5 to 1 on a white card`() {
        // Given
        val colors = spread

        // When
        val tones = colors.mapValues { (_, rgb) -> CustomColor(rgb).toTones() }

        // Then
        tones.forEach { (name, it) -> assertAtLeast(4.5, it.lightData, WHITE, "$name light data") }
    }

    @Test
    fun `GIVEN any base color WHEN deriving its tones THEN the dark chart mark reads at 7 to 1 on the dark card`() {
        // Given
        val colors = spread

        // When
        val tones = colors.mapValues { (_, rgb) -> CustomColor(rgb).toTones() }

        // Then
        tones.forEach { (name, it) -> assertAtLeast(7.0, it.darkData, DARK_CARD, "$name dark data") }
    }

    @Test
    fun `GIVEN any base color WHEN deriving its tones THEN every tone is an RGB color`() {
        // Given
        val colors = spread

        // When
        val tones = colors.values.map { CustomColor(it).toTones() }

        // Then
        tones.flatMap(::allOf).forEach { assertTrue(it in 0..0xFFFFFF, "not an RGB color: $it") }
    }

    private fun allOf(tones: CategoryTones): List<Int> = listOf(
        tones.lightContainer,
        tones.lightContent,
        tones.darkContainer,
        tones.darkContent,
        tones.lightData,
        tones.darkData,
    )

    private fun assertAtLeast(
        minimum: Double,
        foreground: Int,
        background: Int,
        what: String,
    ) {
        val ratio = contrastOf(foreground, background)
        assertTrue(
            ratio >= minimum,
            "$what: ${foreground.toString(16)} on ${background.toString(16)} reads at $ratio, under $minimum",
        )
    }

    private fun contrastOf(
        first: Int,
        second: Int,
    ): Double {
        val a = luminanceOf(first)
        val b = luminanceOf(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    private fun luminanceOf(rgb: Int): Double {
        fun linear(channel: Int): Double {
            val srgb = channel / 255.0
            return if (srgb <= 0.03928) srgb / 12.92 else ((srgb + 0.055) / 1.055).pow(2.4)
        }
        val red = (rgb shr 16) and 0xFF
        val green = (rgb shr 8) and 0xFF
        val blue = rgb and 0xFF
        return 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue)
    }

    private companion object {
        const val WHITE = 0xFFFFFF
        const val DARK_CARD = 0x1D1B20
    }
}
