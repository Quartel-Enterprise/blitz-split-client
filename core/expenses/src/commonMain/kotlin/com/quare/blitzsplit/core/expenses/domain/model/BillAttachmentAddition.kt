package com.quare.blitzsplit.core.expenses.domain.model

enum class BillAttachmentAddition {
    ADDED,

    // Why: the bill already has its 5 photos (or its receipt), maybe added on another device meanwhile.
    NO_ROOM,

    // Why: the picked file is no image this platform decodes.
    UNREADABLE,

    // Why: a photo so detailed no quality brings it under the limit.
    TOO_LARGE,
}
