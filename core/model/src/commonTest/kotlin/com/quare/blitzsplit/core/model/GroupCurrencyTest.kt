package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GroupCurrencyTest {
    @Test
    fun `GIVEN a whole amount WHEN picking the digits to show THEN it shows none`() {
        // Given
        val amountMinor = 50_000L

        // When
        val digits = GroupCurrency.BRL.getDisplayFractionDigits(amountMinor, showsSats = false)

        // Then
        assertEquals(0, digits)
    }

    @Test
    fun `GIVEN an amount with cents WHEN picking the digits to show THEN it shows every digit of the currency`() {
        // Given
        val amountMinor = 1_250L

        // When
        val digits = GroupCurrency.BRL.getDisplayFractionDigits(amountMinor, showsSats = false)

        // Then
        assertEquals(2, digits)
    }

    @Test
    fun `GIVEN a negative amount with cents WHEN picking the digits to show THEN it shows them all`() {
        // Given
        val amountMinor = -1_250L

        // When
        val digits = GroupCurrency.BRL.getDisplayFractionDigits(amountMinor, showsSats = false)

        // Then
        assertEquals(2, digits)
    }

    @Test
    fun `GIVEN a currency without decimals WHEN picking the digits to show THEN it shows none`() {
        // Given
        val amountMinor = 1_234L

        // When
        val digits = GroupCurrency.JPY.getDisplayFractionDigits(amountMinor, showsSats = false)

        // Then
        assertEquals(0, digits)
    }

    @Test
    fun `GIVEN bitcoin and ether WHEN reading their unit THEN both have 8 decimals as cryptocurrencies`() {
        // Given
        val cryptos = listOf(GroupCurrency.BTC, GroupCurrency.ETH)

        // When
        val units = cryptos.map { Triple(it.fractionDigits, it.minorPerMajor, it.isCrypto) }

        // Then
        assertEquals(listOf(Triple(8, 100_000_000L, true), Triple(8, 100_000_000L, true)), units)
    }

    @Test
    fun `GIVEN every currency but bitcoin and ether WHEN checking whether it is a cryptocurrency THEN none is`() {
        // Given
        val fiats = GroupCurrency.entries - GroupCurrency.BTC - GroupCurrency.ETH

        // When
        val cryptos = fiats.filter { it.isCrypto }

        // Then
        assertEquals(emptyList(), cryptos)
    }

    @Test
    fun `GIVEN the codes of bitcoin and ether WHEN finding their currency THEN finds each`() {
        // Given
        val codes = listOf("BTC", "ETH")

        // When
        val currencies = codes.map { GroupCurrency.fromCodeOrNull(it) }

        // Then
        assertEquals(listOf(GroupCurrency.BTC, GroupCurrency.ETH), currencies)
    }

    @Test
    fun `GIVEN a code of no currency WHEN finding its currency THEN finds none`() {
        // Given
        val code = "XYZ"

        // When
        val currency = GroupCurrency.fromCodeOrNull(code)

        // Then
        assertNull(currency)
    }

    @Test
    fun `GIVEN every two letter region code WHEN picking its currency THEN it is never a cryptocurrency`() {
        // Given
        val regions = ('A'..'Z').flatMap { first -> ('A'..'Z').map { second -> "$first$second" } }

        // When
        val cryptos = regions.map { GroupCurrency.ofRegion(it) }.filter { it.isCrypto }

        // Then
        assertTrue(cryptos.isEmpty(), "regions defaulting to a cryptocurrency: $cryptos")
    }

    @Test
    fun `GIVEN no region WHEN picking its currency THEN it falls back to the dollar`() {
        // Given
        val region: String? = null

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.USD, currency)
    }

    @Test
    fun `GIVEN a region without a currency of the list WHEN picking its currency THEN it falls back to the dollar`() {
        // Given
        val region = "ZZ"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.USD, currency)
    }

    @Test
    fun `GIVEN a cryptocurrency code as the region WHEN picking its currency THEN it falls back to the dollar`() {
        // Given
        val regions = listOf("BTC", "ETH")

        // When
        val currencies = regions.map { GroupCurrency.ofRegion(it) }

        // Then
        assertEquals(listOf(GroupCurrency.USD, GroupCurrency.USD), currencies)
    }

    @Test
    fun `GIVEN Brazil as the region WHEN picking its currency THEN it is the real`() {
        // Given
        val region = "BR"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.BRL, currency)
    }

    @Test
    fun `GIVEN the rest of Latin America WHEN picking their currencies THEN each has its own with 2 decimals`() {
        // Given
        val regions = listOf("BO", "VE", "CR", "GT", "HN", "NI", "DO", "CU")

        // When
        val currencies = regions.map { GroupCurrency.ofRegion(it) }

        // Then
        assertEquals(
            listOf(
                GroupCurrency.BOB to 2,
                GroupCurrency.VES to 2,
                GroupCurrency.CRC to 2,
                GroupCurrency.GTQ to 2,
                GroupCurrency.HNL to 2,
                GroupCurrency.NIO to 2,
                GroupCurrency.DOP to 2,
                GroupCurrency.CUP to 2,
            ),
            currencies.map { it to it.fractionDigits },
        )
    }

    @Test
    fun `GIVEN Poland Turkey and Indonesia WHEN picking their currency THEN it is the zloty lira and rupiah`() {
        // Given
        val regions = listOf("PL", "TR", "ID")

        // When
        val currencies = regions.map { GroupCurrency.ofRegion(it) }

        // Then
        assertEquals(listOf(GroupCurrency.PLN, GroupCurrency.TRY, GroupCurrency.IDR), currencies)
    }

    @Test
    fun `GIVEN Russia and Ukraine WHEN picking their currency THEN it is the ruble and the hryvnia`() {
        // Given
        val regions = listOf("RU", "UA")

        // When
        val currencies = regions.map { GroupCurrency.ofRegion(it) }

        // Then
        assertEquals(listOf(GroupCurrency.RUB, GroupCurrency.UAH), currencies)
    }

    @Test
    fun `GIVEN South Korea WHEN picking its currency THEN it is the won typed without decimals`() {
        // Given
        val region = "KR"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.KRW to 0, currency to currency.fractionDigits)
    }

    @Test
    fun `GIVEN India WHEN picking its currency THEN it is the rupee with its 2 decimals`() {
        // Given
        val region = "IN"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.INR to 2, currency to currency.fractionDigits)
    }

    @Test
    fun `GIVEN the Arabic speaking regions WHEN picking their currencies THEN each has its own with 2 decimals`() {
        // Given
        val regions = listOf("SA", "AE", "EG", "MA")

        // When
        val currencies = regions.map { GroupCurrency.ofRegion(it) }

        // Then
        assertEquals(
            listOf(GroupCurrency.SAR to 2, GroupCurrency.AED to 2, GroupCurrency.EGP to 2, GroupCurrency.MAD to 2),
            currencies.map { it to it.fractionDigits },
        )
    }

    @Test
    fun `GIVEN mainland China WHEN picking its currency THEN it is the yuan with its 2 decimals`() {
        // Given
        val region = "CN"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.CNY to 2, currency to currency.fractionDigits)
    }

    @Test
    fun `GIVEN Israel WHEN picking its currency THEN it is the new shekel with 2 decimals`() {
        // Given
        val region = "IL"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.ILS to 2, currency to currency.fractionDigits)
    }

    @Test
    fun `GIVEN Thailand WHEN picking its currency THEN it is the baht with its 2 decimals`() {
        // Given
        val region = "TH"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.THB to 2, currency to currency.fractionDigits)
    }

    @Test
    fun `GIVEN Vietnam WHEN picking its currency THEN it is the dong typed without decimals`() {
        // Given
        val region = "VN"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.VND to 0, currency to currency.fractionDigits)
    }

    @Test
    fun `GIVEN Taiwan WHEN picking its currency THEN it is the new Taiwan dollar typed without decimals`() {
        // Given
        val region = "TW"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.TWD to 0, currency to currency.fractionDigits)
    }

    @Test
    fun `GIVEN Sweden WHEN picking its currency THEN it is the Swedish krona`() {
        // Given
        val region = "SE"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.SEK to 2, currency to currency.fractionDigits)
    }

    @Test
    fun `GIVEN Czechia WHEN picking its currency THEN it is the Czech koruna`() {
        // Given
        val region = "CZ"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.CZK to 2, currency to currency.fractionDigits)
    }

    @Test
    fun `GIVEN Romania WHEN picking its currency THEN it is the Romanian leu`() {
        // Given
        val region = "RO"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.RON to 2, currency to currency.fractionDigits)
    }

    @Test
    fun `GIVEN Bangladesh WHEN picking its currency THEN it is the Bangladeshi taka`() {
        // Given
        val region = "BD"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.BDT to 2, currency to currency.fractionDigits)
    }

    @Test
    fun `GIVEN Malaysia WHEN picking its currency THEN it is the Malaysian ringgit`() {
        // Given
        val region = "MY"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.MYR to 2, currency to currency.fractionDigits)
    }

    @Test
    fun `GIVEN Hungary WHEN picking its currency THEN it is the forint without decimals though ISO gives it 2`() {
        // Given
        val region = "HU"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.HUF to 0, currency to currency.fractionDigits)
    }

    @Test
    fun `GIVEN Pakistan WHEN picking its currency THEN it is the Pakistani rupee`() {
        // Given
        val region = "PK"

        // When
        val currency = GroupCurrency.ofRegion(region)

        // Then
        assertEquals(GroupCurrency.PKR to 2, currency to currency.fractionDigits)
    }

    @Test
    fun `GIVEN the rupiah WHEN picking the digits to type THEN it types none though ISO gives it 2`() {
        // Given
        val currency = GroupCurrency.IDR

        // When
        val digits = currency.getTypedFractionDigits(showsSats = false)

        // Then
        assertEquals(0, digits)
    }

    @Test
    fun `GIVEN 125 thousand satoshis WHEN picking the digits to show THEN it shows the 5 it needs`() {
        // Given
        val amountMinor = 125_000L

        // When
        val digits = GroupCurrency.BTC.getDisplayFractionDigits(amountMinor, showsSats = false)

        // Then
        assertEquals(5, digits)
    }

    @Test
    fun `GIVEN half an ether WHEN picking the digits to show THEN it shows 1`() {
        // Given
        val amountMinor = 50_000_000L

        // When
        val digits = GroupCurrency.ETH.getDisplayFractionDigits(amountMinor, showsSats = false)

        // Then
        assertEquals(1, digits)
    }

    @Test
    fun `GIVEN a single satoshi WHEN picking the digits to show THEN it shows all 8`() {
        // Given
        val amountMinor = 1L

        // When
        val digits = GroupCurrency.BTC.getDisplayFractionDigits(amountMinor, showsSats = false)

        // Then
        assertEquals(8, digits)
    }

    @Test
    fun `GIVEN a whole bitcoin WHEN picking the digits to show THEN it shows none`() {
        // Given
        val amountMinor = 100_000_000L

        // When
        val digits = GroupCurrency.BTC.getDisplayFractionDigits(amountMinor, showsSats = false)

        // Then
        assertEquals(0, digits)
    }

    @Test
    fun `GIVEN several ether WHEN picking the digits to show THEN it shows none`() {
        // Given
        val amountMinor = 300_000_000L

        // When
        val digits = GroupCurrency.ETH.getDisplayFractionDigits(amountMinor, showsSats = false)

        // Then
        assertEquals(0, digits)
    }

    @Test
    fun `GIVEN a bitcoin and a half WHEN picking the digits to show THEN it shows 1`() {
        // Given
        val amountMinor = 150_000_000L

        // When
        val digits = GroupCurrency.BTC.getDisplayFractionDigits(amountMinor, showsSats = false)

        // Then
        assertEquals(1, digits)
    }

    @Test
    fun `GIVEN a negative bitcoin amount WHEN picking the digits to show THEN it shows the 5 it needs`() {
        // Given
        val amountMinor = -125_000L

        // When
        val digits = GroupCurrency.BTC.getDisplayFractionDigits(amountMinor, showsSats = false)

        // Then
        assertEquals(5, digits)
    }

    @Test
    fun `GIVEN a negative ether amount WHEN picking the digits to show THEN it shows the 1 it needs`() {
        // Given
        val amountMinor = -50_000_000L

        // When
        val digits = GroupCurrency.ETH.getDisplayFractionDigits(amountMinor, showsSats = false)

        // Then
        assertEquals(1, digits)
    }

    @Test
    fun `GIVEN bitcoin in satoshis and a fraction of a coin WHEN picking the digits to show THEN it shows none`() {
        // Given
        val amountMinor = 125_000L

        // When
        val digits = GroupCurrency.BTC.getDisplayFractionDigits(amountMinor, showsSats = true)

        // Then
        assertEquals(0, digits)
    }

    @Test
    fun `GIVEN bitcoin in satoshis and a single satoshi WHEN picking the digits to show THEN it shows none`() {
        // Given
        val amountMinor = 1L

        // When
        val digits = GroupCurrency.BTC.getDisplayFractionDigits(amountMinor, showsSats = true)

        // Then
        assertEquals(0, digits)
    }

    @Test
    fun `GIVEN bitcoin in satoshis and a negative amount WHEN picking the digits to show THEN it shows none`() {
        // Given
        val amountMinor = -125_000L

        // When
        val digits = GroupCurrency.BTC.getDisplayFractionDigits(amountMinor, showsSats = true)

        // Then
        assertEquals(0, digits)
    }

    @Test
    fun `GIVEN satoshis on and an ether amount WHEN picking the digits to show THEN it still shows the 5 it needs`() {
        // Given
        val amountMinor = 125_000L

        // When
        val digits = GroupCurrency.ETH.getDisplayFractionDigits(amountMinor, showsSats = true)

        // Then
        assertEquals(5, digits)
    }

    @Test
    fun `GIVEN satoshis on and an amount with cents WHEN picking the digits to show THEN it shows both`() {
        // Given
        val amountMinor = 1_250L

        // When
        val digits = GroupCurrency.BRL.getDisplayFractionDigits(amountMinor, showsSats = true)

        // Then
        assertEquals(2, digits)
    }

    @Test
    fun `GIVEN satoshis on and a whole amount WHEN picking the digits to show THEN it shows none`() {
        // Given
        val amountMinor = 50_000L

        // When
        val digits = GroupCurrency.BRL.getDisplayFractionDigits(amountMinor, showsSats = true)

        // Then
        assertEquals(0, digits)
    }

    @Test
    fun `GIVEN satoshis on and a currency without decimals WHEN picking the digits to show THEN it shows none`() {
        // Given
        val amountMinor = 1_234L

        // When
        val digits = GroupCurrency.JPY.getDisplayFractionDigits(amountMinor, showsSats = true)

        // Then
        assertEquals(0, digits)
    }

    @Test
    fun `GIVEN an amount with only tenths WHEN picking the digits to show THEN it still shows both`() {
        // Given
        val amountMinor = 1_210L

        // When
        val digits = GroupCurrency.BRL.getDisplayFractionDigits(amountMinor, showsSats = false)

        // Then
        assertEquals(2, digits)
    }

    @Test
    fun `GIVEN bitcoin WHEN picking the digits to type THEN it types 8`() {
        // Given
        val currency = GroupCurrency.BTC

        // When
        val digits = currency.getTypedFractionDigits(showsSats = false)

        // Then
        assertEquals(8, digits)
    }

    @Test
    fun `GIVEN bitcoin in satoshis WHEN picking the digits to type THEN it types none`() {
        // Given
        val currency = GroupCurrency.BTC

        // When
        val digits = currency.getTypedFractionDigits(showsSats = true)

        // Then
        assertEquals(0, digits)
    }

    @Test
    fun `GIVEN satoshis on and ether WHEN picking the digits to type THEN it still types 8`() {
        // Given
        val currency = GroupCurrency.ETH

        // When
        val digits = currency.getTypedFractionDigits(showsSats = true)

        // Then
        assertEquals(8, digits)
    }

    @Test
    fun `GIVEN ether WHEN picking the digits to type THEN it types 8`() {
        // Given
        val currency = GroupCurrency.ETH

        // When
        val digits = currency.getTypedFractionDigits(showsSats = false)

        // Then
        assertEquals(8, digits)
    }

    @Test
    fun `GIVEN satoshis on and the real WHEN picking the digits to type THEN it types its 2`() {
        // Given
        val currency = GroupCurrency.BRL

        // When
        val digits = currency.getTypedFractionDigits(showsSats = true)

        // Then
        assertEquals(2, digits)
    }

    @Test
    fun `GIVEN the real WHEN picking the digits to type THEN it types its 2`() {
        // Given
        val currency = GroupCurrency.BRL

        // When
        val digits = currency.getTypedFractionDigits(showsSats = false)

        // Then
        assertEquals(2, digits)
    }

    @Test
    fun `GIVEN satoshis on and the yen WHEN picking the digits to type THEN it types none`() {
        // Given
        val currency = GroupCurrency.JPY

        // When
        val digits = currency.getTypedFractionDigits(showsSats = true)

        // Then
        assertEquals(0, digits)
    }

    @Test
    fun `GIVEN a code this version does not know WHEN reading it THEN keeps the raw code as its own currency`() {
        // Given
        val code = "XYZ"

        // When
        val currency = GroupCurrency.fromCode(code)

        // Then
        assertEquals("XYZ", currency.code)
        assertFalse(currency.isKnown)
        assertEquals(GroupCurrency.fromCode("XYZ"), currency)
        assertNotEquals(GroupCurrency.fromCode("XYW"), currency)
        assertNotEquals(GroupCurrency.USD, currency)
    }

    @Test
    fun `GIVEN a code this version knows WHEN reading it THEN gives the listed currency`() {
        // Given
        val code = "BRL"

        // When
        val currency = GroupCurrency.fromCode(code)

        // Then
        assertSame(GroupCurrency.BRL, currency)
        assertTrue(currency.isKnown)
    }

    @Test
    fun `GIVEN a code this version does not know WHEN listing the currencies THEN it is never offered`() {
        // Given
        val unknown = GroupCurrency.fromCode("XYZ")

        // When
        val entries = GroupCurrency.entries

        // Then
        assertFalse(unknown in entries)
        assertNull(GroupCurrency.fromCodeOrNull("XYZ"))
    }

    @Test
    fun `GIVEN an unknown currency WHEN picking the digits to show THEN it has two decimals like most currencies`() {
        // Given
        val currency = GroupCurrency.fromCode("XYZ")

        // When
        val digits = currency.getDisplayFractionDigits(1_250L, showsSats = false)

        // Then
        assertEquals(2, digits)
    }
}
