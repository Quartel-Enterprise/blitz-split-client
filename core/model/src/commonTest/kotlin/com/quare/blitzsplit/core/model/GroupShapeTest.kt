package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertNull

class GroupShapeTest {
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
