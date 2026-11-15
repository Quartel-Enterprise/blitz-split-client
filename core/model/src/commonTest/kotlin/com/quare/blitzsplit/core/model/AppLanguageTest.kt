package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {
    @Test
    fun `GIVEN a Portuguese tag of Brazil or with no region WHEN reading it THEN is the Portuguese of Brazil`() {
        // Given
        val tags = listOf("pt", "pt-BR", "pt_BR", "PT-br", "pt-AO", "pt-u-fw-mon")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.PORTUGUESE }, languages)
    }

    @Test
    fun `GIVEN a Portuguese tag of Portugal WHEN reading it THEN is the Portuguese of Portugal`() {
        // Given
        val tags = listOf("pt-PT", "pt_PT", "PT-pt", "pt-PT-u-fw-mon", "pt-Latn-PT")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.PORTUGUESE_PORTUGAL }, languages)
    }

    @Test
    fun `GIVEN tags with and without a region WHEN reading their region THEN it is the upper case code or null`() {
        // Given
        val tags = listOf("pt-PT", "pt_br", "zh-Hans-CN", "pt-u-fw-mon", "pt", "es-419", null)

        // When
        val regions = tags.map(AppLanguage::regionOf)

        // Then
        assertEquals(listOf("PT", "BR", "CN", null, null, null, null), regions)
    }

    @Test
    fun `GIVEN the two Portuguese and the two Spanish WHEN drawing the picker chips THEN each shows its region`() {
        // Given
        val languages = listOf(
            AppLanguage.SYSTEM,
            AppLanguage.ENGLISH,
            AppLanguage.PORTUGUESE,
            AppLanguage.PORTUGUESE_PORTUGAL,
            AppLanguage.SPANISH,
            AppLanguage.SPANISH_SPAIN,
        )

        // When
        val codes = languages.map { it.chipCode }

        // Then
        assertEquals(listOf(null, "EN", "BR", "PT", "419", "ES"), codes)
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
    fun `GIVEN a Spanish tag not of Spain WHEN reading it THEN is the Latin American Spanish`() {
        // Given
        val tags = listOf("es", "es-MX", "es_AR", "ES-us", "es-419", "es-u-fw-mon")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.SPANISH }, languages)
    }

    @Test
    fun `GIVEN a Spanish tag of Spain WHEN reading it THEN is the Spanish of Spain`() {
        // Given
        val tags = listOf("es-ES", "es_ES", "ES-es", "es-ES-u-fw-mon")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.SPANISH_SPAIN }, languages)
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
    fun `GIVEN a Portuguese tag of Portugal in other spellings WHEN reading it THEN is the Portuguese of Portugal`() {
        // Given
        val tags = listOf("pt-PT-x-private", "PT-PT", "pt-pt", "pt_pt_x_private", "pt-Latn-pt-u-fw-mon")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.PORTUGUESE_PORTUGAL }, languages)
    }

    @Test
    fun `GIVEN a Portuguese tag with no region or an odd one WHEN reading it THEN is the Portuguese of Brazil`() {
        // Given
        val tags = listOf("PT", "pt-", "pt--PT", "pt-419", "pt-Latn", "pt-Latn-BR", "pt-x-PT", "pt-PTT")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.PORTUGUESE }, languages)
    }

    @Test
    fun `GIVEN every language WHEN its tag is read back THEN is that same language`() {
        // Given
        val languages = AppLanguage.entries

        // When
        val readBack = languages.map { AppLanguage.fromTag(it.tag) }

        // Then
        assertEquals(languages, readBack)
    }

    @Test
    fun `GIVEN every language WHEN drawing the picker chips THEN each shows its code or region and the system none`() {
        // Given
        val languages = AppLanguage.entries

        // When
        val codes = languages.map { it.chipCode }

        // Then
        assertEquals(
            listOf(null, "EN", "BR", "PT", "419", "ES", "FR", "DE", "IT", "PL", "NL", "JA", "TR", "ID"),
            codes,
        )
        assertEquals(codes.filterNotNull().size, codes.filterNotNull().distinct().size)
    }

    @Test
    fun `GIVEN odd tags WHEN reading their region THEN is a two letter one before any extension or null`() {
        // Given
        val tags = listOf("", "pt-", "pt--PT", "pt-x-PT", "pt-Latn", "pt-PT-x-BR", "PT_pt", "pt-Latn-br-u-fw-mon")

        // When
        val regions = tags.map(AppLanguage::regionOf)

        // Then
        assertEquals(listOf(null, null, null, null, null, "PT", "PT", "BR"), regions)
    }
}
