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
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertEquals(123_456L, amount)
    }

    @Test
    fun `GIVEN English separators WHEN parsing THEN reads the comma as thousands and the dot as decimals`() {
        // Given
        val text = "1,234.56"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.USD, false)

        // Then
        assertEquals(123_456L, amount)
    }

    @Test
    fun `GIVEN one decimal digit WHEN parsing THEN reads it as tens of cents`() {
        // Given
        val text = "12,5"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertEquals(1_250L, amount)
    }

    @Test
    fun `GIVEN a whole number WHEN parsing THEN reads it in the major unit`() {
        // Given
        val text = "12"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertEquals(1_200L, amount)
    }

    @Test
    fun `GIVEN less than one real WHEN parsing THEN reads the cents`() {
        // Given
        val text = "0,5"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertEquals(50L, amount)
    }

    @Test
    fun `GIVEN only the decimals WHEN parsing THEN reads the cents`() {
        // Given
        val text = ",50"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertEquals(50L, amount)
    }

    @Test
    fun `GIVEN a separator still without decimals WHEN parsing THEN reads the whole number`() {
        // Given
        val text = "12,"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertEquals(1_200L, amount)
    }

    @Test
    fun `GIVEN three digits after a comma in a currency of two WHEN parsing THEN reads the comma as thousands`() {
        // Given
        val text = "12,345"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertEquals(1_234_500L, amount)
    }

    @Test
    fun `GIVEN three digits after a dot in a currency of two WHEN parsing THEN reads the dot as thousands`() {
        // Given
        val text = "1.234"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertEquals(123_400L, amount)
    }

    @Test
    fun `GIVEN millions grouped by dots WHEN parsing THEN reads every dot as thousands`() {
        // Given
        val text = "1.234.567"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertEquals(123_456_700L, amount)
    }

    @Test
    fun `GIVEN the same separator twice before two digits WHEN parsing THEN reads neither as decimals`() {
        // Given
        val text = "1,50,25"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertEquals(1_502_500L, amount)
    }

    @Test
    fun `GIVEN spaces between the digits WHEN parsing THEN ignores them`() {
        // Given
        val text = " 1 234,56 "

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertEquals(123_456L, amount)
    }

    @Test
    fun `GIVEN letters next to the digits WHEN parsing THEN it is no amount`() {
        // Given
        val text = "12a"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN the currency symbol WHEN parsing THEN it is no amount`() {
        // Given
        val text = "R$ 12,00"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN a minus sign WHEN parsing THEN it is no amount`() {
        // Given
        val text = "-5"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN an empty field WHEN parsing THEN it is no amount`() {
        // Given
        val text = ""

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN only spaces WHEN parsing THEN it is no amount`() {
        // Given
        val text = "   "

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN only a separator WHEN parsing THEN it is no amount`() {
        // Given
        val text = ","

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN more digits than a Long of the minor unit holds WHEN parsing THEN it is no amount`() {
        // Given
        val text = "12345678901234567"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN 12 whole digits WHEN parsing THEN reads them`() {
        // Given
        val text = "123456789012"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertEquals(12_345_678_901_200L, amount)
    }

    @Test
    fun `GIVEN yen grouped by a dot WHEN parsing THEN reads the dot as thousands`() {
        // Given
        val text = "1.500"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.JPY, false)

        // Then
        assertEquals(1_500L, amount)
    }

    @Test
    fun `GIVEN a whole number of pesos WHEN parsing THEN the minor unit is the peso`() {
        // Given
        val text = "2500"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.CLP, false)

        // Then
        assertEquals(2_500L, amount)
    }

    @Test
    fun `GIVEN a decimal comma in a currency without decimals WHEN parsing THEN reads the comma as grouping`() {
        // Given
        val text = "12,5"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.PYG, false)

        // Then
        assertEquals(125L, amount)
    }

    @Test
    fun `GIVEN an amount in cents WHEN formatting with a comma THEN groups with the dot and ends in the cents`() {
        // Given
        val amountMinor = 123_456L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("1.234,56", text)
    }

    @Test
    fun `GIVEN a few cents WHEN formatting with a dot THEN pads them with zeros`() {
        // Given
        val amountMinor = 5L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.USD, false, '.')

        // Then
        assertEquals("0.05", text)
    }

    @Test
    fun `GIVEN yen WHEN formatting THEN groups thousands and writes no decimals`() {
        // Given
        val amountMinor = 1_500L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.JPY, false, ',')

        // Then
        assertEquals("1.500", text)
    }

    @Test
    fun `GIVEN an amount the app formatted WHEN parsing it back THEN reads the same amount`() {
        // Given
        val text = AmountInput.format(624_000L, GroupCurrency.BRL, false, ',')

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, false)

        // Then
        assertEquals(624_000L, amount)
    }

    @Test
    fun `GIVEN a negative amount WHEN formatting THEN puts the minus before the grouped number`() {
        // Given
        val amountMinor = -123_456L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("-1.234,56", text)
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

    @Test
    fun `GIVEN millions in cents WHEN formatting with a comma THEN groups every thousand with the dot`() {
        // Given
        val amountMinor = 123_456_789L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("1.234.567,89", text)
    }

    @Test
    fun `GIVEN millions in cents WHEN formatting with a dot THEN groups every thousand with the comma`() {
        // Given
        val amountMinor = 123_456_789L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.USD, false, '.')

        // Then
        assertEquals("1,234,567.89", text)
    }

    @Test
    fun `GIVEN yen WHEN formatting with a dot THEN groups thousands with the comma`() {
        // Given
        val amountMinor = 1_500L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.JPY, false, '.')

        // Then
        assertEquals("1,500", text)
    }

    @Test
    fun `GIVEN less than a thousand WHEN formatting THEN writes no grouping`() {
        // Given
        val amountMinor = 99_999L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("999,99", text)
    }

    @Test
    fun `GIVEN zero WHEN formatting reais and yen THEN writes zero with the currency decimals`() {
        // Given
        val amountMinor = 0L

        // When
        val reais = AmountInput.format(amountMinor, GroupCurrency.BRL, false, ',')
        val yen = AmountInput.format(amountMinor, GroupCurrency.JPY, false, ',')

        // Then
        assertEquals("0,00", reais)
        assertEquals("0", yen)
    }

    @Test
    fun `GIVEN reais formatted with a comma WHEN parsing them back THEN reads the same amounts`() {
        // Given
        val texts = centAmounts.map { AmountInput.format(it, GroupCurrency.BRL, false, ',') }

        // When
        val amounts = texts.map { AmountInput.parse(it, GroupCurrency.BRL, false) }

        // Then
        assertEquals(centAmounts, amounts)
    }

    @Test
    fun `GIVEN dollars formatted with a dot WHEN parsing them back THEN reads the same amounts`() {
        // Given
        val texts = centAmounts.map { AmountInput.format(it, GroupCurrency.USD, false, '.') }

        // When
        val amounts = texts.map { AmountInput.parse(it, GroupCurrency.USD, false) }

        // Then
        assertEquals(centAmounts, amounts)
    }

    @Test
    fun `GIVEN yen formatted with a comma WHEN parsing them back THEN reads the same amounts`() {
        // Given
        val texts = yenAmounts.map { AmountInput.format(it, GroupCurrency.JPY, false, ',') }

        // When
        val amounts = texts.map { AmountInput.parse(it, GroupCurrency.JPY, false) }

        // Then
        assertEquals(yenAmounts, amounts)
    }

    @Test
    fun `GIVEN yen formatted with a dot WHEN parsing them back THEN reads the same amounts`() {
        // Given
        val texts = yenAmounts.map { AmountInput.format(it, GroupCurrency.JPY, false, '.') }

        // When
        val amounts = texts.map { AmountInput.parse(it, GroupCurrency.JPY, false) }

        // Then
        assertEquals(yenAmounts, amounts)
    }

    @Test
    fun `GIVEN a fraction of a bitcoin WHEN parsing THEN reads it in satoshis`() {
        // Given
        val text = "0,00125"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BTC, false)

        // Then
        assertEquals(125_000L, amount)
    }

    @Test
    fun `GIVEN one satoshi written in bitcoin WHEN parsing THEN reads one minor unit`() {
        // Given
        val text = "0.00000001"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BTC, false)

        // Then
        assertEquals(1L, amount)
    }

    @Test
    fun `GIVEN a whole bitcoin WHEN parsing THEN reads 100 million satoshis`() {
        // Given
        val text = "1"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BTC, false)

        // Then
        assertEquals(100_000_000L, amount)
    }

    @Test
    fun `GIVEN nine decimals in bitcoin WHEN parsing THEN reads the separator as grouping`() {
        // Given
        val text = "0,123456789"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BTC, false)

        // Then
        assertEquals(12_345_678_900_000_000L, amount)
    }

    @Test
    fun `GIVEN satoshis grouped by a dot WHEN parsing in satoshis THEN reads whole sats`() {
        // Given
        val text = "12.345"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BTC, true)

        // Then
        assertEquals(12_345L, amount)
    }

    @Test
    fun `GIVEN satoshis grouped by a comma WHEN parsing in satoshis THEN reads whole sats`() {
        // Given
        val text = "12,345"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BTC, true)

        // Then
        assertEquals(12_345L, amount)
    }

    @Test
    fun `GIVEN two decimals typed in satoshis WHEN parsing THEN reads the comma as grouping`() {
        // Given
        val text = "12,50"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BTC, true)

        // Then
        assertEquals(1_250L, amount)
    }

    @Test
    fun `GIVEN satoshis shown WHEN parsing ether THEN still reads its 8 decimals`() {
        // Given
        val text = "0,5"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.ETH, true)

        // Then
        assertEquals(50_000_000L, amount)
    }

    @Test
    fun `GIVEN satoshis shown WHEN parsing reais THEN still reads the cents`() {
        // Given
        val text = "12,5"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BRL, true)

        // Then
        assertEquals(1_250L, amount)
    }

    @Test
    fun `GIVEN 21 million bitcoin with every decimal WHEN parsing THEN reads the cap without overflowing`() {
        // Given
        val text = "21.000.000,00000000"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BTC, false)

        // Then
        assertEquals(2_100_000_000_000_000L, amount)
    }

    @Test
    fun `GIVEN 90 million ether WHEN parsing THEN reads the cap without overflowing`() {
        // Given
        val text = "90,000,000.00000000"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.ETH, false)

        // Then
        assertEquals(9_000_000_000_000_000L, amount)
    }

    @Test
    fun `GIVEN 21 million bitcoin in satoshis WHEN parsing in satoshis THEN reads the cap`() {
        // Given
        val text = "2.100.000.000.000.000"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BTC, true)

        // Then
        assertEquals(2_100_000_000_000_000L, amount)
    }

    @Test
    fun `GIVEN 10 whole digits of bitcoin and its 8 decimals WHEN parsing THEN reads the 18 digits`() {
        // Given
        val text = "9999999999,99999999"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BTC, false)

        // Then
        assertEquals(999_999_999_999_999_999L, amount)
    }

    @Test
    fun `GIVEN 11 whole digits of bitcoin WHEN parsing THEN it is no amount`() {
        // Given
        val text = "99999999999"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BTC, false)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN 11 whole digits of ether WHEN parsing THEN it is no amount`() {
        // Given
        val text = "99999999999,5"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.ETH, false)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN 18 digits of satoshis WHEN parsing in satoshis THEN reads them`() {
        // Given
        val text = "999999999999999999"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BTC, true)

        // Then
        assertEquals(999_999_999_999_999_999L, amount)
    }

    @Test
    fun `GIVEN 19 digits of satoshis WHEN parsing in satoshis THEN it is no amount`() {
        // Given
        val text = "9999999999999999999"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BTC, true)

        // Then
        assertNull(amount)
    }

    @Test
    fun `GIVEN zeros in front of a bitcoin amount WHEN parsing THEN they do not count as digits`() {
        // Given
        val text = "000000000001,5"

        // When
        val amount = AmountInput.parse(text, GroupCurrency.BTC, false)

        // Then
        assertEquals(150_000_000L, amount)
    }

    @Test
    fun `GIVEN satoshis WHEN formatting in bitcoin with a comma THEN writes the 8 decimals`() {
        // Given
        val amountMinor = 125_000L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.BTC, false, ',')

        // Then
        assertEquals("0,00125000", text)
    }

    @Test
    fun `GIVEN satoshis WHEN formatting in satoshis with a comma THEN writes whole sats grouped by the dot`() {
        // Given
        val amountMinor = 12_345L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.BTC, true, ',')

        // Then
        assertEquals("12.345", text)
    }

    @Test
    fun `GIVEN satoshis WHEN formatting in satoshis with a dot THEN writes whole sats grouped by the comma`() {
        // Given
        val amountMinor = 1_234_567L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.BTC, true, '.')

        // Then
        assertEquals("1,234,567", text)
    }

    @Test
    fun `GIVEN satoshis shown WHEN formatting ether THEN still writes its 8 decimals`() {
        // Given
        val amountMinor = 150_000_000L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.ETH, true, ',')

        // Then
        assertEquals("1,50000000", text)
    }

    @Test
    fun `GIVEN satoshis shown WHEN formatting reais THEN still writes the cents`() {
        // Given
        val amountMinor = 1_250L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.BRL, true, ',')

        // Then
        assertEquals("12,50", text)
    }

    @Test
    fun `GIVEN the ether cap WHEN formatting with a comma THEN groups the 90 million`() {
        // Given
        val amountMinor = 9_000_000_000_000_000L

        // When
        val text = AmountInput.format(amountMinor, GroupCurrency.ETH, false, ',')

        // Then
        assertEquals("90.000.000,00000000", text)
    }

    @Test
    fun `GIVEN bitcoin formatted with a comma WHEN parsing them back THEN reads the same amounts`() {
        // Given
        val texts = satoshiAmounts.map { AmountInput.format(it, GroupCurrency.BTC, false, ',') }

        // When
        val amounts = texts.map { AmountInput.parse(it, GroupCurrency.BTC, false) }

        // Then
        assertEquals(satoshiAmounts, amounts)
    }

    @Test
    fun `GIVEN ether formatted with a dot WHEN parsing them back THEN reads the same amounts`() {
        // Given
        val texts = satoshiAmounts.map { AmountInput.format(it, GroupCurrency.ETH, false, '.') }

        // When
        val amounts = texts.map { AmountInput.parse(it, GroupCurrency.ETH, false) }

        // Then
        assertEquals(satoshiAmounts, amounts)
    }

    @Test
    fun `GIVEN satoshis formatted with a comma WHEN parsing them back in satoshis THEN reads the same amounts`() {
        // Given
        val texts = satoshiAmounts.map { AmountInput.format(it, GroupCurrency.BTC, true, ',') }

        // When
        val amounts = texts.map { AmountInput.parse(it, GroupCurrency.BTC, true) }

        // Then
        assertEquals(satoshiAmounts, amounts)
    }

    @Test
    fun `GIVEN satoshis formatted with a dot WHEN parsing them back in satoshis THEN reads the same amounts`() {
        // Given
        val texts = satoshiAmounts.map { AmountInput.format(it, GroupCurrency.BTC, true, '.') }

        // When
        val amounts = texts.map { AmountInput.parse(it, GroupCurrency.BTC, true) }

        // Then
        assertEquals(satoshiAmounts, amounts)
    }

    private val centAmounts: List<Long> =
        listOf(0L, 1L, 99L, 100L, 99_999L, 100_000L, 123_456_789L, 10_000_000_000L, 99_999_999_999L)

    private val yenAmounts: List<Long> = listOf(0L, 1L, 999L, 1_000L, 1_234_567L, 100_000_000L, 999_999_999L)

    private val satoshiAmounts: List<Long> =
        listOf(
            0L,
            1L,
            999L,
            125_000L,
            100_000_000L,
            123_456_789_012L,
            2_100_000_000_000_000L,
            9_000_000_000_000_000L,
        )
}
