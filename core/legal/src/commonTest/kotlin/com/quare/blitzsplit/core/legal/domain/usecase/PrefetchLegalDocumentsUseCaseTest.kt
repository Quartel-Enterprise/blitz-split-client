package com.quare.blitzsplit.core.legal.domain.usecase

import com.quare.blitzsplit.core.legal.domain.usecase.impl.PrefetchLegalDocumentsUseCase
import com.quare.blitzsplit.core.model.LegalPage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrefetchLegalDocumentsUseCaseTest {
    private lateinit var repository: FakeLegalRepository
    private lateinit var prefetch: PrefetchLegalDocumentsUseCase

    @Test
    fun `GIVEN an app that draws the texts in Turkish WHEN prefetching THEN refreshes both texts in Turkish`() =
        runTest {
            // Given
            prepareScenario(language = "tr")

            // When
            prefetch()

            // Then
            assertEquals(
                listOf(LegalPage.PRIVACY_POLICY to "tr", LegalPage.TERMS_OF_USE to "tr"),
                repository.refreshed,
            )
        }

    @Test
    fun `GIVEN no internet WHEN prefetching THEN still tries the terms after the privacy policy fails`() = runTest {
        // Given
        prepareScenario(language = "pt")
        repository.refreshResult = Result.failure(IllegalStateException("offline"))

        // When
        prefetch()

        // Then
        assertEquals(
            listOf(LegalPage.PRIVACY_POLICY to "pt", LegalPage.TERMS_OF_USE to "pt"),
            repository.refreshed,
        )
    }

    @Test
    fun `GIVEN the web that opens the site pages WHEN prefetching THEN downloads nothing`() = runTest {
        // Given
        prepareScenario(
            language = "pt",
            drawsTextsInApp = false,
        )

        // When
        prefetch()

        // Then
        assertTrue(repository.refreshed.isEmpty())
    }

    private fun prepareScenario(
        language: String,
        drawsTextsInApp: Boolean = true,
    ) {
        repository = FakeLegalRepository()
        prefetch = PrefetchLegalDocumentsUseCase(
            drawsTextsInApp = drawsTextsInApp,
            getLegalLanguage = { language },
            repository = repository,
        )
    }
}
