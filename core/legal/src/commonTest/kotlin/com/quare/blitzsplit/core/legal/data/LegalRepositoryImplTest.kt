package com.quare.blitzsplit.core.legal.data

import com.quare.blitzsplit.core.legal.data.datasource.LegalLocalDataSource
import com.quare.blitzsplit.core.legal.data.datasource.LegalRemoteAnswer
import com.quare.blitzsplit.core.legal.data.datasource.LegalRemoteDataSource
import com.quare.blitzsplit.core.legal.data.datasource.SavedLegalText
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

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

    @Test
    fun `GIVEN the terms were checked an hour ago WHEN refreshing them THEN succeeds without asking the site`() =
        runTest {
            // Given
            prepareScenario(
                saved = mapOf((LegalPage.TERMS_OF_USE to "pt") to markdown(title = "Termos")),
                savedChecks = mapOf((LegalPage.TERMS_OF_USE to "pt") to SavedLegalText("\"a\"", NOW - 1.hours.ms)),
                site = mapOf("pt" to markdown(title = "Termos novos")),
            )

            // When
            val result = repository.refreshDocument(LegalPage.TERMS_OF_USE, "pt")

            // Then
            assertTrue(result.isSuccess)
            assertTrue(remote.requests.isEmpty())
            assertEquals(0, local.saveCount)
        }

    @Test
    fun `GIVEN the terms were checked yesterday and are unchanged WHEN refreshing THEN only their check time moves`() =
        runTest {
            // Given
            val saved = mapOf((LegalPage.TERMS_OF_USE to "pt") to markdown(title = "Termos"))
            prepareScenario(
                saved = saved,
                savedChecks = mapOf((LegalPage.TERMS_OF_USE to "pt") to SavedLegalText("\"a\"", NOW - 25.hours.ms)),
                site = mapOf("pt" to markdown(title = "Termos")),
                siteEtags = mapOf("pt" to "\"a\""),
            )

            // When
            val result = repository.refreshDocument(LegalPage.TERMS_OF_USE, "pt")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf<String?>("\"a\""), remote.sentEtags)
            assertEquals(saved, local.saved.value)
            assertEquals(0, local.saveCount)
            assertEquals(SavedLegalText("\"a\"", NOW), local.checks[LegalPage.TERMS_OF_USE to "pt"])
        }

    @Test
    fun `GIVEN the terms were checked yesterday and changed WHEN refreshing THEN saves the new text and its ETag`() =
        runTest {
            // Given
            prepareScenario(
                saved = mapOf((LegalPage.TERMS_OF_USE to "pt") to markdown(title = "Termos")),
                savedChecks = mapOf((LegalPage.TERMS_OF_USE to "pt") to SavedLegalText("\"a\"", NOW - 25.hours.ms)),
                site = mapOf("pt" to markdown(title = "Termos novos")),
                siteEtags = mapOf("pt" to "\"b\""),
            )

            // When
            repository.refreshDocument(LegalPage.TERMS_OF_USE, "pt")

            // Then
            assertEquals(
                mapOf((LegalPage.TERMS_OF_USE to "pt") to markdown(title = "Termos novos")),
                local.saved.value,
            )
            assertEquals(SavedLegalText("\"b\"", NOW), local.checks[LegalPage.TERMS_OF_USE to "pt"])
        }

    @Test
    fun `GIVEN the device clock was set back since the check WHEN refreshing THEN asks the site`() = runTest {
        // Given
        prepareScenario(
            saved = mapOf((LegalPage.TERMS_OF_USE to "pt") to markdown(title = "Termos")),
            savedChecks = mapOf((LegalPage.TERMS_OF_USE to "pt") to SavedLegalText("\"a\"", NOW + 1.hours.ms)),
            site = mapOf("pt" to markdown(title = "Termos")),
            siteEtags = mapOf("pt" to "\"a\""),
        )

        // When
        repository.refreshDocument(LegalPage.TERMS_OF_USE, "pt")

        // Then
        assertEquals(listOf(LegalPage.TERMS_OF_USE to "pt"), remote.requests)
    }

    @Test
    fun `GIVEN French kept the unchanged English text WHEN refreshing in French THEN English answers it unchanged`() =
        runTest {
            // Given
            prepareScenario(
                saved = mapOf((LegalPage.PRIVACY_POLICY to "fr") to markdown(title = "Privacy")),
                savedChecks = mapOf((LegalPage.PRIVACY_POLICY to "fr") to SavedLegalText("\"en\"", NOW - 25.hours.ms)),
                site = mapOf("en" to markdown(title = "Privacy")),
                siteEtags = mapOf("en" to "\"en\""),
            )

            // When
            val result = repository.refreshDocument(LegalPage.PRIVACY_POLICY, "fr")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(LegalPage.PRIVACY_POLICY to "fr", LegalPage.PRIVACY_POLICY to "en"), remote.requests)
            assertEquals(0, local.saveCount)
            assertEquals(SavedLegalText("\"en\"", NOW), local.checks[LegalPage.PRIVACY_POLICY to "fr"])
        }

    @Test
    fun `GIVEN the privacy policy was just downloaded WHEN refreshing it again THEN the site is asked once`() =
        runTest {
            // Given
            prepareScenario(site = mapOf("pt" to markdown(title = "Privacidade")))
            repository.refreshDocument(LegalPage.PRIVACY_POLICY, "pt")

            // When
            val result = repository.refreshDocument(LegalPage.PRIVACY_POLICY, "pt")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(LegalPage.PRIVACY_POLICY to "pt"), remote.requests)
        }

    @Test
    fun `GIVEN a text checked yesterday WHEN refreshing offline THEN fails and keeps its check time`() = runTest {
        // Given
        val check = SavedLegalText("\"a\"", NOW - 25.hours.ms)
        prepareScenario(
            saved = mapOf((LegalPage.TERMS_OF_USE to "pt") to markdown(title = "Termos")),
            savedChecks = mapOf((LegalPage.TERMS_OF_USE to "pt") to check),
            siteError = IllegalStateException("offline"),
        )

        // When
        val result = repository.refreshDocument(LegalPage.TERMS_OF_USE, "pt")

        // Then
        assertTrue(result.isFailure)
        assertEquals(check, local.checks[LegalPage.TERMS_OF_USE to "pt"])
    }

    private fun markdown(title: String): String = "---\ntitle: $title\nupdated: 2026-11-05\n---\n"

    private fun prepareScenario(
        saved: Map<Pair<LegalPage, String>, String> = emptyMap(),
        savedChecks: Map<Pair<LegalPage, String>, SavedLegalText> = emptyMap(),
        site: Map<String, String> = emptyMap(),
        siteEtags: Map<String, String> = emptyMap(),
        siteError: Throwable? = null,
    ) {
        local = FakeLegalLocalDataSource(
            initial = saved,
            initialChecks = savedChecks,
        )
        remote = FakeLegalRemoteDataSource(
            site = site,
            etags = siteEtags,
            error = siteError,
        )
        repository = LegalRepositoryImpl(
            local = local,
            remote = remote,
            parser = LegalDocumentParserImpl(),
            currentTimeMillis = { NOW },
        )
    }

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}

private val Duration.ms: Long
    get() = inWholeMilliseconds

/*
 * Why: saved holds each text's Markdown and checks its ETag and check time; a text saved without a check is one the
 * app kept before it knew when, so it is stale.
 */
private class FakeLegalLocalDataSource(
    initial: Map<Pair<LegalPage, String>, String>,
    initialChecks: Map<Pair<LegalPage, String>, SavedLegalText>,
) : LegalLocalDataSource {
    val saved = MutableStateFlow(initial)
    val checks = initialChecks.toMutableMap()
    var saveCount = 0

    override fun observeMarkdown(
        page: LegalPage,
        language: String,
    ): Flow<String?> = saved.map { it[page to language] }

    override suspend fun getSavedText(
        page: LegalPage,
        language: String,
    ): SavedLegalText? {
        if (page to language !in saved.value) return null
        return checks[page to language] ?: SavedLegalText(
            etag = null,
            checkedAt = null,
        )
    }

    override suspend fun saveMarkdown(
        page: LegalPage,
        language: String,
        markdown: String,
        etag: String?,
        checkedAt: Long,
    ) {
        saveCount++
        saved.update { it + ((page to language) to markdown) }
        checks[page to language] = SavedLegalText(etag, checkedAt)
    }

    override suspend fun markChecked(
        page: LegalPage,
        language: String,
        checkedAt: Long,
    ) {
        checks[page to language] = SavedLegalText(
            etag = checks[page to language]?.etag,
            checkedAt = checkedAt,
        )
    }
}

/*
 * Why: site holds the languages the site writes the texts in, any other answering 404; etags their ETags, a matching
 * one answering 304.
 */
private class FakeLegalRemoteDataSource(
    private val site: Map<String, String>,
    private val etags: Map<String, String>,
    private val error: Throwable?,
) : LegalRemoteDataSource {
    val requests = mutableListOf<Pair<LegalPage, String>>()
    val sentEtags = mutableListOf<String?>()

    override suspend fun fetchMarkdown(
        page: LegalPage,
        language: String,
        etag: String?,
    ): LegalRemoteAnswer {
        requests += page to language
        sentEtags += etag
        error?.let { throw it }
        val markdown = site[language] ?: return LegalRemoteAnswer.Missing
        if (etag != null && etag == etags[language]) return LegalRemoteAnswer.Unchanged
        return LegalRemoteAnswer.Text(
            markdown = markdown,
            etag = etags[language],
        )
    }
}
