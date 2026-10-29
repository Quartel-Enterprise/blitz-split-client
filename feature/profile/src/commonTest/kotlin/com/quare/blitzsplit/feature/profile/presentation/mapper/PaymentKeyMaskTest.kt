package com.quare.blitzsplit.feature.profile.presentation.mapper

import com.quare.blitzsplit.core.model.PaymentKeyFormat
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentKeyMaskTest {
    @Test
    fun `GIVEN a CPF WHEN masking it THEN shows only its middle digits`() {
        // Given
        val cpf = "12345678909"

        // When
        val masked = cpf.toMaskedPaymentKey(PaymentKeyFormat.CPF)

        // Then
        assertEquals("•••.456.789-••", masked)
    }

    @Test
    fun `GIVEN a phone WHEN masking it THEN keeps the country the DDD the leading 9 and the last four digits`() {
        // Given
        val phone = "+5511912345678"

        // When
        val masked = phone.toMaskedPaymentKey(PaymentKeyFormat.PHONE)

        // Then
        assertEquals("+55 (11) 9••••-5678", masked)
    }

    @Test
    fun `GIVEN an email with a long name WHEN masking it THEN keeps the first three characters and the domain`() {
        // Given
        val email = "pierre@email.com"

        // When
        val masked = email.toMaskedPaymentKey(PaymentKeyFormat.EMAIL)

        // Then
        assertEquals("pie•••@email.com", masked)
    }

    @Test
    fun `GIVEN an email with a four character name WHEN masking it THEN keeps the first three characters`() {
        // Given
        val email = "pier@email.com"

        // When
        val masked = email.toMaskedPaymentKey(PaymentKeyFormat.EMAIL)

        // Then
        assertEquals("pie•••@email.com", masked)
    }

    @Test
    fun `GIVEN an email with a three character name WHEN masking it THEN keeps only the first character`() {
        // Given
        val email = "ana@email.com"

        // When
        val masked = email.toMaskedPaymentKey(PaymentKeyFormat.EMAIL)

        // Then
        assertEquals("a•••@email.com", masked)
    }

    @Test
    fun `GIVEN an email with a one character name WHEN masking it THEN still hides it behind the dots`() {
        // Given
        val email = "p@email.com"

        // When
        val masked = email.toMaskedPaymentKey(PaymentKeyFormat.EMAIL)

        // Then
        assertEquals("p•••@email.com", masked)
    }

    @Test
    fun `GIVEN a random key WHEN masking it THEN keeps its first 8 and last 6 characters`() {
        // Given
        val randomKey = "7d9f3c2a-1b4e-4c8d-9a6f-2e5b7c4d1a37"

        // When
        val masked = randomKey.toMaskedPaymentKey(PaymentKeyFormat.RANDOM)

        // Then
        assertEquals("7d9f3c2a…4d1a37", masked)
    }

    @Test
    fun `GIVEN a Lightning Address WHEN masking it THEN shows it whole`() {
        // Given
        val address = "pierre@getalby.com"

        // When
        val masked = address.toMaskedPaymentKey(PaymentKeyFormat.LIGHTNING_ADDRESS)

        // Then
        assertEquals("pierre@getalby.com", masked)
    }

    @Test
    fun `GIVEN an LNURL WHEN masking it THEN keeps its first 10 and last 6 characters`() {
        // Given
        val lnurl = LNURL

        // When
        val masked = lnurl.toMaskedPaymentKey(PaymentKeyFormat.LNURL)

        // Then
        assertEquals("lnurl1dp68…fq5fns", masked)
    }

    @Test
    fun `GIVEN a SegWit address WHEN masking it THEN keeps its first 8 and last 7 characters`() {
        // Given
        val address = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"

        // When
        val masked = address.toMaskedPaymentKey(PaymentKeyFormat.SEGWIT)

        // Then
        assertEquals("bc1qw508…kv8f3t4", masked)
    }

    @Test
    fun `GIVEN a Taproot address WHEN masking it THEN keeps its first 8 and last 7 characters`() {
        // Given
        val address = "bc1p5d7rjq7g6rdk2yhzks9smlaqtedr4dekq08ge8ztwac72sfr9rusxg3297"

        // When
        val masked = address.toMaskedPaymentKey(PaymentKeyFormat.TAPROOT)

        // Then
        assertEquals("bc1p5d7r…sxg3297", masked)
    }

    @Test
    fun `GIVEN a phone not in the stored shape WHEN masking it THEN hides it entirely`() {
        // Given
        val phone = "11912345678"

        // When
        val masked = phone.toMaskedPaymentKey(PaymentKeyFormat.PHONE)

        // Then
        assertEquals("•••", masked)
    }

    @Test
    fun `GIVEN an email without a name WHEN masking it THEN hides it entirely`() {
        // Given
        val email = "@email.com"

        // When
        val masked = email.toMaskedPaymentKey(PaymentKeyFormat.EMAIL)

        // Then
        assertEquals("•••", masked)
    }

    @Test
    fun `GIVEN a random key no longer than what the mask keeps WHEN masking it THEN shows it whole`() {
        // Given
        val randomKey = "7d9f3c2a4d1a37"

        // When
        val masked = randomKey.toMaskedPaymentKey(PaymentKeyFormat.RANDOM)

        // Then
        assertEquals("7d9f3c2a4d1a37", masked)
    }

    private companion object {
        const val LNURL =
            "lnurl1dp68gurn8ghj7um9wfmxjcm99e3k7mf0v9cxj0m385ekvcenxc6r2c35xvukxefcv5mkvv34x5ekzd3ev56nyd3hxqu" +
                "rzepexejxxepnxscrvwfnv9nxzcn9xq6xyefhvgcxxcmyxymnserxfq5fns"
    }
}
