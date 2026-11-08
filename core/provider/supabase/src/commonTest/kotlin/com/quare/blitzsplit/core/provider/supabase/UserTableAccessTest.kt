package com.quare.blitzsplit.core.provider.supabase

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserTableAccessTest {
    private val requestedPaths = mutableListOf<String>()

    private val client = clientFor(URL)

    @Test
    fun `GIVEN a server under a path prefix and no user token WHEN reading a table THEN fails before reaching it`() =
        runTest {
            // Given
            val prefixedClient = clientFor(PREFIXED_URL)

            // When
            assertFailsWith<UserSessionRequiredException> { prefixedClient.get("$PREFIXED_URL/rest/v1/groups") }

            // Then
            assertEquals(emptyList(), requestedPaths)
        }

    @Test
    fun `GIVEN no user token WHEN reading a table THEN fails before reaching the server`() = runTest {
        // Given
        val url = "$URL/rest/v1/groups"

        // When
        val error = assertFailsWith<UserSessionRequiredException> { client.get(url) }

        // Then
        assertEquals("/rest/v1/groups", error.path)
        assertEquals(emptyList(), requestedPaths)
    }

    @Test
    fun `GIVEN only the publishable key WHEN reading a table THEN fails before reaching the server`() = runTest {
        // Given
        val url = "$URL/rest/v1/expenses"

        // When
        assertFailsWith<UserSessionRequiredException> { client.get(url) { bearerAuth(PUBLISHABLE_KEY) } }

        // Then
        assertEquals(emptyList(), requestedPaths)
    }

    @Test
    fun `GIVEN no user token WHEN writing a table THEN fails before reaching the server`() = runTest {
        // Given
        val url = "$URL/rest/v1/settlements"

        // When
        assertFailsWith<UserSessionRequiredException> { client.post(url) }

        // Then
        assertEquals(emptyList(), requestedPaths)
    }

    @Test
    fun `GIVEN a user token WHEN reading a table THEN reaches the server`() = runTest {
        // Given
        val url = "$URL/rest/v1/groups"

        // When
        client.get(url) { withUserToken() }

        // Then
        assertEquals(listOf("/rest/v1/groups"), requestedPaths)
    }

    @Test
    fun `GIVEN no user token WHEN calling a function of the database THEN reaches the server`() = runTest {
        // Given
        val url = "$URL/rest/v1/rpc/get_invite"

        // When
        client.post(url)

        // Then
        assertEquals(listOf("/rest/v1/rpc/get_invite"), requestedPaths)
    }

    @Test
    fun `GIVEN no user token WHEN calling Auth THEN reaches the server`() = runTest {
        // Given
        val url = "$URL/auth/v1/signup"

        // When
        client.post(url)

        // Then
        assertEquals(listOf("/auth/v1/signup"), requestedPaths)
    }

    private fun clientFor(supabaseUrl: String): HttpClient = HttpClient(
        MockEngine { request ->
            requestedPaths += request.url.encodedPath
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
            )
        },
    ) {
        install(
            createUserTableAccess(
                supabaseUrl = supabaseUrl,
                publishableKey = PUBLISHABLE_KEY,
            ),
        )
    }

    private fun HttpRequestBuilder.withUserToken() {
        bearerAuth("header.payload.signature")
    }

    private companion object {
        const val URL = "https://project.supabase.co"
        const val PREFIXED_URL = "https://example.com/supabase"
        const val PUBLISHABLE_KEY = "sb_publishable_key"
    }
}
