package com.quare.blitzsplit.core.legal.data

import com.quare.blitzsplit.core.legal.data.datasource.LegalLocalDataSource
import com.quare.blitzsplit.core.legal.data.datasource.LegalRemoteDataSource
import com.quare.blitzsplit.core.legal.data.parser.LegalDocumentParserImpl
import com.quare.blitzsplit.core.legal.data.repository.LegalRepositoryImpl
import com.quare.blitzsplit.core.legal.domain.model.LegalDate
import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.model.LegalPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LegalRepositoryImplTest {
    private lateinit var local: FakeLegalLocalDataSource
    private lateinit var remote: FakeLegalRemoteDataSource
    private lateinit var repository: LegalRepositoryImpl

    @Test
    fun `GIVEN nothing was ever downloaded WHEN observing the privacy policy THEN there is no document`() = runTest {
        // Given
        prepareScenario()

        // When
        val document = repository.observeDocument(LegalPage.PRIVACY_POLICY, "pt").first()

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN the privacy policy was saved in Portuguese WHEN observing it offline THEN shows the saved text`() =
        runTest {
            // Given
            prepareScenario(
                saved = mapOf((LegalPage.PRIVACY_POLICY to "pt") to markdown(title = "Privacidade")),
                siteError = IllegalStateException("offline"),
            )

            // When
            val document = repository.observeDocument(LegalPage.PRIVACY_POLICY, "pt").first()

            // Then
            assertEquals(
                LegalDocument(
                    title = "Privacidade",
                    updated = LegalDate(2026, 11, 5),
                    blocks = emptyList(),
                ),
                document,
            )
            assertTrue(remote.requests.isEmpty())
        }

    @Test
    fun `GIVEN only the privacy policy was saved WHEN observing the terms THEN there is no document`() = runTest {
        // Given
        prepareScenario(saved = mapOf((LegalPage.PRIVACY_POLICY to "pt") to markdown(title = "Privacidade")))

        // When
        val document = repository.observeDocument(LegalPage.TERMS_OF_USE, "pt").first()

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN the terms were saved in Portuguese only WHEN observing them in Spanish THEN there is no document`() =
        runTest {
            // Given
            prepareScenario(saved = mapOf((LegalPage.TERMS_OF_USE to "pt") to markdown(title = "Termos")))

            // When
            val document = repository.observeDocument(LegalPage.TERMS_OF_USE, "es").first()

            // Then
            assertNull(document)
        }

    @Test
    fun `GIVEN the saved text is not a legal text WHEN observing it THEN there is no document`() = runTest {
        // Given
        prepareScenario(saved = mapOf((LegalPage.TERMS_OF_USE to "en") to "<html>Bad gateway</html>"))

        // When
        val document = repository.observeDocument(LegalPage.TERMS_OF_USE, "en").first()

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN the page is open with an old text WHEN a refresh downloads a new one THEN the page gets the new text`() =
        runTest(UnconfinedTestDispatcher()) {
            // Given
            prepareScenario(
                saved = mapOf((LegalPage.TERMS_OF_USE to "en") to markdown(title = "Old Terms")),
                site = mapOf("en" to markdown(title = "New Terms")),
            )
            val titles = mutableListOf<String?>()
            backgroundScope.launch {
                repository.observeDocument(LegalPage.TERMS_OF_USE, "en").collect { titles += it?.title }
            }

            // When
            repository.refreshDocument(LegalPage.TERMS_OF_USE, "en")

            // Then
            assertEquals(listOf<String?>("Old Terms", "New Terms"), titles)
        }

    @Test
    fun `GIVEN the site has the text in Portuguese WHEN refreshing in Portuguese THEN saves it under Portuguese`() =
        runTest {
            // Given
            prepareScenario(site = mapOf("pt" to markdown(title = "Privacidade"), "en" to markdown(title = "Privacy")))

            // When
            val result = repository.refreshDocument(LegalPage.PRIVACY_POLICY, "pt")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                mapOf((LegalPage.PRIVACY_POLICY to "pt") to markdown(title = "Privacidade")),
                local.saved.value,
            )
            assertEquals(listOf(LegalPage.PRIVACY_POLICY to "pt"), remote.requests)
        }

    @Test
    fun `GIVEN the site has no French text WHEN refreshing in French THEN saves the English text under French`() =
        runTest {
            // Given
            prepareScenario(site = mapOf("en" to markdown(title = "Terms of Service")))

            // When
            val result = repository.refreshDocument(LegalPage.TERMS_OF_USE, "fr")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                mapOf((LegalPage.TERMS_OF_USE to "fr") to markdown(title = "Terms of Service")),
                local.saved.value,
            )
            assertEquals(listOf(LegalPage.TERMS_OF_USE to "fr", LegalPage.TERMS_OF_USE to "en"), remote.requests)
        }

    @Test
    fun `GIVEN the site had no French text WHEN observing in French after a refresh THEN shows the English text`() =
        runTest {
            // Given
            prepareScenario(site = mapOf("en" to markdown(title = "Terms of Service")))
            repository.refreshDocument(LegalPage.TERMS_OF_USE, "fr")

            // When
            val document = repository.observeDocument(LegalPage.TERMS_OF_USE, "fr").first()

            // Then
            assertEquals("Terms of Service", document?.title)
        }

    @Test
    fun `GIVEN the site has neither the language nor English WHEN refreshing THEN fails and keeps the saved text`() =
        runTest {
            // Given
            val saved = mapOf((LegalPage.PRIVACY_POLICY to "fr") to markdown(title = "Privacy"))
            prepareScenario(
                saved = saved,
                site = emptyMap(),
            )

            // When
            val result = repository.refreshDocument(LegalPage.PRIVACY_POLICY, "fr")

            // Then
            assertTrue(result.isFailure)
            assertEquals(saved, local.saved.value)
            assertEquals(0, local.saveCount)
        }

    @Test
    fun `GIVEN the site has no English text WHEN refreshing in English THEN fails and saves nothing`() = runTest {
        // Given
        prepareScenario(site = mapOf("pt" to markdown(title = "Privacidade")))

        // When
        val result = repository.refreshDocument(LegalPage.PRIVACY_POLICY, "en")

        // Then
        assertTrue(result.isFailure)
        assertEquals(0, local.saveCount)
    }

    @Test
    fun `GIVEN the site has no English text WHEN refreshing in English THEN asks the site only once`() = runTest {
        // Given
        prepareScenario(site = mapOf("pt" to markdown(title = "Privacidade")))

        // When
        repository.refreshDocument(LegalPage.PRIVACY_POLICY, "en")

        // Then
        assertEquals(listOf(LegalPage.PRIVACY_POLICY to "en"), remote.requests)
    }

    @Test
    fun `GIVEN the site answers a page that is not a legal text WHEN refreshing THEN fails and keeps the saved text`() =
        runTest {
            // Given
            val saved = mapOf((LegalPage.PRIVACY_POLICY to "pt") to markdown(title = "Privacidade"))
            prepareScenario(
                saved = saved,
                site = mapOf("pt" to "<html>Sign in to the hotel wifi</html>"),
            )

            // When
            val result = repository.refreshDocument(LegalPage.PRIVACY_POLICY, "pt")

            // Then
            assertTrue(result.isFailure)
            assertEquals(saved, local.saved.value)
            assertEquals(0, local.saveCount)
        }

    @Test
    fun `GIVEN the English fallback is not a legal text WHEN refreshing in French THEN fails and saves nothing`() =
        runTest {
            // Given
            prepareScenario(site = mapOf("en" to "Not found"))

            // When
            val result = repository.refreshDocument(LegalPage.TERMS_OF_USE, "fr")

            // Then
            assertTrue(result.isFailure)
            assertEquals(0, local.saveCount)
        }

    @Test
    fun `GIVEN the site answers French with a page that is not a legal text WHEN refreshing THEN saves the English`() =
        runTest {
            // Given
            prepareScenario(
                site = mapOf(
                    "fr" to "<!doctype html><html><body>Blitz Split</body></html>",
                    "en" to markdown(title = "Terms of Service"),
                ),
            )

            // When
            val result = repository.refreshDocument(LegalPage.TERMS_OF_USE, "fr")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                mapOf((LegalPage.TERMS_OF_USE to "fr") to markdown(title = "Terms of Service")),
                local.saved.value,
            )
        }

    @Test
    fun `GIVEN no internet WHEN refreshing THEN fails with the error and keeps the saved text`() = runTest {
        // Given
        val saved = mapOf((LegalPage.TERMS_OF_USE to "pt") to markdown(title = "Termos"))
        prepareScenario(
            saved = saved,
            siteError = IllegalStateException("offline"),
        )

        // When
        val result = repository.refreshDocument(LegalPage.TERMS_OF_USE, "pt")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("offline", result.exceptionOrNull()?.message)
        assertEquals(saved, local.saved.value)
        assertEquals(0, local.saveCount)
    }

    @Test
    fun `GIVEN the refresh is cancelled WHEN refreshing THEN the cancellation goes through`() = runTest {
        // Given
        prepareScenario(siteError = CancellationException("left the page"))

        // When
        val failure = assertFailsWith<CancellationException> {
            repository.refreshDocument(LegalPage.TERMS_OF_USE, "pt")
        }

        // Then
        assertEquals("left the page", failure.message)
        assertEquals(0, local.saveCount)
    }

    private fun markdown(title: String): String = "---\ntitle: $title\nupdated: 2026-11-05\n---\n"

    private fun prepareScenario(
        saved: Map<Pair<LegalPage, String>, String> = emptyMap(),
        site: Map<String, String> = emptyMap(),
        siteError: Throwable? = null,
    ) {
        local = FakeLegalLocalDataSource(saved)
        remote = FakeLegalRemoteDataSource(
            site = site,
            error = siteError,
        )
        repository = LegalRepositoryImpl(
            local = local,
            remote = remote,
            parser = LegalDocumentParserImpl(),
        )
    }
}

private class FakeLegalLocalDataSource(
    initial: Map<Pair<LegalPage, String>, String>,
) : LegalLocalDataSource {
    val saved = MutableStateFlow(initial)
    var saveCount = 0

    override fun observeMarkdown(
        page: LegalPage,
        language: String,
    ): Flow<String?> = saved.map { it[page to language] }

    override suspend fun saveMarkdown(
        page: LegalPage,
        language: String,
        markdown: String,
    ) {
        saveCount++
        saved.update { it + ((page to language) to markdown) }
    }
}

// Why: site holds the languages the site writes the texts in; any other answers 404, which is null here.
private class FakeLegalRemoteDataSource(
    private val site: Map<String, String>,
    private val error: Throwable?,
) : LegalRemoteDataSource {
    val requests = mutableListOf<Pair<LegalPage, String>>()

    override suspend fun fetchMarkdown(
        page: LegalPage,
        language: String,
    ): String? {
        requests += page to language
        error?.let { throw it }
        return site[language]
    }
}
