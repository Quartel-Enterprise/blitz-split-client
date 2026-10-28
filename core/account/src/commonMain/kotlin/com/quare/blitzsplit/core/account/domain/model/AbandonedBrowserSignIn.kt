package com.quare.blitzsplit.core.account.domain.model

// Why: a cancel ends the wait silently, like a cancelled native sheet, while a failure shows its error.
enum class AbandonedBrowserSignIn {
    CANCELLED,
    FAILED,
}
