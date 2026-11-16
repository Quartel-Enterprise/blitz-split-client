package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.SplitBarColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SplitBarColorsTest {
    @Test
    fun `GIVEN more people than colors WHEN picking by place THEN neighbors differ and the palette starts over`() {
        // Given
        val places = 0 until SplitBarColors.palette.size * 2 + 1

        // When
        val colors = places.map(SplitBarColors::getAt)

        // Then
        assertTrue(colors.zipWithNext().none { (left, right) -> left == right })
        assertEquals(SplitBarColors.getAt(0), SplitBarColors.getAt(SplitBarColors.palette.size))
    }

    @Test
    fun `GIVEN the palette WHEN reading it THEN it leaves out the hues near the money green and the debt orange`() {
        // Given
        val nearStatus = listOf(CategoryColor.GREEN, CategoryColor.TEAL, CategoryColor.ORANGE, CategoryColor.BROWN)

        // When
        val palette = SplitBarColors.palette

        // Then
        assertFalse(palette.any { it in nearStatus })
        assertEquals(palette.size, palette.distinct().size)
    }
}
