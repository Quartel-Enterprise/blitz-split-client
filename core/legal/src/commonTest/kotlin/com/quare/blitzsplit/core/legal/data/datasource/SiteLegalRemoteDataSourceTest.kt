package com.quare.blitzsplit.core.legal.data.datasource

import com.quare.blitzsplit.core.model.LegalPage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SiteLegalRemoteDataSourceTest {
    private lateinit var requests: MutableList<Pair<HttpMethod, String>>
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
            val markdown = dataSource.fetchMarkdown(LegalPage.PRIVACY_POLICY, "pt")

            // Then
            assertEquals("---\ntitle: Privacidade\n---\n", markdown)
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
        val markdown = dataSource.fetchMarkdown(LegalPage.TERMS_OF_USE, "en")

        // Then
        assertEquals("terms", markdown)
        assertEquals(listOf(HttpMethod.Get to "https://blitzsplit.com/legal/terms.en.md"), requests)
    }

    @Test
    fun `GIVEN the site has no file for the language WHEN fetching THEN returns nothing`() = runTest {
        // Given
        prepareScenario(
            status = HttpStatusCode.NotFound,
            body = "<html>Not found</html>",
        )

        // When
        val markdown = dataSource.fetchMarkdown(LegalPage.PRIVACY_POLICY, "tr")

        // Then
        assertNull(markdown)
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
            dataSource.fetchMarkdown(LegalPage.PRIVACY_POLICY, "pt")
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
            dataSource.fetchMarkdown(LegalPage.TERMS_OF_USE, "pt")
        }

        // Then
        assertEquals(true, failure.message?.contains("403"))
    }

    private fun prepareScenario(
        status: HttpStatusCode,
        body: String,
    ) {
        requests = mutableListOf()
        val engine = MockEngine { request ->
            requests += request.method to request.url.toString()
            respond(
                content = body,
                status = status,
            )
        }
        dataSource = SiteLegalRemoteDataSource(HttpClient(engine))
    }
}
