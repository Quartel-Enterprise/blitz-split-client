package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.provider.crashlytics.ReportableFailure
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncFailureTest {
    private val noAnswer = HttpRequestException(
        message = "timeout",
        request = HttpRequestBuilder().apply { url("https://example.supabase.co/rest/v1/expenses?id=eq.bill-42") },
    )

    @Test
    fun `GIVEN a failure alone WHEN describing it THEN says only its kind`() {
        // Given
        val failure = IllegalStateException("server down")

        // When
        val message = SyncFailure(failure).message

        // Then
        assertEquals("IllegalStateException", message)
    }

    @Test
    fun `GIVEN a failure with causes WHEN describing it THEN names each kind cause by cause`() {
        // Given
        val failure = IllegalStateException(
            "sync failed",
            IllegalArgumentException("refused", UnsupportedOperationException("unknown column")),
        )

        // When
        val message = SyncFailure(failure).message

        // Then
        assertEquals("IllegalStateException ← IllegalArgumentException ← UnsupportedOperationException", message)
    }

    @Test
    fun `GIVEN messages with an id a name and an amount WHEN describing the failure THEN none of them is in it`() {
        // Given
        val failure = IllegalStateException(
            "bill-42 of Pierre refused",
            IllegalArgumentException("Pierre owes 312 in group-7"),
        )

        // When
        val message = SyncFailure(failure).message.orEmpty()

        // Then
        assertFalse("bill-42" in message)
        assertFalse("Pierre" in message)
        assertFalse("312" in message)
        assertFalse("group-7" in message)
    }

    @Test
    fun `GIVEN a request that got no answer WHEN describing the failure THEN its URL is not in it`() {
        // Given
        val failure = IllegalStateException("pull failed", noAnswer)

        // When
        val message = SyncFailure(failure).message.orEmpty()

        // Then
        assertEquals("IllegalStateException ← HttpRequestException", message)
        assertFalse("bill-42" in message)
    }

    @Test
    fun `GIVEN a failure with a cause WHEN wrapping it THEN the report has no cause`() {
        // Given
        val failure = IllegalStateException("sync failed", IllegalArgumentException("refused"))

        // When
        val reported = SyncFailure(failure)

        // Then
        assertNull(reported.cause)
    }

    @Test
    fun `GIVEN a failure WHEN wrapping it THEN it is sent as written`() {
        // Given
        val failure = IllegalStateException("server down")

        // When
        val reported: Throwable = SyncFailure(failure)

        // Then
        assertIs<ReportableFailure>(reported)
    }

    @Test
    fun `GIVEN a request that got no answer WHEN checking the failure THEN it is a connection failure`() {
        // Given
        val failure: Throwable = noAnswer

        // When
        val isConnectionFailure = failure.isConnectionFailure()

        // Then
        assertTrue(isConnectionFailure)
    }

    @Test
    fun `GIVEN a failure caused by a request that got no answer WHEN checking it THEN it is a connection failure`() {
        // Given
        val failure = IllegalStateException("sync failed", IllegalArgumentException("pull failed", noAnswer))

        // When
        val isConnectionFailure = failure.isConnectionFailure()

        // Then
        assertTrue(isConnectionFailure)
    }

    @Test
    fun `GIVEN a change the server refused WHEN checking the failure THEN it is not a connection failure`() {
        // Given
        val failure = IllegalStateException("refused", IllegalArgumentException("check violated"))

        // When
        val isConnectionFailure = failure.isConnectionFailure()

        // Then
        assertFalse(isConnectionFailure)
    }
}
