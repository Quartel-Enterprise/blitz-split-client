package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {
    @Test
    fun `GIVEN any Portuguese tag the platform kept WHEN reading it THEN is Portuguese`() {
        // Given
        val tags = listOf("pt", "pt-BR", "pt-PT", "pt_BR", "PT-br")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.PORTUGUESE }, languages)
    }

    @Test
    fun `GIVEN an English tag WHEN reading it THEN is English`() {
        // Given
        val tag = "en-GB"

        // When
        val language = AppLanguage.fromTag(tag)

        // Then
        assertEquals(AppLanguage.ENGLISH, language)
    }

    @Test
    fun `GIVEN no tag or a language the app does not have WHEN reading it THEN follows the device`() {
        // Given
        val tags = listOf(null, "", "de", "it-IT")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.SYSTEM }, languages)
    }

    @Test
    fun `GIVEN any Spanish tag WHEN reading it THEN is Spanish`() {
        // Given
        val tags = listOf("es", "es-ES", "es-MX", "es-419")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.SPANISH }, languages)
    }

    @Test
    fun `GIVEN any French tag WHEN reading it THEN is French`() {
        // Given
        val tags = listOf("fr", "fr-FR", "fr-CA", "fr_BE", "FR-ch")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.FRENCH }, languages)
    }

    @Test
    fun `GIVEN a device in any region WHEN French is picked THEN is the bare French`() {
        // Given
        val regions = listOf("FR", "CA", "ES", null)

        // When
        val tags = regions.map(AppLanguage.FRENCH::toTag)

        // Then
        assertEquals(listOf("fr", "fr", "fr", "fr"), tags)
    }

    @Test
    fun `GIVEN a device in Spain WHEN Spanish is picked THEN keeps the Spanish of Spain`() {
        // Given
        val region = "ES"

        // When
        val tag = AppLanguage.SPANISH.toTag(region)

        // Then
        assertEquals("es-ES", tag)
    }

    @Test
    fun `GIVEN a device anywhere else or with no region WHEN Spanish is picked THEN is the neutral Spanish`() {
        // Given
        val regions = listOf("MX", "BR", null)

        // When
        val tags = regions.map(AppLanguage.SPANISH::toTag)

        // Then
        assertEquals(listOf("es", "es", "es"), tags)
    }

    @Test
    fun `GIVEN a device in Spain WHEN another language is picked THEN its tag stays`() {
        // Given
        val languages = listOf(AppLanguage.SYSTEM, AppLanguage.ENGLISH, AppLanguage.PORTUGUESE)

        // When
        val tags = languages.map { it.toTag("ES") }

        // Then
        assertEquals(listOf(null, "en", "pt-BR"), tags)
    }
}
