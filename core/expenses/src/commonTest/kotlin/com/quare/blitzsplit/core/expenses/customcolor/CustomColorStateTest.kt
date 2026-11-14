package com.quare.blitzsplit.core.expenses.customcolor

import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.CustomPalette
import com.quare.blitzsplit.core.expenses.domain.model.tint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomColorStateTest {
    private val blue = CustomColor(0x3A7BD5)
    private val yellow = CustomColor(0xFFF59D)
    private val navy = CustomColor(0x1A2340)

    @Test
    fun `GIVEN a palette one short of full WHEN asking for plus THEN it is offered`() {
        // Given
        val state = stateWith(paletteOf(CustomPalette.MAX_COLORS - 1))

        // When
        val canAdd = state.canAdd

        // Then
        assertTrue(canAdd)
    }

    @Test
    fun `GIVEN a full palette WHEN asking for plus THEN it is not offered`() {
        // Given
        val state = stateWith(paletteOf(CustomPalette.MAX_COLORS))

        // When
        val canAdd = state.canAdd

        // Then
        assertFalse(canAdd)
    }

    @Test
    fun `GIVEN a palette past full from two devices WHEN asking for plus THEN it is not offered`() {
        // Given
        val state = stateWith(paletteOf(CustomPalette.MAX_COLORS + 1))

        // When
        val canAdd = state.canAdd

        // Then
        assertFalse(canAdd)
    }

    @Test
    fun `GIVEN another member color on a group category WHEN listing the swatches THEN it follows the palette`() {
        // Given
        val state = stateWith(listOf(blue, yellow))

        // When
        val swatches = state.customSwatchesFor(navy.tint)

        // Then
        assertEquals(listOf(blue, yellow, navy), swatches)
    }

    @Test
    fun `GIVEN a selected color already in the palette WHEN listing the swatches THEN it is not repeated`() {
        // Given
        val state = stateWith(listOf(blue, yellow))

        // When
        val swatches = state.customSwatchesFor(yellow.tint)

        // Then
        assertEquals(listOf(blue, yellow), swatches)
    }

    @Test
    fun `GIVEN a named color or none selected WHEN listing the swatches THEN only the palette shows`() {
        // Given
        val state = stateWith(listOf(blue))

        // When
        val named = state.customSwatchesFor(CategoryColor.TEAL.tint)
        val none = state.customSwatchesFor(null)

        // Then
        assertEquals(listOf(blue), named)
        assertEquals(listOf(blue), none)
    }

    @Test
    fun `GIVEN a color of the palette WHEN asking whether it can be deleted THEN it can`() {
        // Given
        val state = stateWith(listOf(blue))

        // When
        val deletable = state.isDeletable(blue)

        // Then
        assertTrue(deletable)
    }

    @Test
    fun `GIVEN another member color not in the palette WHEN asking whether it can be deleted THEN it cannot`() {
        // Given
        val state = stateWith(listOf(blue))

        // When
        val deletable = state.isDeletable(navy)

        // Then
        assertFalse(deletable)
    }

    private fun paletteOf(size: Int): List<CustomColor> = List(size) { CustomColor(it + 1) }

    private fun stateWith(palette: List<CustomColor>): CustomColorState = CustomColorState.idle.copy(palette = palette)
}
