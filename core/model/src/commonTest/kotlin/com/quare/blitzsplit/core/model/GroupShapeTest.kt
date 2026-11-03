package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GroupShapeTest {
    @Test
    fun `GIVEN the last shape WHEN taking the next THEN wraps around to the first`() {
        // Given
        val shape = GroupShape.entries.last()

        // When
        val next = shape.toNext()

        // Then
        assertEquals(GroupShape.entries.first(), next)
    }

    @Test
    fun `GIVEN a code no version knows WHEN reading it THEN there is no shape`() {
        // Given
        val code = "hexagon"

        // When
        val shape = GroupShape.fromCodeOrNull(code)

        // Then
        assertNull(shape)
    }
}
