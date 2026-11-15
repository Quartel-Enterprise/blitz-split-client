package com.quare.blitzsplit.core.account.domain.usecase

// Why: the welcome's notice of an account deleted for inactivity was read, so it doesn't show again.
fun interface DismissExpiredAccountNotice {
    suspend operator fun invoke()
}
