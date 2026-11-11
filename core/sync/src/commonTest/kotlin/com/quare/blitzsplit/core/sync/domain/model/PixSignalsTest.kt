package com.quare.blitzsplit.core.sync.domain.model

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PixSignalsTest {
    @Test
    fun `GIVEN the app picked in Portuguese WHEN reading whether it shows Pix THEN it does on an English device`() {
        // Given
        val signals = signals(
            appLanguage = AppLanguage.PORTUGUESE,
            deviceLanguage = "en-US",
        )

        // When
        val showsPix = signals.showsPix

        // Then
        assertTrue(showsPix)
    }

    @Test
    fun `GIVEN the app following a Brazilian Portuguese device WHEN reading whether it shows Pix THEN it does`() {
        // Given
        val tags = listOf("pt-BR", "pt_BR", "pt-br", "pt")

        // When
        val results = tags.map { signals(deviceLanguage = it).showsPix }

        // Then
        assertEquals(listOf(true, true, true, true), results)
    }

    @Test
    fun `GIVEN the app following a Portuguese of Portugal device WHEN reading whether it shows Pix THEN it does not`() {
        // Given
        val tags = listOf("pt-PT", "pt_PT")

        // When
        val results = tags.map { signals(deviceLanguage = it).showsPix }

        // Then
        assertEquals(listOf(false, false), results)
    }

    @Test
    fun `GIVEN the app following a device in another language WHEN reading whether it shows Pix THEN it does not`() {
        // Given
        val tags = listOf("en-US", "es-MX", "fr", null)

        // When
        val results = tags.map { signals(deviceLanguage = it).showsPix }

        // Then
        assertEquals(listOf(false, false, false, false), results)
    }

    @Test
    fun `GIVEN another language picked on a Brazilian device WHEN reading whether it shows Pix THEN it does not`() {
        // Given
        val pickedLanguages = AppLanguage.entries.filter { it != AppLanguage.SYSTEM && it != AppLanguage.PORTUGUESE }

        // When
        val results = pickedLanguages.map {
            signals(
                appLanguage = it,
                deviceLanguage = "pt-BR",
            ).showsPix
        }

        // Then
        assertTrue(results.isNotEmpty())
        assertTrue(results.none { it })
    }

    @Test
    fun `GIVEN a saved Pix key WHEN reading whether it shows Pix THEN it does in English on a US device`() {
        // Given
        val signals = signals(hasOwnPixKey = true)

        // When
        val showsPix = signals.showsPix

        // Then
        assertTrue(showsPix)
    }

    @Test
    fun `GIVEN a group in reais WHEN reading whether it shows Pix THEN it does`() {
        // Given
        val signals = signals(groupCurrencies = listOf(GroupCurrency.USD, GroupCurrency.BRL))

        // When
        val showsPix = signals.showsPix

        // Then
        assertTrue(showsPix)
    }

    @Test
    fun `GIVEN groups in other currencies WHEN reading whether it shows Pix THEN it does not`() {
        // Given
        val signals = signals(
            groupCurrencies = listOf(GroupCurrency.USD, GroupCurrency.EUR, GroupCurrency.BTC),
        )

        // When
        val showsPix = signals.showsPix

        // Then
        assertFalse(showsPix)
    }

    @Test
    fun `GIVEN a member with a Pix key WHEN reading whether it shows Pix THEN it does`() {
        // Given
        val signals = signals(hasMemberWithPixKey = true)

        // When
        val showsPix = signals.showsPix

        // Then
        assertTrue(showsPix)
    }

    @Test
    fun `GIVEN a device region of Brazil WHEN reading whether it shows Pix THEN it does whatever the case`() {
        // Given
        val regions = listOf("BR", "br", "Br")

        // When
        val results = regions.map { signals(deviceRegion = it).showsPix }

        // Then
        assertEquals(listOf(true, true, true), results)
    }

    @Test
    fun `GIVEN a device region of Portugal or the US WHEN reading whether it shows Pix THEN it does not`() {
        // Given
        val regions = listOf("PT", "US", null)

        // When
        val results = regions.map { signals(deviceRegion = it).showsPix }

        // Then
        assertEquals(listOf(false, false, false), results)
    }

    @Test
    fun `GIVEN no signal at all WHEN reading whether it shows Pix THEN it does not`() {
        // Given
        val signals = signals()

        // When
        val showsPix = signals.showsPix

        // Then
        assertFalse(showsPix)
    }

    private fun signals(
        appLanguage: AppLanguage = AppLanguage.SYSTEM,
        deviceLanguage: String? = "en-US",
        deviceRegion: String? = "US",
        hasOwnPixKey: Boolean = false,
        groupCurrencies: List<GroupCurrency> = listOf(GroupCurrency.USD),
        hasMemberWithPixKey: Boolean = false,
    ): PixSignals = PixSignals(
        appLanguage = appLanguage,
        deviceLanguage = deviceLanguage,
        deviceRegion = deviceRegion,
        hasOwnPixKey = hasOwnPixKey,
        groupCurrencies = groupCurrencies,
        hasMemberWithPixKey = hasMemberWithPixKey,
    )
}
