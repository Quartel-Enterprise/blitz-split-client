package com.quare.blitzsplit.core.account.data.datasource

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SupabaseCallbackUrlTest {
    @Test
    fun `GIVEN the app callback with identity already exists in both parts WHEN reading THEN it is already linked`() {
        // Given
        val error = "error=server_error&error_code=identity_already_exists" +
            "&error_description=Identity+is+already+linked+to+another+user"
        val url = "borarachar://auth-callback?$error#$error"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertEquals(BrowserCallback.IDENTITY_ALREADY_LINKED, callback)
    }

    @Test
    fun `GIVEN identity already exists only in the web fragment WHEN reading THEN it is already linked`() {
        // Given
        val url = "https://app.example/#error=server_error&error_code=identity_already_exists" +
            "&error_description=Identity+is+already+linked+to+another+user"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertEquals(BrowserCallback.IDENTITY_ALREADY_LINKED, callback)
    }

    @Test
    fun `GIVEN identity already exists only in the web query WHEN reading THEN it is already linked`() {
        // Given
        val url = "https://app.example/?error=server_error&error_code=identity_already_exists" +
            "&error_description=Identity+is+already+linked+to+another+user"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertEquals(BrowserCallback.IDENTITY_ALREADY_LINKED, callback)
    }

    @Test
    fun `GIVEN the app callback with another error code WHEN reading THEN the sign in failed`() {
        // Given
        val error = "error=invalid_request&error_code=bad_oauth_state&error_description=OAuth+state+is+invalid"
        val url = "borarachar://auth-callback?$error#$error"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertEquals(BrowserCallback.FAILED, callback)
    }

    @Test
    fun `GIVEN a web callback with an error and no error code WHEN reading THEN the sign in failed`() {
        // Given
        val url = "https://app.example/#error=access_denied&error_description=The+user+denied+the+request"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertEquals(BrowserCallback.FAILED, callback)
    }

    @Test
    fun `GIVEN the app callback with a PKCE code WHEN reading THEN signed in`() {
        // Given
        val url = "borarachar://auth-callback?code=3f2a9c1e-77b0-4d2e-9a51-0c6f8e4b2d11"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertEquals(BrowserCallback.SIGNED_IN, callback)
    }

    @Test
    fun `GIVEN a web callback with the tokens in the fragment WHEN reading THEN signed in`() {
        // Given
        val url = "https://app.example/#access_token=eyJhbGciOi.payload.signature" +
            "&expires_in=3600&refresh_token=r3fr35h&token_type=bearer"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertEquals(BrowserCallback.SIGNED_IN, callback)
    }

    @Test
    fun `GIVEN a plain web page load WHEN reading THEN it is not a callback`() {
        // Given
        val url = "https://app.example/"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertNull(callback)
    }

    @Test
    fun `GIVEN a web page with unrelated query parameters and fragment WHEN reading THEN it is not a callback`() {
        // Given
        val url = "https://app.example/groups?tab=saldos&utm_source=whatsapp#top"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertNull(callback)
    }

    @Test
    fun `GIVEN the app callback with nothing in it WHEN reading THEN it is not a callback`() {
        // Given
        val url = "borarachar://auth-callback"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertNull(callback)
    }

    @Test
    fun `GIVEN a malformed URL WHEN reading THEN it is not a callback`() {
        // Given
        val url = "https://[broken"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertNull(callback)
    }

    @Test
    fun `GIVEN a bad percent escape in the fragment WHEN reading THEN it is not a callback`() {
        // Given
        val url = "https://app.example/#error=%zz"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertNull(callback)
    }

    @Test
    fun `GIVEN a bad percent escape in the query WHEN reading THEN it is not a callback`() {
        // Given
        val url = "borarachar://auth-callback?code=%G1"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertNull(callback)
    }

    @Test
    fun `GIVEN an empty string WHEN reading THEN it is not a callback`() {
        // Given
        val url = ""

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertNull(callback)
    }

    @Test
    fun `GIVEN the app callback with an error and no description WHEN reading THEN the sign in failed`() {
        // Given
        val url = "borarachar://auth-callback?error=access_denied"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertEquals(BrowserCallback.FAILED, callback)
    }

    @Test
    fun `GIVEN a web page with an unrelated code parameter WHEN reading THEN it is not a callback`() {
        // Given
        val url = "https://app.example/?code=INVITE42"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertNull(callback)
    }

    @Test
    fun `GIVEN a web page with an unrelated error parameter WHEN reading THEN it is not a callback`() {
        // Given
        val url = "https://app.example/?error=identity_already_exists&error_code=identity_already_exists"

        // When
        val callback = SupabaseCallbackUrl.read(url)

        // Then
        assertNull(callback)
    }
}
