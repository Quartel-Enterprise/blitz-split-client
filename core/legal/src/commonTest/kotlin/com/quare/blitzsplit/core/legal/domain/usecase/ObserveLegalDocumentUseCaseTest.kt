package com.quare.blitzsplit.core.legal.domain.usecase

import com.quare.blitzsplit.core.legal.domain.model.LegalDate
import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.legal.domain.usecase.impl.ObserveLegalDocumentUseCase
import com.quare.blitzsplit.core.model.LegalPage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveLegalDocumentUseCaseTest {
    private lateinit var repository: FakeLegalRepository
    private lateinit var observeLegalDocument: ObserveLegalDocumentUseCase

    @Test
    fun `GIVEN the terms saved in Portuguese and Portuguese as the language WHEN observing them THEN gives them`() =
        runTest {
            // Given
            prepareScenario(
                language = "pt",
                saved = mapOf((LegalPage.TERMS_OF_USE to "pt") to termsInPortuguese),
            )

            // When
            val document = observeLegalDocument(LegalPage.TERMS_OF_USE).first()

            // Then
            assertEquals(termsInPortuguese, document)
            assertEquals(listOf(LegalPage.TERMS_OF_USE to "pt"), repository.observed)
        }

    @Test
    fun `GIVEN the terms saved in Portuguese and Spanish as the language WHEN observing them THEN there is no text`() =
        runTest {
            // Given
            prepareScenario(
                language = "es",
                saved = mapOf((LegalPage.TERMS_OF_USE to "pt") to termsInPortuguese),
            )

            // When
            val document = observeLegalDocument(LegalPage.TERMS_OF_USE).first()

            // Then
            assertNull(document)
            assertEquals(listOf(LegalPage.TERMS_OF_USE to "es"), repository.observed)
        }

    @Test
    fun `GIVEN only the terms saved WHEN observing the privacy policy THEN there is no text`() = runTest {
        // Given
        prepareScenario(
            language = "pt",
            saved = mapOf((LegalPage.TERMS_OF_USE to "pt") to termsInPortuguese),
        )

        // When
        val document = observeLegalDocument(LegalPage.PRIVACY_POLICY).first()

        // Then
        assertNull(document)
        assertEquals(listOf(LegalPage.PRIVACY_POLICY to "pt"), repository.observed)
    }

    @Test
    fun `GIVEN the page is open with nothing saved WHEN the text is saved THEN the page gets it`() =
        runTest(UnconfinedTestDispatcher()) {
            // Given
            prepareScenario(
                language = "pt",
                saved = emptyMap(),
            )
            val documents = mutableListOf<LegalDocument?>()
            backgroundScope.launch { observeLegalDocument(LegalPage.TERMS_OF_USE).collect(documents::add) }

            // When
            repository.documents.update { it + ((LegalPage.TERMS_OF_USE to "pt") to termsInPortuguese) }

            // Then
            assertEquals(listOf(null, termsInPortuguese), documents)
        }

    private val termsInPortuguese =
        LegalDocument(
            title = "Termos",
            updated = LegalDate(2026, 11, 5),
            blocks = emptyList(),
        )

    private fun prepareScenario(
        language: String,
        saved: Map<Pair<LegalPage, String>, LegalDocument>,
    ) {
        repository = FakeLegalRepository().also { it.documents.value = saved }
        observeLegalDocument = ObserveLegalDocumentUseCase(
            repository = repository,
            getLegalLanguage = { language },
        )
    }
}
