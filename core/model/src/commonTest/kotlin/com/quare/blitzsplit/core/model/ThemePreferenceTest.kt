package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ThemePreferenceTest {
    @Test
    fun `GIVEN a stored code WHEN reading it THEN is its theme`() {
        // Given
        val code = "dark"

        // When
        val theme = ThemePreference.fromCodeOrNull(code)

        // Then
        assertEquals(ThemePreference.DARK, theme)
    }

    @Test
    fun `GIVEN a code this version does not know WHEN reading it THEN is null`() {
        // Given
        val code = "sepia"

        // When
        val theme = ThemePreference.fromCodeOrNull(code)

        // Then
        assertNull(theme)
    }
}
