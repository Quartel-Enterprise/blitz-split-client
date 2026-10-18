package com.quare.blitzsplit.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecoveryCodeFormatTest {
    @Test
    fun `GIVEN dashes spaces and lower case WHEN normalizing THEN returns the 12 upper case characters`() {
        // Given
        val typed = "k7qm-4xpa 9tre"

        // When
        val normalized = RecoveryCodeFormat.normalize(typed)

        // Then
        assertEquals("K7QM4XPA9TRE", normalized)
    }

    @Test
    fun `GIVEN O I and L WHEN normalizing THEN reads them as the digits they look like`() {
        // Given
        val typed = "OOOO-IIII-LLLL"

        // When
        val normalized = RecoveryCodeFormat.normalize(typed)

        // Then
        assertEquals("000011111111", normalized)
    }

    @Test
    fun `GIVEN too few characters WHEN normalizing THEN returns null`() {
        // Given
        val typed = "K7QM-4XPA"

        // When
        val normalized = RecoveryCodeFormat.normalize(typed)

        // Then
        assertNull(normalized)
    }

    @Test
    fun `GIVEN a U WHEN normalizing THEN returns null since Crockford base32 leaves it out`() {
        // Given
        val typed = "UUUU-4XPA-9TRE"

        // When
        val normalized = RecoveryCodeFormat.normalize(typed)

        // Then
        assertNull(normalized)
    }

    @Test
    fun `GIVEN a normalized code WHEN formatting THEN groups it by four`() {
        // Given
        val code = "K7QM4XPA9TRE"

        // When
        val formatted = RecoveryCodeFormat.format(code)

        // Then
        assertEquals("K7QM-4XPA-9TRE", formatted)
    }
}
