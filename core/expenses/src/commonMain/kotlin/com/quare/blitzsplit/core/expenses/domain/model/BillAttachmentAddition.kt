package com.quare.blitzsplit.core.expenses.domain.model

enum class BillAttachmentAddition {
    ADDED,

    // Why: the bill already has its 5 photos (or its receipt), maybe added on another device meanwhile.
    NO_ROOM,
}
