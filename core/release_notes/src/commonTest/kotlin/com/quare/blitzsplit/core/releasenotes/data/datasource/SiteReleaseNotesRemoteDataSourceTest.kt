package com.quare.blitzsplit.core.releasenotes.data.datasource

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

class SiteReleaseNotesRemoteDataSourceTest {
    private val requests = mutableListOf<Pair<HttpMethod, String>>()
    private val sentIfNoneMatch = mutableListOf<List<String>?>()
    private lateinit var dataSource: SiteReleaseNotesRemoteDataSource

    @Test
    fun `GIVEN the site has the feed in Portuguese WHEN fetching it THEN asks for its file and gets its text`() =
        runTest {
            // Given
            prepareScenario(
                status = HttpStatusCode.OK,
                body = FEED,
            )

            // When
            val answer = dataSource.fetchFeed(
                language = "pt",
                etag = null,
            )

            // Then
            assertEquals(
                ReleaseNotesRemoteAnswer.Feed(
                    json = FEED,
                    etag = null,
                ),
                answer,
            )
            assertEquals(listOf(HttpMethod.Get to "https://blitzsplit.com/release-notes/pt.json"), requests)
        }

    @Test
    fun `GIVEN the site has the feed in Filipino WHEN fetching it THEN asks for the file of that language`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.OK,
            body = FEED,
        )

        // When
        dataSource.fetchFeed(
            language = "fil",
            etag = null,
        )

        // Then
        assertEquals(listOf(HttpMethod.Get to "https://blitzsplit.com/release-notes/fil.json"), requests)
    }

    @Test
    fun `GIVEN the site sends an ETag WHEN fetching THEN keeps it with the feed`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.OK,
            body = FEED,
            etag = "\"abc\"",
        )

        // When
        val answer = dataSource.fetchFeed(
            language = "en",
            etag = null,
        )

        // Then
        assertEquals(
            ReleaseNotesRemoteAnswer.Feed(
                json = FEED,
                etag = "\"abc\"",
            ),
            answer,
        )
    }

    @Test
    fun `GIVEN no saved feed WHEN fetching THEN asks without If-None-Match`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.OK,
            body = FEED,
        )

        // When
        dataSource.fetchFeed(
            language = "en",
            etag = null,
        )

        // Then
        assertEquals(listOf<List<String>?>(null), sentIfNoneMatch)
    }

    @Test
    fun `GIVEN the ETag of a saved feed WHEN fetching THEN sends it as If-None-Match`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.NotModified,
            body = "",
        )

        // When
        dataSource.fetchFeed(
            language = "pt",
            etag = "\"abc\"",
        )

        // Then
        assertEquals(listOf<List<String>?>(listOf("\"abc\"")), sentIfNoneMatch)
    }

    @Test
    fun `GIVEN the saved feed is the current one WHEN the site answers 304 THEN it is unchanged`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.NotModified,
            body = "",
            etag = "\"abc\"",
        )

        // When
        val answer = dataSource.fetchFeed(
            language = "pt",
            etag = "\"abc\"",
        )

        // Then
        assertEquals(ReleaseNotesRemoteAnswer.Unchanged, answer)
    }

    @Test
    fun `GIVEN the site has no feed for the language WHEN fetching THEN it is missing`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.NotFound,
            body = "<html>Not found</html>",
        )

        // When
        val answer = dataSource.fetchFeed(
            language = "tr",
            etag = null,
        )

        // Then
        assertEquals(ReleaseNotesRemoteAnswer.Missing, answer)
    }

    @Test
    fun `GIVEN the site answers a server error WHEN fetching THEN fails`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.InternalServerError,
            body = FEED,
        )

        // When
        val failure = assertFailsWith<IllegalStateException> {
            dataSource.fetchFeed(
                language = "pt",
                etag = null,
            )
        }

        // Then
        assertEquals(true, failure.message?.contains("500"))
    }

    @Test
    fun `GIVEN the site answers forbidden WHEN fetching THEN fails`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.Forbidden,
            body = "blocked",
        )

        // When
        val failure = assertFailsWith<IllegalStateException> {
            dataSource.fetchFeed(
                language = "pt",
                etag = null,
            )
        }

        // Then
        assertEquals(true, failure.message?.contains("403"))
    }

    private fun prepareScenario(
        status: HttpStatusCode,
        body: String,
        etag: String? = null,
    ) {
        val engine = MockEngine { request ->
            requests += request.method to request.url.toString()
            sentIfNoneMatch += request.headers.getAll(HttpHeaders.IfNoneMatch)
            respond(
                content = body,
                status = status,
                headers = etag?.let { headersOf(HttpHeaders.ETag, it) } ?: headersOf(),
            )
        }
        dataSource = SiteReleaseNotesRemoteDataSource(HttpClient(engine))
    }

    private companion object {
        const val FEED = """{"releases":[]}"""
    }
}
