package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.CardTints
import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.CustomColorTones
import com.quare.blitzsplit.core.expenses.domain.model.tint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CardTintsTest {
    private val blue = CustomColor(0x3A7BD5)
    private val paleYellow = CustomColor(0xFFFF99)

    @Test
    fun `GIVEN a named category color key WHEN reading a card color THEN gives that named color`() {
        // Given
        val code = "teal"

        // When
        val tint = CardTints.fromCodeOrNull(code)

        // Then
        assertEquals(CategoryColor.TEAL.tint, tint)
    }

    @Test
    fun `GIVEN an uppercase hex code WHEN reading a card color THEN gives the custom color`() {
        // Given
        val code = "#3A7BD5"

        // When
        val tint = CardTints.fromCodeOrNull(code)

        // Then
        assertEquals(blue.tint, tint)
    }

    @Test
    fun `GIVEN a lowercase hex code WHEN reading a card color THEN gives the custom color`() {
        // Given
        val code = "#3a7bd5"

        // When
        val tint = CardTints.fromCodeOrNull(code)

        // Then
        assertEquals(blue.tint, tint)
    }

    @Test
    fun `GIVEN a hex code without the hash WHEN reading a card color THEN gives none`() {
        // Given
        val code = "3A7BD5"

        // When
        val tint = CardTints.fromCodeOrNull(code)

        // Then
        assertNull(tint)
    }

    @Test
    fun `GIVEN the first card keys that are named colors WHEN reading them THEN each gives its namesake`() {
        // Given
        val codes = listOf("purple", "blue", "green", "orange", "red", "pink")

        // When
        val tints = codes.map { CardTints.fromCodeOrNull(it) }

        // Then
        assertEquals(
            listOf(
                CategoryColor.PURPLE.tint,
                CategoryColor.BLUE.tint,
                CategoryColor.GREEN.tint,
                CategoryColor.ORANGE.tint,
                CategoryColor.RED.tint,
                CategoryColor.PINK.tint,
            ),
            tints,
        )
    }

    @Test
    fun `GIVEN a card saved in gold WHEN reading its color THEN gives yellow`() {
        // Given
        val code = "gold"

        // When
        val tint = CardTints.fromCodeOrNull(code)

        // Then
        assertEquals(CategoryColor.YELLOW.tint, tint)
    }

    @Test
    fun `GIVEN a card saved in graphite WHEN reading its color THEN gives gray`() {
        // Given
        val code = "graphite"

        // When
        val tint = CardTints.fromCodeOrNull(code)

        // Then
        assertEquals(CategoryColor.GRAY.tint, tint)
    }

    @Test
    fun `GIVEN a key this version does not know WHEN reading a card color THEN gives none`() {
        // Given
        val code = "chartreuse"

        // When
        val tint = CardTints.fromCodeOrNull(code)

        // Then
        assertNull(tint)
    }

    @Test
    fun `GIVEN an empty key WHEN reading a card color THEN gives none`() {
        // Given
        val code = ""

        // When
        val tint = CardTints.fromCodeOrNull(code)

        // Then
        assertNull(tint)
    }

    @Test
    fun `GIVEN no key WHEN reading a card color THEN gives none`() {
        // Given
        val code: String? = null

        // When
        val tint = CardTints.fromCodeOrNull(code)

        // Then
        assertNull(tint)
    }

    @Test
    fun `GIVEN a new card WHEN reading the default color THEN it is purple`() {
        // Given
        val purple = CategoryColor.PURPLE.tint

        // When
        val default = CardTints.default

        // Then
        assertEquals(purple, default)
    }

    @Test
    fun `GIVEN a named color WHEN drawing a card tile THEN fills it with the color base`() {
        // Given
        val tint = CategoryColor.TEAL.tint

        // When
        val fill = CardTints.fillRgbOf(tint)

        // Then
        assertEquals(CategoryColor.TEAL.baseRgb, fill)
    }

    @Test
    fun `GIVEN a custom color WHEN drawing a card tile THEN fills it with the color light data tone`() {
        // Given
        val tint = blue.tint

        // When
        val fill = CardTints.fillRgbOf(tint)

        // Then
        assertEquals(blue.toTones().lightData, fill)
    }

    @Test
    fun `GIVEN a card with no color WHEN drawing its tile THEN fills it with purple`() {
        // Given
        val tint = null

        // When
        val fill = CardTints.fillRgbOf(tint)

        // Then
        assertEquals(CategoryColor.PURPLE.baseRgb, fill)
    }

    @Test
    fun `GIVEN a very light custom color WHEN drawing a card tile THEN the fill is darker and white reads on it`() {
        // Given
        val tint = paleYellow.tint

        // When
        val fill = CardTints.fillRgbOf(tint)

        // Then
        assertNotEquals(paleYellow.rgb, fill)
        assertTrue(CustomColorTones.getContrast(fill, WHITE) > CustomColorTones.getContrast(paleYellow.rgb, WHITE))
        assertTrue(CustomColorTones.getContrast(fill, WHITE) >= TEXT_CONTRAST)
    }

    private companion object {
        const val WHITE = 0xFFFFFF
        const val TEXT_CONTRAST = 4.5
    }
}
