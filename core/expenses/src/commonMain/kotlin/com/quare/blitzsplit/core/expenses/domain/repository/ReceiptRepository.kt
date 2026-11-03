package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: a receipt is read online only, by the server; nothing of it is kept on the device but the photo the bill attaches.
fun interface ReceiptRepository {
    // Why: null when the photo shows no receipt, or nothing that can be saved as a total.
    suspend fun readReceipt(
        jpeg: ByteArray,
        currency: GroupCurrency,
    ): Result<ReceiptReading?>
}
