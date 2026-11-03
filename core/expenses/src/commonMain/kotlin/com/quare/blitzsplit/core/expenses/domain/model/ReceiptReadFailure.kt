package com.quare.blitzsplit.core.expenses.domain.model

// Why: why "Ler cupom fiscal" read nothing from a photo (7.1g), which Rachar conta says each its own way.
enum class ReceiptReadFailure {
    // Why: no receipt nor total in the photo, or a photo that couldn't be opened: another photo may do.
    UNREADABLE,

    // Why: the server's reader didn't answer, isn't set up or failed: not the photo's fault, so the same one may do later.
    READER_UNAVAILABLE,

    // Why: the account read 30 receipts in the last 24 hours (the server's too_many_reads).
    LIMIT_REACHED,

    // Why: the photo never reached the server.
    OFFLINE,
}
