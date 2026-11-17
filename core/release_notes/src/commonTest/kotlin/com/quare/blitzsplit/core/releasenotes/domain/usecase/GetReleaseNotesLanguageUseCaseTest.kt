package com.quare.blitzsplit.core.releasenotes.domain.usecase

import com.quare.blitzsplit.core.releasenotes.domain.usecase.impl.GetReleaseNotesLanguageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetReleaseNotesLanguageUseCaseTest {
    private lateinit var spokenLanguage: MutableStateFlow<String>
    private lateinit var getReleaseNotesLanguage: GetReleaseNotesLanguageUseCase

    @Test
    fun `GIVEN the app is speaking Portuguese WHEN asking the language THEN it is pt`() = runTest {
        // Given
        prepareScenario(spoken = "pt")

        // When
        val language = getReleaseNotesLanguage()

        // Then
        assertEquals("pt", language)
    }

    @Test
    fun `GIVEN the app is speaking Traditional Chinese WHEN asking the language THEN it is zh-Hant`() = runTest {
        // Given
        prepareScenario(spoken = "zh-Hant")

        // When
        val language = getReleaseNotesLanguage()

        // Then
        assertEquals("zh-Hant", language)
    }

    @Test
    fun `GIVEN the app is speaking English WHEN asking the language THEN it is en`() = runTest {
        // Given
        prepareScenario(spoken = "en")

        // When
        val language = getReleaseNotesLanguage()

        // Then
        assertEquals("en", language)
    }

    @Test
    fun `GIVEN the app is speaking a language of three letters WHEN asking the language THEN it is that language`() =
        runTest {
            // Given
            prepareScenario(spoken = "fil")

            // When
            val language = getReleaseNotesLanguage()

            // Then
            assertEquals("fil", language)
        }

    @Test
    fun `GIVEN a spoken language with a region WHEN asking the language THEN it reads English`() = runTest {
        // Given
        prepareScenario(spoken = "pt-BR")

        // When
        val language = getReleaseNotesLanguage()

        // Then
        assertEquals("en", language)
    }

    @Test
    fun `GIVEN a spoken language in upper case WHEN asking the language THEN it reads English`() = runTest {
        // Given
        prepareScenario(spoken = "PT")

        // When
        val language = getReleaseNotesLanguage()

        // Then
        assertEquals("en", language)
    }

    @Test
    fun `GIVEN a spoken language that is a path WHEN asking the language THEN it reads English`() = runTest {
        // Given
        prepareScenario(spoken = "../x")

        // When
        val language = getReleaseNotesLanguage()

        // Then
        assertEquals("en", language)
    }

    @Test
    fun `GIVEN a spoken language of one letter WHEN asking the language THEN it reads English`() = runTest {
        // Given
        prepareScenario(spoken = "p")

        // When
        val language = getReleaseNotesLanguage()

        // Then
        assertEquals("en", language)
    }

    @Test
    fun `GIVEN a spoken language of four letters WHEN asking the language THEN it reads English`() = runTest {
        // Given
        prepareScenario(spoken = "port")

        // When
        val language = getReleaseNotesLanguage()

        // Then
        assertEquals("en", language)
    }

    @Test
    fun `GIVEN an empty spoken language WHEN asking the language THEN it reads English`() = runTest {
        // Given
        prepareScenario(spoken = "")

        // When
        val language = getReleaseNotesLanguage()

        // Then
        assertEquals("en", language)
    }

    @Test
    fun `GIVEN a spoken language with digits a space a dot or a line break WHEN asking each THEN each reads English`() =
        runTest {
            // Given
            val spoken = listOf("p1", "12", " pt", "pt ", "pt.json", "pt\n", "pt/", "p_t", "pt?x", "ptã")
            prepareScenario(spoken = "pt")

            // When
            val languages = spoken.associateWith { language ->
                spokenLanguage.value = language
                getReleaseNotesLanguage()
            }

            // Then
            assertEquals(spoken.associateWith { "en" }, languages)
        }

    private fun prepareScenario(spoken: String) {
        spokenLanguage = MutableStateFlow(spoken)
        getReleaseNotesLanguage = GetReleaseNotesLanguageUseCase(observeSpokenLanguage = { spokenLanguage })
    }
}
