package com.quare.blitzsplit.core.expenses.testing

import com.quare.blitzsplit.core.expenses.domain.model.ReceiptCategoryHints
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.core.expenses.domain.repository.ReceiptRepository
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlinx.coroutines.CompletableDeferred

/*
 * In-memory ReceiptRepository, as the server's reader would answer: whatever reading is set, null for a photo it
 * couldn't read. The knobs are public vars, and each photo sent is kept for a test to check.
 */
class FakeReceiptRepository(
    var reading: ReceiptReading? = null,
) : ReceiptRepository {
    val readPhotos = mutableListOf<Pair<ByteArray, GroupCurrency>>()

    // Why: the categories and terms each read was sent with, in the order of readPhotos.
    val readHints = mutableListOf<ReceiptCategoryHints>()

    var readError: Throwable? = null

    // Why: holds a read until the test completes it, to see what shows while it runs.
    var readGate: CompletableDeferred<Unit>? = null

    override suspend fun readReceipt(
        jpeg: ByteArray,
        currency: GroupCurrency,
        hints: ReceiptCategoryHints,
    ): Result<ReceiptReading?> {
        readPhotos += jpeg to currency
        readHints += hints
        readGate?.await()
        readError?.let { return Result.failure(it) }
        return Result.success(reading)
    }
}
