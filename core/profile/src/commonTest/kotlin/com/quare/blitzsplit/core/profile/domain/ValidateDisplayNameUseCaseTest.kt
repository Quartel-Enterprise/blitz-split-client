package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.profile.domain.model.DisplayNameValidation
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ValidateDisplayNameUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidateDisplayNameUseCaseTest {
    private val useCase = ValidateDisplayNameUseCase()

    @Test
    fun `GIVEN one letter WHEN validating THEN is too short`() {
        // Given
        val name = "P"

        // When
        val result = useCase(name)

        // Then
        assertEquals(DisplayNameValidation.TOO_SHORT, result)
    }

    @Test
    fun `GIVEN two letters surrounded by spaces WHEN validating THEN trims before counting`() {
        // Given
        val name = "  Pi  "

        // When
        val result = useCase(name)

        // Then
        assertEquals(DisplayNameValidation.VALID, result)
    }

    @Test
    fun `GIVEN only spaces WHEN validating THEN is too short`() {
        // Given
        val name = "     "

        // When
        val result = useCase(name)

        // Then
        assertEquals(DisplayNameValidation.TOO_SHORT, result)
    }

    @Test
    fun `GIVEN 41 letters WHEN validating THEN is too long`() {
        // Given
        val name = "a".repeat(41)

        // When
        val result = useCase(name)

        // Then
        assertEquals(DisplayNameValidation.TOO_LONG, result)
    }

    @Test
    fun `GIVEN 40 letters WHEN validating THEN is valid`() {
        // Given
        val name = "a".repeat(40)

        // When
        val result = useCase(name)

        // Then
        assertEquals(DisplayNameValidation.VALID, result)
    }
}
