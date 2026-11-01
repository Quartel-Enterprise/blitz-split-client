package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GroupsLayoutCodesTest {
    @Test
    fun `GIVEN a stored display code WHEN reading it THEN is its display`() {
        // Given
        val code = "grid"

        // When
        val display = GroupsDisplay.fromCodeOrNull(code)

        // Then
        assertEquals(GroupsDisplay.GRID, display)
    }

    @Test
    fun `GIVEN a display code this version does not know WHEN reading it THEN is null`() {
        // Given
        val code = "map"

        // When
        val display = GroupsDisplay.fromCodeOrNull(code)

        // Then
        assertNull(display)
    }

    @Test
    fun `GIVEN a stored grid size code WHEN reading it THEN is its size`() {
        // Given
        val code = "large"

        // When
        val size = GroupGridSize.fromCodeOrNull(code)

        // Then
        assertEquals(GroupGridSize.LARGE, size)
    }

    @Test
    fun `GIVEN a grid size code this version does not know WHEN reading it THEN is null`() {
        // Given
        val code = "huge"

        // When
        val size = GroupGridSize.fromCodeOrNull(code)

        // Then
        assertNull(size)
    }
}
