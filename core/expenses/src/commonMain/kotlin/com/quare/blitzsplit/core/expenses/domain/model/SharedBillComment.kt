package com.quare.blitzsplit.core.expenses.domain.model

// Why: how a comment through a bill's link ended.
enum class SharedBillComment {
    POSTED,

    // Why: the link stopped working, or the bill was deleted, while it was open.
    NOT_FOUND,

    // Why: the link became "Só quem está na conta" while it was open.
    MEMBERS_ONLY,

    // Why: someone signed in with no name the server knows, and none on this device.
    NEEDS_NAME,
}
