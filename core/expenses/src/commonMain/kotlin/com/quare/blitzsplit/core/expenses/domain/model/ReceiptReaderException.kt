package com.quare.blitzsplit.core.expenses.domain.model

// Why: the server didn't read the photo for a reason of its own (never UNREADABLE, which is a reading of null).
class ReceiptReaderException(
    val failure: ReceiptReadFailure,
    cause: Throwable,
) : Exception(failure.name, cause)
