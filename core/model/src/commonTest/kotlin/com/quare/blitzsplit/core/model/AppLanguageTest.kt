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
    fun `GIVEN tags of languages the app has or lacks WHEN reading their language THEN it is the lower case code`() {
        // Given
        val tags = listOf("pt-BR", "PT_br", "zh-Hans-CN", "in-ID", "sv", null)

        // When
        val languages = tags.map(AppLanguage::languageOf)

        // Then
        assertEquals(listOf("pt", "pt", "zh", "id", "sv", null), languages)
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
        val tags = listOf(null, "", "sv", "sv-SE")

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
    fun `GIVEN any German tag WHEN reading it THEN is German`() {
        // Given
        val tags = listOf("de", "de-DE", "de-AT", "de_CH", "DE-li")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.GERMAN }, languages)
    }

    @Test
    fun `GIVEN any Italian tag WHEN reading it THEN is Italian`() {
        // Given
        val tags = listOf("it", "it-IT", "it-CH", "it_SM", "IT-va")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.ITALIAN }, languages)
    }

    @Test
    fun `GIVEN any Polish tag WHEN reading it THEN is Polish`() {
        // Given
        val tags = listOf("pl", "pl-PL", "pl_PL", "PL-pl")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.POLISH }, languages)
    }

    @Test
    fun `GIVEN any Dutch tag WHEN reading it THEN is Dutch`() {
        // Given
        val tags = listOf("nl", "nl-NL", "nl-BE", "nl_SR", "NL-be")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.DUTCH }, languages)
    }

    @Test
    fun `GIVEN any Japanese tag WHEN reading it THEN is Japanese`() {
        // Given
        val tags = listOf("ja", "ja-JP", "ja_JP", "JA-jp")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.JAPANESE }, languages)
    }

    @Test
    fun `GIVEN any Turkish tag WHEN reading it THEN is Turkish`() {
        // Given
        val tags = listOf("tr", "tr-TR", "tr_CY", "TR-tr")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.TURKISH }, languages)
    }

    @Test
    fun `GIVEN any Indonesian tag including the legacy in WHEN reading it THEN is Indonesian`() {
        // Given
        val tags = listOf("id", "id-ID", "in", "in_ID", "IN-id")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.INDONESIAN }, languages)
    }

    @Test
    fun `GIVEN a device in any region WHEN a language with one variant is picked THEN is the bare language`() {
        // Given
        val languages = listOf(
            AppLanguage.GERMAN,
            AppLanguage.ITALIAN,
            AppLanguage.POLISH,
            AppLanguage.DUTCH,
            AppLanguage.JAPANESE,
            AppLanguage.TURKISH,
            AppLanguage.INDONESIAN,
        )

        // When
        val tags = languages.map { it.toTag("BE") }

        // Then
        assertEquals(listOf("de", "it", "pl", "nl", "ja", "tr", "id"), tags)
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
