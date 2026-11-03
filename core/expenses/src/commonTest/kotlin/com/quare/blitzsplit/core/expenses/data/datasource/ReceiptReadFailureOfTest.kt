package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReadFailure
import kotlin.test.Test
import kotlin.test.assertEquals

class ReceiptReadFailureOfTest {
    @Test
    fun `GIVEN status 422 for a photo with no receipt or no total WHEN mapping it THEN asks for another photo`() {
        // Given
        val statusCode = 422

        // When
        val failure = receiptReadFailureOf(statusCode)

        // Then
        assertEquals(ReceiptReadFailure.UNREADABLE, failure)
    }

    @Test
    fun `GIVEN status 413 for a photo too large WHEN mapping it THEN asks for another photo`() {
        // Given
        val statusCode = 413

        // When
        val failure = receiptReadFailureOf(statusCode)

        // Then
        assertEquals(ReceiptReadFailure.UNREADABLE, failure)
    }

    @Test
    fun `GIVEN status 429 for 30 reads in 24 hours WHEN mapping it THEN reports the daily limit`() {
        // Given
        val statusCode = 429

        // When
        val failure = receiptReadFailureOf(statusCode)

        // Then
        assertEquals(ReceiptReadFailure.LIMIT_REACHED, failure)
    }

    @Test
    fun `GIVEN status 503 for a reader not set up WHEN mapping it THEN reports the reader unavailable`() {
        // Given
        val statusCode = 503

        // When
        val failure = receiptReadFailureOf(statusCode)

        // Then
        assertEquals(ReceiptReadFailure.READER_UNAVAILABLE, failure)
    }

    @Test
    fun `GIVEN status 502 for a reader that failed WHEN mapping it THEN reports the reader unavailable`() {
        // Given
        val statusCode = 502

        // When
        val failure = receiptReadFailureOf(statusCode)

        // Then
        assertEquals(ReceiptReadFailure.READER_UNAVAILABLE, failure)
    }

    @Test
    fun `GIVEN status 500 for a server error WHEN mapping it THEN reports the reader unavailable`() {
        // Given
        val statusCode = 500

        // When
        val failure = receiptReadFailureOf(statusCode)

        // Then
        assertEquals(ReceiptReadFailure.READER_UNAVAILABLE, failure)
    }

    @Test
    fun `GIVEN status 400 for an invalid body WHEN mapping it THEN reports the reader unavailable`() {
        // Given
        val statusCode = 400

        // When
        val failure = receiptReadFailureOf(statusCode)

        // Then
        assertEquals(ReceiptReadFailure.READER_UNAVAILABLE, failure)
    }

    @Test
    fun `GIVEN status 401 for an unauthorized session WHEN mapping it THEN reports the reader unavailable`() {
        // Given
        val statusCode = 401

        // When
        val failure = receiptReadFailureOf(statusCode)

        // Then
        assertEquals(ReceiptReadFailure.READER_UNAVAILABLE, failure)
    }

    @Test
    fun `GIVEN status 404 the server never answers WHEN mapping it THEN reports the reader unavailable`() {
        // Given
        val statusCode = 404

        // When
        val failure = receiptReadFailureOf(statusCode)

        // Then
        assertEquals(ReceiptReadFailure.READER_UNAVAILABLE, failure)
    }
}
