package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyError
import com.quare.blitzsplit.core.model.PaymentKeyFormat
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyValidation
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ValidatePaymentKeyUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

// The Pix rules of ValidatePaymentKeyUseCase: CPF, phone, email and random key, and a key saved twice.
class ValidatePixKeyUseCaseTest {
    private val useCase = ValidatePaymentKeyUseCase()

    @Test
    fun `GIVEN a CPF typed with its mask WHEN validating THEN stores its 11 digits`() {
        // Given
        val input = "123.456.789-09"

        // When
        val result = useCase(PaymentKeyType.CPF, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid("12345678909", PaymentKeyFormat.CPF), result)
    }

    @Test
    fun `GIVEN a CPF typed without its mask WHEN validating THEN is valid`() {
        // Given
        val input = "12345678909"

        // When
        val result = useCase(PaymentKeyType.CPF, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid("12345678909", PaymentKeyFormat.CPF), result)
    }

    @Test
    fun `GIVEN a CPF whose check digits do not match WHEN validating THEN is an invalid CPF`() {
        // Given
        val input = "123.456.789-00"

        // When
        val result = useCase(PaymentKeyType.CPF, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_CPF), result)
    }

    @Test
    fun `GIVEN a CPF of one repeated digit WHEN validating THEN is an invalid CPF`() {
        // Given
        val input = "111.111.111-11"

        // When
        val result = useCase(PaymentKeyType.CPF, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_CPF), result)
    }

    @Test
    fun `GIVEN a CPF missing a digit WHEN validating THEN is an invalid CPF`() {
        // Given
        val input = "123.456.789-0"

        // When
        val result = useCase(PaymentKeyType.CPF, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_CPF), result)
    }

    @Test
    fun `GIVEN a CPF with a letter in it WHEN validating THEN is an invalid CPF`() {
        // Given
        val input = "123.456.789-09a"

        // When
        val result = useCase(PaymentKeyType.CPF, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_CPF), result)
    }

    @Test
    fun `GIVEN a mobile number without the country WHEN validating THEN stores it with plus 55`() {
        // Given
        val input = "11912345678"

        // When
        val result = useCase(PaymentKeyType.PHONE, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid("+5511912345678", PaymentKeyFormat.PHONE), result)
    }

    @Test
    fun `GIVEN a mobile number written with plus 55 and spaces WHEN validating THEN stores its digits`() {
        // Given
        val input = "+55 11 91234-5678"

        // When
        val result = useCase(PaymentKeyType.PHONE, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid("+5511912345678", PaymentKeyFormat.PHONE), result)
    }

    @Test
    fun `GIVEN a mobile number with 55 in front and no plus WHEN validating THEN reads 55 as the country`() {
        // Given
        val input = "5511912345678"

        // When
        val result = useCase(PaymentKeyType.PHONE, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid("+5511912345678", PaymentKeyFormat.PHONE), result)
    }

    @Test
    fun `GIVEN a mobile number of DDD 55 without the country WHEN validating THEN reads 55 as the DDD`() {
        // Given
        val input = "55 91234-5678"

        // When
        val result = useCase(PaymentKeyType.PHONE, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid("+5555912345678", PaymentKeyFormat.PHONE), result)
    }

    @Test
    fun `GIVEN a mobile number of DDD 55 with plus 55 WHEN validating THEN is valid`() {
        // Given
        val input = "+55 (55) 91234-5678"

        // When
        val result = useCase(PaymentKeyType.PHONE, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid("+5555912345678", PaymentKeyFormat.PHONE), result)
    }

    @Test
    fun `GIVEN a DDD Anatel never assigned WHEN validating THEN is an invalid phone`() {
        // Given
        val input = "20912345678"

        // When
        val result = useCase(PaymentKeyType.PHONE, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_PHONE), result)
    }

    @Test
    fun `GIVEN a landline without the leading 9 WHEN validating THEN is an invalid phone`() {
        // Given
        val input = "1131234567"

        // When
        val result = useCase(PaymentKeyType.PHONE, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_PHONE), result)
    }

    @Test
    fun `GIVEN 11 digits whose third is not 9 WHEN validating THEN is an invalid phone`() {
        // Given
        val input = "11812345678"

        // When
        val result = useCase(PaymentKeyType.PHONE, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_PHONE), result)
    }

    @Test
    fun `GIVEN a mobile number missing a digit WHEN validating THEN is an invalid phone`() {
        // Given
        val input = "+55 11 91234-567"

        // When
        val result = useCase(PaymentKeyType.PHONE, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_PHONE), result)
    }

    @Test
    fun `GIVEN a Moscow number written with its country code 7 WHEN validating THEN is an invalid phone`() {
        // Given
        val input = "+7 495 123-45-67"

        // When
        val result = useCase(PaymentKeyType.PHONE, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_PHONE), result)
    }

    @Test
    fun `GIVEN an email in upper case WHEN validating THEN stores it in lower case`() {
        // Given
        val input = "Pierre.Vieira@Email.com"

        // When
        val result = useCase(PaymentKeyType.EMAIL, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid("pierre.vieira@email.com", PaymentKeyFormat.EMAIL), result)
    }

    @Test
    fun `GIVEN an email without a top level domain WHEN validating THEN is an invalid email`() {
        // Given
        val input = "pierre@email"

        // When
        val result = useCase(PaymentKeyType.EMAIL, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_EMAIL), result)
    }

    @Test
    fun `GIVEN an email without a name WHEN validating THEN is an invalid email`() {
        // Given
        val input = "@email.com"

        // When
        val result = useCase(PaymentKeyType.EMAIL, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_EMAIL), result)
    }

    @Test
    fun `GIVEN an email of 77 characters WHEN validating THEN is valid`() {
        // Given
        val input = "a".repeat(67) + "@email.com"

        // When
        val result = useCase(PaymentKeyType.EMAIL, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid(input, PaymentKeyFormat.EMAIL), result)
    }

    @Test
    fun `GIVEN an email of 78 characters WHEN validating THEN is an invalid email`() {
        // Given
        val input = "a".repeat(68) + "@email.com"

        // When
        val result = useCase(PaymentKeyType.EMAIL, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_EMAIL), result)
    }

    @Test
    fun `GIVEN a random key with dashes WHEN validating THEN is valid`() {
        // Given
        val input = RANDOM_KEY

        // When
        val result = useCase(PaymentKeyType.RANDOM, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid(RANDOM_KEY, PaymentKeyFormat.RANDOM), result)
    }

    @Test
    fun `GIVEN a random key in upper case without dashes WHEN validating THEN stores it lower case with dashes`() {
        // Given
        val input = "123E4567E89B12D3A456426614174000"

        // When
        val result = useCase(PaymentKeyType.RANDOM, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid(RANDOM_KEY, PaymentKeyFormat.RANDOM), result)
    }

    @Test
    fun `GIVEN part of a random key WHEN validating THEN is an invalid random key`() {
        // Given
        val input = "123e4567-e89b-12d3"

        // When
        val result = useCase(PaymentKeyType.RANDOM, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_RANDOM), result)
    }

    @Test
    fun `GIVEN a random key with a character out of hex WHEN validating THEN is an invalid random key`() {
        // Given
        val input = "123e4567-e89b-12d3-a456-42661417400g"

        // When
        val result = useCase(PaymentKeyType.RANDOM, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_RANDOM), result)
    }

    @Test
    fun `GIVEN the CPF of another saved key typed with its mask WHEN validating THEN is already saved`() {
        // Given
        val otherKeys = listOf(key(PaymentKeyType.CPF, "12345678909"))

        // When
        val result = useCase(PaymentKeyType.CPF, "123.456.789-09", otherKeys)

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.ALREADY_SAVED), result)
    }

    @Test
    fun `GIVEN the email of another saved key in upper case WHEN validating THEN is already saved`() {
        // Given
        val otherKeys = listOf(key(PaymentKeyType.EMAIL, "pierre@email.com"))

        // When
        val result = useCase(PaymentKeyType.EMAIL, "PIERRE@email.com", otherKeys)

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.ALREADY_SAVED), result)
    }

    @Test
    fun `GIVEN the phone of another saved key written another way WHEN validating THEN is already saved`() {
        // Given
        val otherKeys = listOf(key(PaymentKeyType.PHONE, "+5511912345678"))

        // When
        val result = useCase(PaymentKeyType.PHONE, "11912345678", otherKeys)

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.ALREADY_SAVED), result)
    }

    @Test
    fun `GIVEN the same text saved as a Lightning Address WHEN validating it as an email THEN is valid`() {
        // Given
        val otherKeys = listOf(key(PaymentKeyType.LIGHTNING, "pierre@email.com"))

        // When
        val result = useCase(PaymentKeyType.EMAIL, "pierre@email.com", otherKeys)

        // Then
        assertEquals(PaymentKeyValidation.Valid("pierre@email.com", PaymentKeyFormat.EMAIL), result)
    }

    @Test
    fun `GIVEN other saved keys with other values WHEN validating THEN is valid`() {
        // Given
        val otherKeys = listOf(key(PaymentKeyType.CPF, "52998224725"))

        // When
        val result = useCase(PaymentKeyType.CPF, "123.456.789-09", otherKeys)

        // Then
        assertEquals(PaymentKeyValidation.Valid("12345678909", PaymentKeyFormat.CPF), result)
    }

    @Test
    fun `GIVEN an invalid CPF equal to a saved value WHEN validating THEN reports the invalid CPF`() {
        // Given
        val otherKeys = listOf(key(PaymentKeyType.CPF, "12345678900"))

        // When
        val result = useCase(PaymentKeyType.CPF, "12345678900", otherKeys)

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_CPF), result)
    }

    private fun key(
        type: PaymentKeyType,
        value: String,
    ): PaymentKey = PaymentKey(
        id = "key-1",
        type = type,
        value = value,
        isDefault = true,
        addedAt = 1L,
    )

    private companion object {
        const val RANDOM_KEY = "123e4567-e89b-12d3-a456-426614174000"
    }
}
