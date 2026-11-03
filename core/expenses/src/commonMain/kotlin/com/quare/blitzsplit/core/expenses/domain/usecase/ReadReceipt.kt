package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.ReceiptCategoryHints
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptScan
import com.quare.blitzsplit.core.model.GroupCurrency

fun interface ReadReceipt {
    // Why: source is the photo as it came, from the camera, the gallery or a file; it never throws: a failure is a ReceiptScan.Failed.
    suspend operator fun invoke(
        source: ByteArray,
        currency: GroupCurrency,
        hints: ReceiptCategoryHints,
    ): ReceiptScan
}
