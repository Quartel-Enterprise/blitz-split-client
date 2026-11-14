package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.Hsv
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomColorTest {
    @Test
    fun `GIVEN a color with lower channels WHEN reading its hex THEN it is uppercase and zero padded`() {
        // Given
        val colors = listOf(CustomColor(0x3a7bd5), CustomColor(0x0000FF), CustomColor(0x000000))

        // When
        val hexes = colors.map { it.hex }

        // Then
        assertEquals(listOf("#3A7BD5", "#0000FF", "#000000"), hexes)
    }

    @Test
    fun `GIVEN a color WHEN reading its channels THEN gives red green and blue`() {
        // Given
        val color = CustomColor(0x3A7BD5)

        // When
        val channels = Triple(color.red, color.green, color.blue)

        // Then
        assertEquals(Triple(58, 123, 213), channels)
    }

    @Test
    fun `GIVEN six hex digits with or without a hash in any case and spaces WHEN reading them THEN gives the color`() {
        // Given
        val typed = listOf("#3A7BD5", "3A7BD5", "#3a7bd5", "  #3a7Bd5  ", "3a7bd5 ")

        // When
        val colors = typed.map { CustomColor.fromHexOrNull(it) }

        // Then
        assertTrue(colors.all { it == CustomColor(0x3A7BD5) })
    }

    @Test
    fun `GIVEN three digits too many digits or non hex characters WHEN reading them THEN gives no color`() {
        // Given
        val typed = listOf("#FA0", "FA0", "#3A7BD5F", "3A7BD5FF", "#3A7BDZ", "#GGGGGG", "", "#", "##3A7BD5", "#3A 7BD")

        // When
        val colors = typed.map { CustomColor.fromHexOrNull(it) }

        // Then
        assertTrue(colors.all { it == null })
    }

    @Test
    fun `GIVEN no text WHEN reading it as hex THEN gives no color`() {
        // Given
        val typed: String? = null

        // When
        val color = CustomColor.fromHexOrNull(typed)

        // Then
        assertNull(color)
    }

    @Test
    fun `GIVEN channels from 0 to 255 WHEN reading them as RGB THEN gives the color`() {
        // Given
        val channels = listOf(Triple(58, 123, 213), Triple(0, 0, 0), Triple(255, 255, 255))

        // When
        val colors = channels.map { (r, g, b) -> CustomColor.fromRgbOrNull(r, g, b) }

        // Then
        assertEquals(listOf(CustomColor(0x3A7BD5), CustomColor(0x000000), CustomColor(0xFFFFFF)), colors)
    }

    @Test
    fun `GIVEN a channel out of 0 to 255 WHEN reading them as RGB THEN gives no color`() {
        // Given
        val channels =
            listOf(Triple(256, 0, 0), Triple(0, 256, 0), Triple(0, 0, 256), Triple(-1, 0, 0), Triple(0, 0, -1))

        // When
        val colors = channels.map { (r, g, b) -> CustomColor.fromRgbOrNull(r, g, b) }

        // Then
        assertTrue(colors.all { it == null })
    }

    @Test
    fun `GIVEN the primary hues at full strength WHEN reading them from the gradient THEN gives red green and blue`() {
        // Given
        val hues = listOf(0.0, 120.0, 240.0)

        // When
        val colors = hues.map { CustomColor.fromHsv(it, 1.0, 1.0) }

        // Then
        assertEquals(listOf(CustomColor(0xFF0000), CustomColor(0x00FF00), CustomColor(0x0000FF)), colors)
    }

    @Test
    fun `GIVEN a hue past 360 or below 0 WHEN reading it from the gradient THEN it turns around the circle`() {
        // Given
        val hues = listOf(360.0, 480.0, -120.0, 720.0)

        // When
        val colors = hues.map { CustomColor.fromHsv(it, 1.0, 1.0) }

        // Then
        assertEquals(
            listOf(CustomColor(0xFF0000), CustomColor(0x00FF00), CustomColor(0x0000FF), CustomColor(0xFF0000)),
            colors,
        )
    }

    @Test
    fun `GIVEN saturation and value out of 0 to 1 WHEN reading them from the gradient THEN they are clamped`() {
        // Given
        val oversaturated = Triple(0.0, 2.0, 5.0)
        val belowZero = Triple(0.0, -1.0, -1.0)

        // When
        val bright = CustomColor.fromHsv(oversaturated.first, oversaturated.second, oversaturated.third)
        val dark = CustomColor.fromHsv(belowZero.first, belowZero.second, belowZero.third)

        // Then
        assertEquals(CustomColor(0xFF0000), bright)
        assertEquals(CustomColor(0x000000), dark)
    }

    @Test
    fun `GIVEN the primaries WHEN placing them on the gradient THEN gives their hue at full saturation and value`() {
        // Given
        val primaries = listOf(CustomColor(0xFF0000), CustomColor(0x00FF00), CustomColor(0x0000FF))

        // When
        val hsvs = primaries.map { it.toHsv() }

        // Then
        assertEquals(listOf(Hsv(0.0, 1.0, 1.0), Hsv(120.0, 1.0, 1.0), Hsv(240.0, 1.0, 1.0)), hsvs)
    }

    @Test
    fun `GIVEN a gray WHEN placing it on the gradient THEN has no saturation and its lightness as value`() {
        // Given
        val gray = CustomColor(0x808080)

        // When
        val hsv = gray.toHsv()

        // Then
        assertEquals(0.0, hsv.saturation)
        assertClose(128 / 255.0, hsv.value)
    }

    @Test
    fun `GIVEN black WHEN placing it on the gradient THEN has no saturation and no value`() {
        // Given
        val black = CustomColor(0x000000)

        // When
        val hsv = black.toHsv()

        // Then
        assertEquals(0.0, hsv.saturation)
        assertEquals(0.0, hsv.value)
    }

    @Test
    fun `GIVEN a spread of colors WHEN placing them on the gradient and back THEN each is the same color`() {
        // Given
        val colors = listOf(
            0xFF0000,
            0x00FF00,
            0x0000FF,
            0x808080,
            0x000000,
            0xFFFFFF,
            0x3A7BD5,
            0xFFF59D,
            0x1A2340,
            0xFF00FF,
        ).map(::CustomColor)

        // When
        val roundTrips = colors.map {
            it.toHsv().let { hsv ->
                CustomColor.fromHsv(hsv.hue, hsv.saturation, hsv.value)
            }
        }

        // Then
        assertEquals(colors, roundTrips)
    }

    private fun assertClose(
        expected: Double,
        actual: Double,
    ) {
        assertTrue(abs(expected - actual) < 1e-9, "expected $expected, got $actual")
    }
}
