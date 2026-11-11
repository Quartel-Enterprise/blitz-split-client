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
    fun `GIVEN the site up WHEN refreshing the privacy policy in Portuguese THEN downloads that page in Portuguese`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val result = refreshLegalDocument(
                page = LegalPage.PRIVACY_POLICY,
                language = "pt",
            )

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(LegalPage.PRIVACY_POLICY to "pt"), repository.refreshed)
        }

    @Test
    fun `GIVEN the site up WHEN refreshing the terms in Turkish THEN downloads the terms in Turkish`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = refreshLegalDocument(
            page = LegalPage.TERMS_OF_USE,
            language = "tr",
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(LegalPage.TERMS_OF_USE to "tr"), repository.refreshed)
    }

    @Test
    fun `GIVEN the download fails WHEN refreshing THEN gives the failure`() = runTest {
        // Given
        prepareScenario(refreshResult = Result.failure(IllegalStateException("offline")))

        // When
        val result = refreshLegalDocument(
            page = LegalPage.TERMS_OF_USE,
            language = "pt",
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun prepareScenario(refreshResult: Result<Unit> = Result.success(Unit)) {
        repository = FakeLegalRepository().also { it.refreshResult = refreshResult }
        refreshLegalDocument = RefreshLegalDocumentUseCase(repository)
    }
}
