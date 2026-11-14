package com.quare.blitzsplit.core.legal.data.datasource

import com.quare.blitzsplit.core.model.LegalPage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SiteLegalRemoteDataSourceTest {
    private lateinit var requests: MutableList<Pair<HttpMethod, String>>
    private val sentEtags = mutableListOf<String?>()
    private lateinit var dataSource: SiteLegalRemoteDataSource

    @Test
    fun `GIVEN the site has the privacy policy in Portuguese WHEN fetching it THEN gets its file and its text`() =
        runTest {
            // Given
            prepareScenario(
                status = HttpStatusCode.OK,
                body = "---\ntitle: Privacidade\n---\n",
            )

            // When
            val answer = dataSource.fetchMarkdown(LegalPage.PRIVACY_POLICY, "pt", etag = null)

            // Then
            assertEquals(
                LegalRemoteAnswer.Text(
                    markdown = "---\ntitle: Privacidade\n---\n",
                    etag = null,
                ),
                answer,
            )
            assertEquals(listOf(HttpMethod.Get to "https://blitzsplit.com/legal/privacy.pt.md"), requests)
        }

    @Test
    fun `GIVEN the site has the terms in English WHEN fetching them THEN gets the file of the terms`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.OK,
            body = "terms",
        )

        // When
        val answer = dataSource.fetchMarkdown(LegalPage.TERMS_OF_USE, "en", etag = null)

        // Then
        assertEquals(
            LegalRemoteAnswer.Text(
                markdown = "terms",
                etag = null,
            ),
            answer,
        )
        assertEquals(listOf(HttpMethod.Get to "https://blitzsplit.com/legal/terms.en.md"), requests)
    }

    @Test
    fun `GIVEN the site sends an ETag WHEN fetching THEN keeps it with the text`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.OK,
            body = "terms",
            etag = "\"abc\"",
        )

        // When
        val answer = dataSource.fetchMarkdown(LegalPage.TERMS_OF_USE, "en", etag = null)

        // Then
        assertEquals(
            LegalRemoteAnswer.Text(
                markdown = "terms",
                etag = "\"abc\"",
            ),
            answer,
        )
    }

    @Test
    fun `GIVEN no saved text WHEN fetching THEN asks without If-None-Match`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.OK,
            body = "terms",
        )

        // When
        dataSource.fetchMarkdown(LegalPage.TERMS_OF_USE, "en", etag = null)

        // Then
        assertEquals(listOf<String?>(null), sentEtags)
    }

    @Test
    fun `GIVEN the ETag of a saved text WHEN fetching THEN sends it as If-None-Match`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.NotModified,
            body = "",
        )

        // When
        dataSource.fetchMarkdown(LegalPage.PRIVACY_POLICY, "pt", etag = "\"abc\"")

        // Then
        assertEquals(listOf<String?>("\"abc\""), sentEtags)
    }

    @Test
    fun `GIVEN the saved text is the current one WHEN the site answers 304 THEN it is unchanged`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.NotModified,
            body = "",
        )

        // When
        val answer = dataSource.fetchMarkdown(LegalPage.PRIVACY_POLICY, "pt", etag = "\"abc\"")

        // Then
        assertEquals(LegalRemoteAnswer.Unchanged, answer)
    }

    @Test
    fun `GIVEN the site has no file for the language WHEN fetching THEN it is missing`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.NotFound,
            body = "<html>Not found</html>",
        )

        // When
        val answer = dataSource.fetchMarkdown(LegalPage.PRIVACY_POLICY, "tr", etag = null)

        // Then
        assertEquals(LegalRemoteAnswer.Missing, answer)
    }

    @Test
    fun `GIVEN the site answers a server error WHEN fetching THEN throws`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.InternalServerError,
            body = "oops",
        )

        // When
        val failure = assertFailsWith<IllegalStateException> {
            dataSource.fetchMarkdown(LegalPage.PRIVACY_POLICY, "pt", etag = null)
        }

        // Then
        assertEquals(true, failure.message?.contains("500"))
    }

    @Test
    fun `GIVEN the site answers forbidden WHEN fetching THEN throws`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.Forbidden,
            body = "blocked",
        )

        // When
        val failure = assertFailsWith<IllegalStateException> {
            dataSource.fetchMarkdown(LegalPage.TERMS_OF_USE, "pt", etag = null)
        }

        // Then
        assertEquals(true, failure.message?.contains("403"))
    }

    @Test
    fun `GIVEN the site answers without an ETag WHEN fetching THEN the text has none`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.OK,
            body = "terms",
        )

        // When
        val answer = dataSource.fetchMarkdown(LegalPage.TERMS_OF_USE, "en", etag = null)

        // Then
        assertNull((answer as LegalRemoteAnswer.Text).etag)
    }

    private fun prepareScenario(
        status: HttpStatusCode,
        body: String,
        etag: String? = null,
    ) {
        requests = mutableListOf()
        sentEtags.clear()
        val engine = MockEngine { request ->
            requests += request.method to request.url.toString()
            sentEtags += request.headers[HttpHeaders.IfNoneMatch]
            respond(
                content = body,
                status = status,
                headers = etag?.let { headersOf(HttpHeaders.ETag, it) } ?: headersOf(),
            )
        }
        dataSource = SiteLegalRemoteDataSource(HttpClient(engine))
    }
}
