package com.quare.blitzsplit.core.account.domain.usecase

// Why: each platform hands over the URL the OAuth callback came back with, after handleDeeplinks.
fun interface HandleBrowserCallback {
    suspend operator fun invoke(url: String): Result<Unit>
}
