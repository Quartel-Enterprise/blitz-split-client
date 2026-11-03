package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class GroupCurrencyTest {
    @Test
    fun `GIVEN a whole amount WHEN picking the digits to show THEN it shows none`() {
        // Given
        val amountMinor = 50_000L

        // When
        val digits = GroupCurrency.BRL.getDisplayFractionDigits(amountMinor)

        // Then
        assertEquals(0, digits)
    }

    @Test
    fun `GIVEN an amount with cents WHEN picking the digits to show THEN it shows every digit of the currency`() {
        // Given
        val amountMinor = 1_250L

        // When
        val digits = GroupCurrency.BRL.getDisplayFractionDigits(amountMinor)

        // Then
        assertEquals(2, digits)
    }

    @Test
    fun `GIVEN a negative amount with cents WHEN picking the digits to show THEN it shows them all`() {
        // Given
        val amountMinor = -1_250L

        // When
        val digits = GroupCurrency.BRL.getDisplayFractionDigits(amountMinor)

        // Then
        assertEquals(2, digits)
    }

    @Test
    fun `GIVEN a currency without decimals WHEN picking the digits to show THEN it shows none`() {
        // Given
        val amountMinor = 1_234L

        // When
        val digits = GroupCurrency.JPY.getDisplayFractionDigits(amountMinor)

        // Then
        assertEquals(0, digits)
    }
}
