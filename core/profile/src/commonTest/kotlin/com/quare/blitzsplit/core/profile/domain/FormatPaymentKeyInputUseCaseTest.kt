package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyInput
import com.quare.blitzsplit.core.profile.domain.usecase.impl.FormatPaymentKeyInputUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatPaymentKeyInputUseCaseTest {
    private val useCase = FormatPaymentKeyInputUseCase()

    @Test
    fun `GIVEN six CPF digits WHEN masking a CPF THEN shows the mask so far`() {
        // Given
        val input = "123456"

        // When
        val masked = useCase(PaymentKeyType.CPF, input)

        // Then
        assertEquals(PaymentKeyInput("123.456", isComplete = false), masked)
    }

    @Test
    fun `GIVEN a masked phone WHEN masking it as a CPF THEN keeps the typed digits without the country code`() {
        // Given
        val input = "+55 (11) 91234-5678"

        // When
        val masked = useCase(PaymentKeyType.CPF, input)

        // Then
        assertEquals(PaymentKeyInput("119.123.456-78", isComplete = true), masked)
    }

    @Test
    fun `GIVEN nine CPF digits WHEN masking a CPF THEN adds the dash only with the tenth`() {
        // Given
        val input = "123456789"

        // When
        val masked = useCase(PaymentKeyType.CPF, input)

        // Then
        assertEquals(PaymentKeyInput("123.456.789", isComplete = false), masked)
    }

    @Test
    fun `GIVEN every CPF digit WHEN masking a CPF THEN shows the whole mask and is complete`() {
        // Given
        val input = "12345678909"

        // When
        val masked = useCase(PaymentKeyType.CPF, input)

        // Then
        assertEquals(PaymentKeyInput("123.456.789-09", isComplete = true), masked)
    }

    @Test
    fun `GIVEN a masked CPF typed again WHEN masking a CPF THEN keeps the same mask`() {
        // Given
        val input = "123.456.78"

        // When
        val masked = useCase(PaymentKeyType.CPF, input)

        // Then
        assertEquals(PaymentKeyInput("123.456.78", isComplete = false), masked)
    }

    @Test
    fun `GIVEN a CPF with one digit too many WHEN masking a CPF THEN keeps the first 11`() {
        // Given
        val input = "123.456.789-091"

        // When
        val masked = useCase(PaymentKeyType.CPF, input)

        // Then
        assertEquals(PaymentKeyInput("123.456.789-09", isComplete = true), masked)
    }

    @Test
    fun `GIVEN an empty field WHEN masking a CPF THEN shows nothing`() {
        // Given
        val input = ""

        // When
        val masked = useCase(PaymentKeyType.CPF, input)

        // Then
        assertEquals(PaymentKeyInput("", isComplete = false), masked)
    }

    @Test
    fun `GIVEN the DDD and the first digit WHEN masking a phone THEN shows the country and the DDD`() {
        // Given
        val input = "119"

        // When
        val masked = useCase(PaymentKeyType.PHONE, input)

        // Then
        assertEquals(PaymentKeyInput("+55 (11) 9", isComplete = false), masked)
    }

    @Test
    fun `GIVEN every phone digit WHEN masking a phone THEN shows the whole mask and is complete`() {
        // Given
        val input = "11912345678"

        // When
        val masked = useCase(PaymentKeyType.PHONE, input)

        // Then
        assertEquals(PaymentKeyInput("+55 (11) 91234-5678", isComplete = true), masked)
    }

    @Test
    fun `GIVEN a masked phone typed again WHEN masking a phone THEN keeps the same mask`() {
        // Given
        val input = "+55 (11) 9123"

        // When
        val masked = useCase(PaymentKeyType.PHONE, input)

        // Then
        assertEquals(PaymentKeyInput("+55 (11) 9123", isComplete = false), masked)
    }

    @Test
    fun `GIVEN a full number pasted with 55 in front WHEN masking a phone THEN drops the country`() {
        // Given
        val input = "5511912345678"

        // When
        val masked = useCase(PaymentKeyType.PHONE, input)

        // Then
        assertEquals(PaymentKeyInput("+55 (11) 91234-5678", isComplete = true), masked)
    }

    @Test
    fun `GIVEN a number of DDD 55 WHEN masking a phone THEN keeps 55 as the DDD`() {
        // Given
        val input = "55912345678"

        // When
        val masked = useCase(PaymentKeyType.PHONE, input)

        // Then
        assertEquals(PaymentKeyInput("+55 (55) 91234-5678", isComplete = true), masked)
    }

    @Test
    fun `GIVEN a masked number of DDD 55 typed again WHEN masking a phone THEN keeps 55 as the DDD`() {
        // Given
        val input = "+55 (55) 9"

        // When
        val masked = useCase(PaymentKeyType.PHONE, input)

        // Then
        assertEquals(PaymentKeyInput("+55 (55) 9", isComplete = false), masked)
    }

    @Test
    fun `GIVEN an empty field WHEN masking a phone THEN shows nothing`() {
        // Given
        val input = ""

        // When
        val masked = useCase(PaymentKeyType.PHONE, input)

        // Then
        assertEquals(PaymentKeyInput("", isComplete = false), masked)
    }

    @Test
    fun `GIVEN 32 characters of a random key WHEN masking a random key THEN is complete`() {
        // Given
        val input = "123e4567e89b12d3a456426614174000"

        // When
        val masked = useCase(PaymentKeyType.RANDOM, input)

        // Then
        assertEquals(PaymentKeyInput(input, isComplete = true), masked)
    }

    @Test
    fun `GIVEN a random key with dashes WHEN masking a random key THEN keeps it and is complete`() {
        // Given
        val input = "123e4567-e89b-12d3-a456-426614174000"

        // When
        val masked = useCase(PaymentKeyType.RANDOM, input)

        // Then
        assertEquals(PaymentKeyInput(input, isComplete = true), masked)
    }

    @Test
    fun `GIVEN 31 characters of a random key WHEN masking a random key THEN is not complete`() {
        // Given
        val input = "123e4567e89b12d3a45642661417400"

        // When
        val masked = useCase(PaymentKeyType.RANDOM, input)

        // Then
        assertEquals(PaymentKeyInput(input, isComplete = false), masked)
    }

    @Test
    fun `GIVEN an email WHEN masking an email THEN returns it unchanged and not complete`() {
        // Given
        val input = "Pierre@Email.com"

        // When
        val masked = useCase(PaymentKeyType.EMAIL, input)

        // Then
        assertEquals(PaymentKeyInput(input, isComplete = false), masked)
    }

    @Test
    fun `GIVEN a Lightning Address WHEN masking Lightning THEN returns it unchanged and not complete`() {
        // Given
        val input = "Pierre@GetAlby.com"

        // When
        val masked = useCase(PaymentKeyType.LIGHTNING, input)

        // Then
        assertEquals(PaymentKeyInput(input, isComplete = false), masked)
    }

    @Test
    fun `GIVEN an address WHEN masking on-chain THEN returns it unchanged and not complete`() {
        // Given
        val input = "BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4"

        // When
        val masked = useCase(PaymentKeyType.ONCHAIN, input)

        // Then
        assertEquals(PaymentKeyInput(input, isComplete = false), masked)
    }
}
