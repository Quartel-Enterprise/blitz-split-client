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
        val tags = listOf(null, "", "es-ES", "fr")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.SYSTEM }, languages)
    }
}
