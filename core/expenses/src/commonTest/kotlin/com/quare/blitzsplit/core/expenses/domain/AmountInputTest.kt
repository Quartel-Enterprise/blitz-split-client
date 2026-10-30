package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AmountInputTest {
    @Test
    fun `GIVEN Portuguese separators WHEN parsing THEN reads the dot as thousands and the comma as decimals`() {
        // Given
        val text = "1.234,56"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertEquals(123_456L, amount)
    }

    @Test
    fun `GIVEN English separators WHEN parsing THEN reads the comma as thousands and the dot as decimals`() {
        // Given
        val text = "1,234.56"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.USD)

        // Then
        assertEquals(123_456L, amount)
    }

    @Test
    fun `GIVEN one decimal digit WHEN parsing THEN reads it as tens of cents`() {
        // Given
        val text = "12,5"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertEquals(1_250L, amount)
    }

    @Test
    fun `GIVEN a whole number WHEN parsing THEN reads it in the major unit`() {
        // Given
        val text = "12"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertEquals(1_200L, amount)
    }

    @Test
    fun `GIVEN less than one real WHEN parsing THEN reads the cents`() {
        // Given
        val text = "0,5"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertEquals(50L, amount)
    }

    @Test
    fun `GIVEN only the decimals WHEN parsing THEN reads the cents`() {
        // Given
        val text = ",50"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertEquals(50L, amount)
    }

    @Test
    fun `GIVEN a separator still without decimals WHEN parsing THEN reads the whole number`() {
        // Given
        val text = "12,"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertEquals(1_200L, amount)
    }

    @Test
    fun `GIVEN three digits after a comma in a currency of two WHEN parsing THEN reads the comma as thousands`() {
        // Given
        val text = "12,345"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertEquals(1_234_500L, amount)
    }

    @Test
    fun `GIVEN three digits after a dot in a currency of two WHEN parsing THEN reads the dot as thousands`() {
        // Given
        val text = "1.234"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertEquals(123_400L, amount)
    }

    @Test
    fun `GIVEN millions grouped by dots WHEN parsing THEN reads every dot as thousands`() {
        // Given
        val text = "1.234.567"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertEquals(123_456_700L, amount)
    }

    @Test
    fun `GIVEN the same separator twice before two digits WHEN parsing THEN reads neither as decimals`() {
        // Given
        val text = "1,50,25"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertEquals(1_502_500L, amount)
    }

    @Test
    fun `GIVEN spaces between the digits WHEN parsing THEN ignores them`() {
        // Given
        val text = " 1 234,56 "

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertEquals(123_456L, amount)
    }

    @Test
    fun `GIVEN letters next to the digits WHEN parsing THEN it is no amount`() {
        // Given
        val text = "12a"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN the currency symbol WHEN parsing THEN it is no amount`() {
        // Given
        val text = "R$ 12,00"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN a minus sign WHEN parsing THEN it is no amount`() {
        // Given
        val text = "-5"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN an empty field WHEN parsing THEN it is no amount`() {
        // Given
        val text = ""

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN only spaces WHEN parsing THEN it is no amount`() {
        // Given
        val text = "   "

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN only a separator WHEN parsing THEN it is no amount`() {
        // Given
        val text = ","

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN 13 whole digits WHEN parsing THEN it is no amount`() {
        // Given
        val text = "1234567890123"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN 12 whole digits WHEN parsing THEN reads them`() {
        // Given
        val text = "123456789012"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertEquals(12_345_678_901_200L, amount)
    }

    @Test
    fun `GIVEN yen grouped by a dot WHEN parsing THEN reads the dot as thousands`() {
        // Given
        val text = "1.500"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.JPY)

        // Then
        assertEquals(1_500L, amount)
    }

    @Test
    fun `GIVEN a whole number of pesos WHEN parsing THEN the minor unit is the peso`() {
        // Given
        val text = "2500"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.CLP)

        // Then
        assertEquals(2_500L, amount)
    }

    @Test
    fun `GIVEN a decimal comma in a currency without decimals WHEN parsing THEN reads the comma as grouping`() {
        // Given
        val text = "12,5"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.PYG)

        // Then
        assertEquals(125L, amount)
    }

    @Test
    fun `GIVEN an amount in cents WHEN formatting with a comma THEN writes the cents after it`() {
        // Given
        val amountMinor = 123_456L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.BRL, ',')

        // Then
        assertEquals("1234,56", text)
    }

    @Test
    fun `GIVEN a few cents WHEN formatting with a dot THEN pads them with zeros`() {
        // Given
        val amountMinor = 5L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.USD, '.')

        // Then
        assertEquals("0.05", text)
    }

    @Test
    fun `GIVEN yen WHEN formatting THEN writes no separator`() {
        // Given
        val amountMinor = 1_500L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.JPY, ',')

        // Then
        assertEquals("1500", text)
    }

    @Test
    fun `GIVEN an amount the app formatted WHEN parsing it back THEN reads the same amount`() {
        // Given
        val text = AmountInput.format(624_000L, GroupCurrency.BRL, ',')

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL)

        // Then
        assertEquals(624_000L, amount)
    }

    @Test
    fun `GIVEN a whole percentage WHEN parsing a whole number THEN reads it`() {
        // Given
        val text = "50"

        // When
        val value = AmountInput.parseWhole(text)

        // Then
        assertEquals(50L, value)
    }

    @Test
    fun `GIVEN spaces around the digits WHEN parsing a whole number THEN ignores them`() {
        // Given
        val text = " 3 3 "

        // When
        val value = AmountInput.parseWhole(text)

        // Then
        assertEquals(33L, value)
    }

    @Test
    fun `GIVEN an empty field WHEN parsing a whole number THEN it is no number`() {
        // Given
        val text = " "

        // When
        val value = AmountInput.parseWhole(text)

        // Then
        assertNull(value)
    }

    @Test
    fun `GIVEN a decimal WHEN parsing a whole number THEN it is no number`() {
        // Given
        val text = "12,5"

        // When
        val value = AmountInput.parseWhole(text)

        // Then
        assertNull(value)
    }

    @Test
    fun `GIVEN a minus sign WHEN parsing a whole number THEN it is no number`() {
        // Given
        val text = "-1"

        // When
        val value = AmountInput.parseWhole(text)

        // Then
        assertNull(value)
    }

    @Test
    fun `GIVEN 13 digits WHEN parsing a whole number THEN it is no number`() {
        // Given
        val text = "1234567890123"

        // When
        val value = AmountInput.parseWhole(text)

        // Then
        assertNull(value)
    }

    @Test
    fun `GIVEN 12 digits WHEN parsing a whole number THEN reads them`() {
        // Given
        val text = "999999999999"

        // When
        val value = AmountInput.parseWhole(text)

        // Then
        assertEquals(999_999_999_999L, value)
    }
}
