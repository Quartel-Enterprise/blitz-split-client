package com.quare.blitzsplit.feature.profile.presentation.mapper

import kotlin.test.Test
import kotlin.test.assertEquals

class EmailMaskTest {
    @Test
    fun `GIVEN a Gmail address WHEN masking it THEN keeps the first letter and the domain`() {
        // Given
        val email = "pierre@gmail.com"

        // When
        val masked = email.toMaskedEmail()

        // Then
        assertEquals("p•••@gmail.com", masked)
    }

    @Test
    fun `GIVEN a one letter name WHEN masking it THEN still hides it behind the dots`() {
        // Given
        val email = "p@gmail.com"

        // When
        val masked = email.toMaskedEmail()

        // Then
        assertEquals("p•••@gmail.com", masked)
    }

    @Test
    fun `GIVEN an address without an at sign WHEN masking it THEN keeps only its first character`() {
        // Given
        val email = "pierre"

        // When
        val masked = email.toMaskedEmail()

        // Then
        assertEquals("p•••", masked)
    }

    @Test
    fun `GIVEN an address starting with the at sign WHEN masking it THEN keeps only its first character`() {
        // Given
        val email = "@gmail.com"

        // When
        val masked = email.toMaskedEmail()

        // Then
        assertEquals("@•••", masked)
    }
}
