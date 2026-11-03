package com.quare.blitzsplit.feature.splitbill.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReadFailure
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptError

internal fun ReceiptReadFailure.toReceiptError(): ReceiptError = when (this) {
    ReceiptReadFailure.UNREADABLE -> ReceiptError.UNREADABLE
    ReceiptReadFailure.READER_UNAVAILABLE -> ReceiptError.READER_UNAVAILABLE
    ReceiptReadFailure.LIMIT_REACHED -> ReceiptError.LIMIT_REACHED
    ReceiptReadFailure.OFFLINE -> ReceiptError.OFFLINE
}
