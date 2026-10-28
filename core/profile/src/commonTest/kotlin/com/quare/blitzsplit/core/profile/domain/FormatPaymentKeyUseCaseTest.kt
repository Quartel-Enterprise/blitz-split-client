package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.domain.usecase.impl.FormatPaymentKeyUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatPaymentKeyUseCaseTest {
    private val useCase = FormatPaymentKeyUseCase()

    @Test
    fun `GIVEN a stored CPF WHEN formatting THEN shows its mask`() {
        // Given
        val key = key(PaymentKeyType.CPF, "12345678909")

        // When
        val shown = useCase(key)

        // Then
        assertEquals("123.456.789-09", shown)
    }

    @Test
    fun `GIVEN a stored phone WHEN formatting THEN shows the country the DDD and the dash`() {
        // Given
        val key = key(PaymentKeyType.PHONE, "+5511912345678")

        // When
        val shown = useCase(key)

        // Then
        assertEquals("+55 (11) 91234-5678", shown)
    }

    @Test
    fun `GIVEN a stored phone of DDD 55 WHEN formatting THEN shows 55 as the DDD`() {
        // Given
        val key = key(PaymentKeyType.PHONE, "+5555912345678")

        // When
        val shown = useCase(key)

        // Then
        assertEquals("+55 (55) 91234-5678", shown)
    }

    @Test
    fun `GIVEN a stored email WHEN formatting THEN shows it unchanged`() {
        // Given
        val key = key(PaymentKeyType.EMAIL, "pierre@email.com")

        // When
        val shown = useCase(key)

        // Then
        assertEquals("pierre@email.com", shown)
    }

    @Test
    fun `GIVEN a stored random key WHEN formatting THEN shows it unchanged`() {
        // Given
        val key = key(PaymentKeyType.RANDOM, "123e4567-e89b-12d3-a456-426614174000")

        // When
        val shown = useCase(key)

        // Then
        assertEquals("123e4567-e89b-12d3-a456-426614174000", shown)
    }

    @Test
    fun `GIVEN a stored Lightning Address WHEN formatting THEN shows it unchanged`() {
        // Given
        val key = key(PaymentKeyType.LIGHTNING, "pierre@getalby.com")

        // When
        val shown = useCase(key)

        // Then
        assertEquals("pierre@getalby.com", shown)
    }

    @Test
    fun `GIVEN a stored address WHEN formatting THEN shows it unchanged`() {
        // Given
        val key = key(PaymentKeyType.ONCHAIN, "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")

        // When
        val shown = useCase(key)

        // Then
        assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", shown)
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
}
