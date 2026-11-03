package com.quare.blitzsplit.core.activity.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ActivityFieldTest {
    @Test
    fun `GIVEN the shape and photo codes WHEN looking up their fields THEN finds the shape and the photo`() {
        // Given
        val codes = listOf("shape", "photo_url")

        // When
        val fields = codes.map(ActivityField::fromCodeOrNull)

        // Then
        assertEquals(listOf(ActivityField.SHAPE, ActivityField.PHOTO), fields)
    }
}
