package com.quare.blitzsplit.core.expenses.export

import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.test.Test
import kotlin.test.assertEquals

class DecimalOfTest {
    @Test
    fun `GIVEN an amount in reais WHEN writing it THEN keeps both decimals with a dot`() {
        // Given
        val minor = 12_050L

        // When
        val decimal = decimalOf(minor, GroupCurrency.BRL)

        // Then
        assertEquals("120.50", decimal)
    }

    @Test
    fun `GIVEN a negative amount WHEN writing it THEN leads with a minus and keeps the decimals`() {
        // Given
        val minor = -1_000L

        // When
        val decimal = decimalOf(minor, GroupCurrency.BRL)

        // Then
        assertEquals("-10.00", decimal)
    }

    @Test
    fun `GIVEN a few cents WHEN writing them THEN pads the whole part with a zero`() {
        // Given
        val minor = 5L

        // When
        val decimal = decimalOf(minor, GroupCurrency.BRL)

        // Then
        assertEquals("0.05", decimal)
    }

    @Test
    fun `GIVEN a few negative cents WHEN writing them THEN keeps the minus before the zero`() {
        // Given
        val minor = -5L

        // When
        val decimal = decimalOf(minor, GroupCurrency.BRL)

        // Then
        assertEquals("-0.05", decimal)
    }

    @Test
    fun `GIVEN zero WHEN writing it THEN keeps the currency decimals`() {
        // Given
        val minor = 0L

        // When
        val decimal = decimalOf(minor, GroupCurrency.BRL)

        // Then
        assertEquals("0.00", decimal)
    }

    @Test
    fun `GIVEN a large amount WHEN writing it THEN has no thousands separator`() {
        // Given
        val minor = 123_456_789L

        // When
        val decimal = decimalOf(minor, GroupCurrency.BRL)

        // Then
        assertEquals("1234567.89", decimal)
    }

    @Test
    fun `GIVEN an amount in yen WHEN writing it THEN has no decimals`() {
        // Given
        val minor = 1_500L

        // When
        val decimal = decimalOf(minor, GroupCurrency.JPY)

        // Then
        assertEquals("1500", decimal)
    }

    @Test
    fun `GIVEN a negative amount in yen WHEN writing it THEN has a minus and no decimals`() {
        // Given
        val minor = -1_500L

        // When
        val decimal = decimalOf(minor, GroupCurrency.JPY)

        // Then
        assertEquals("-1500", decimal)
    }

    @Test
    fun `GIVEN an amount in bitcoin WHEN writing it THEN keeps its eight decimals`() {
        // Given
        val minor = 125_000L

        // When
        val decimal = decimalOf(minor, GroupCurrency.BTC)

        // Then
        assertEquals("0.00125000", decimal)
    }
}
