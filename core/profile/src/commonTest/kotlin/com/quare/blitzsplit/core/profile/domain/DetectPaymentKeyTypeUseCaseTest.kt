package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.profile.domain.usecase.impl.DetectPaymentKeyTypeUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DetectPaymentKeyTypeUseCaseTest {
    private val useCase = DetectPaymentKeyTypeUseCase()

    @Test
    fun `GIVEN text with an at sign WHEN detecting a Pix key THEN is an email`() {
        // Given
        val input = "pierre@email.com"

        // When
        val type = useCase(PaymentMethod.PIX, input)

        // Then
        assertEquals(PaymentKeyType.EMAIL, type)
    }

    @Test
    fun `GIVEN a full random key WHEN detecting a Pix key THEN is a random key`() {
        // Given
        val input = "123e4567-e89b-12d3-a456-426614174000"

        // When
        val type = useCase(PaymentMethod.PIX, input)

        // Then
        assertEquals(PaymentKeyType.RANDOM, type)
    }

    @Test
    fun `GIVEN a random key in upper case without dashes WHEN detecting a Pix key THEN is a random key`() {
        // Given
        val input = "123E4567E89B12D3A456426614174000"

        // When
        val type = useCase(PaymentMethod.PIX, input)

        // Then
        assertEquals(PaymentKeyType.RANDOM, type)
    }

    @Test
    fun `GIVEN part of a random key WHEN detecting a Pix key THEN keeps the picked type`() {
        // Given
        val input = "123e4567-e89b"

        // When
        val type = useCase(PaymentMethod.PIX, input)

        // Then
        assertNull(type)
    }

    @Test
    fun `GIVEN a number starting with plus WHEN detecting a Pix key THEN is a phone`() {
        // Given
        val input = "+55 11 91234-5678"

        // When
        val type = useCase(PaymentMethod.PIX, input)

        // Then
        assertEquals(PaymentKeyType.PHONE, type)
    }

    @Test
    fun `GIVEN a valid CPF with its mask WHEN detecting a Pix key THEN is a CPF`() {
        // Given
        val input = "123.456.789-09"

        // When
        val type = useCase(PaymentMethod.PIX, input)

        // Then
        assertEquals(PaymentKeyType.CPF, type)
    }

    @Test
    fun `GIVEN a valid CPF without its mask WHEN detecting a Pix key THEN is a CPF`() {
        // Given
        val input = "12345678909"

        // When
        val type = useCase(PaymentMethod.PIX, input)

        // Then
        assertEquals(PaymentKeyType.CPF, type)
    }

    @Test
    fun `GIVEN 11 digits that are not a valid CPF WHEN detecting a Pix key THEN keeps the picked type`() {
        // Given
        val input = "12345678900"

        // When
        val type = useCase(PaymentMethod.PIX, input)

        // Then
        assertNull(type)
    }

    @Test
    fun `GIVEN 13 digits starting with 55 WHEN detecting a Pix key THEN is a phone`() {
        // Given
        val input = "5511912345678"

        // When
        val type = useCase(PaymentMethod.PIX, input)

        // Then
        assertEquals(PaymentKeyType.PHONE, type)
    }

    @Test
    fun `GIVEN 13 digits not starting with 55 WHEN detecting a Pix key THEN keeps the picked type`() {
        // Given
        val input = "4411912345678"

        // When
        val type = useCase(PaymentMethod.PIX, input)

        // Then
        assertNull(type)
    }

    @Test
    fun `GIVEN a few digits WHEN detecting a Pix key THEN keeps the picked type`() {
        // Given
        val input = "123"

        // When
        val type = useCase(PaymentMethod.PIX, input)

        // Then
        assertNull(type)
    }

    @Test
    fun `GIVEN a word WHEN detecting a Pix key THEN keeps the picked type`() {
        // Given
        val input = "pierre"

        // When
        val type = useCase(PaymentMethod.PIX, input)

        // Then
        assertNull(type)
    }

    @Test
    fun `GIVEN an empty field WHEN detecting a Pix key THEN keeps the picked type`() {
        // Given
        val input = ""

        // When
        val type = useCase(PaymentMethod.PIX, input)

        // Then
        assertNull(type)
    }

    @Test
    fun `GIVEN a bc1 address WHEN detecting a Bitcoin key THEN is on-chain`() {
        // Given
        val input = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"

        // When
        val type = useCase(PaymentMethod.BITCOIN, input)

        // Then
        assertEquals(PaymentKeyType.ONCHAIN, type)
    }

    @Test
    fun `GIVEN a bc1 address in upper case WHEN detecting a Bitcoin key THEN is on-chain`() {
        // Given
        val input = "BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4"

        // When
        val type = useCase(PaymentMethod.BITCOIN, input)

        // Then
        assertEquals(PaymentKeyType.ONCHAIN, type)
    }

    @Test
    fun `GIVEN a tb1 address WHEN detecting a Bitcoin key THEN is on-chain`() {
        // Given
        val input = "tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx"

        // When
        val type = useCase(PaymentMethod.BITCOIN, input)

        // Then
        assertEquals(PaymentKeyType.ONCHAIN, type)
    }

    @Test
    fun `GIVEN a bcrt1 address WHEN detecting a Bitcoin key THEN is on-chain`() {
        // Given
        val input = "bcrt1qw508d6qejxtdg4y5r3zarvary0c5xw7kygt080"

        // When
        val type = useCase(PaymentMethod.BITCOIN, input)

        // Then
        assertEquals(PaymentKeyType.ONCHAIN, type)
    }

    @Test
    fun `GIVEN a bitcoin URI WHEN detecting a Bitcoin key THEN is on-chain`() {
        // Given
        val input = "bitcoin:bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4?amount=0.001"

        // When
        val type = useCase(PaymentMethod.BITCOIN, input)

        // Then
        assertEquals(PaymentKeyType.ONCHAIN, type)
    }

    @Test
    fun `GIVEN a Lightning Address WHEN detecting a Bitcoin key THEN is Lightning`() {
        // Given
        val input = "pierre@getalby.com"

        // When
        val type = useCase(PaymentMethod.BITCOIN, input)

        // Then
        assertEquals(PaymentKeyType.LIGHTNING, type)
    }

    @Test
    fun `GIVEN an LNURL WHEN detecting a Bitcoin key THEN is Lightning`() {
        // Given
        val input =
            "LNURL1DP68GURN8GHJ7UM9WFMXJCM99E3K7MF0V9CXJ0M385EKVCENXC6R2C35XVUKXEFC" +
                "V5MKVV34X5EKZD3EV56NYD3HXQURZEPEXEJXXEPNXSCRVWFNV9NXZCN9XQ6XYEFHVGCXXCMYXYMNSERXFQ5FNS"

        // When
        val type = useCase(PaymentMethod.BITCOIN, input)

        // Then
        assertEquals(PaymentKeyType.LIGHTNING, type)
    }

    @Test
    fun `GIVEN an invoice WHEN detecting a Bitcoin key THEN is Lightning`() {
        // Given
        val input = "lnbc2500u1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypq"

        // When
        val type = useCase(PaymentMethod.BITCOIN, input)

        // Then
        assertEquals(PaymentKeyType.LIGHTNING, type)
    }

    @Test
    fun `GIVEN the lightning prefix WHEN detecting a Bitcoin key THEN is Lightning`() {
        // Given
        val input = "lightning:pierre"

        // When
        val type = useCase(PaymentMethod.BITCOIN, input)

        // Then
        assertEquals(PaymentKeyType.LIGHTNING, type)
    }

    @Test
    fun `GIVEN a legacy address WHEN detecting a Bitcoin key THEN keeps the picked type`() {
        // Given
        val input = "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2"

        // When
        val type = useCase(PaymentMethod.BITCOIN, input)

        // Then
        assertNull(type)
    }

    @Test
    fun `GIVEN a word WHEN detecting a Bitcoin key THEN keeps the picked type`() {
        // Given
        val input = "pierre"

        // When
        val type = useCase(PaymentMethod.BITCOIN, input)

        // Then
        assertNull(type)
    }
}
