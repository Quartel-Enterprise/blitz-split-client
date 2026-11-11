package com.quare.blitzsplit.core.legal.domain.usecase

import com.quare.blitzsplit.core.legal.domain.usecase.impl.RefreshLegalDocumentUseCase
import com.quare.blitzsplit.core.model.LegalPage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RefreshLegalDocumentUseCaseTest {
    private lateinit var repository: FakeLegalRepository
    private lateinit var refreshLegalDocument: RefreshLegalDocumentUseCase

    @Test
    fun `GIVEN Portuguese as the language WHEN refreshing the privacy policy THEN downloads that page in Portuguese`() =
        runTest {
            // Given
            prepareScenario(language = "pt")

            // When
            val result = refreshLegalDocument(LegalPage.PRIVACY_POLICY)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(LegalPage.PRIVACY_POLICY to "pt"), repository.refreshed)
        }

    @Test
    fun `GIVEN Turkish as the language WHEN refreshing the terms THEN downloads the terms in Turkish`() = runTest {
        // Given
        prepareScenario(language = "tr")

        // When
        val result = refreshLegalDocument(LegalPage.TERMS_OF_USE)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(LegalPage.TERMS_OF_USE to "tr"), repository.refreshed)
    }

    @Test
    fun `GIVEN the download fails WHEN refreshing THEN gives the failure`() = runTest {
        // Given
        prepareScenario(
            language = "pt",
            refreshResult = Result.failure(IllegalStateException("offline")),
        )

        // When
        val result = refreshLegalDocument(LegalPage.TERMS_OF_USE)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun prepareScenario(
        language: String,
        refreshResult: Result<Unit> = Result.success(Unit),
    ) {
        repository = FakeLegalRepository().also { it.refreshResult = refreshResult }
        refreshLegalDocument = RefreshLegalDocumentUseCase(
            repository = repository,
            getLegalLanguage = { language },
        )
    }
}
