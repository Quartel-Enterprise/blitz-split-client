package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.ReceiptScan
import com.quare.blitzsplit.core.model.GroupCurrency

fun interface ReadReceipt {
    // Why: source is the photo as it came, from the camera, the gallery or a file; it never fails, see ReceiptScan.
    suspend operator fun invoke(
        source: ByteArray,
        currency: GroupCurrency,
    ): ReceiptScan
}
