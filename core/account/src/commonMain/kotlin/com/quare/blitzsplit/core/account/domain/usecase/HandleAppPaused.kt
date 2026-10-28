package com.quare.blitzsplit.core.account.domain.usecase

// Why: each platform reports the app leaving the foreground, as it does when the browser opens over it.
fun interface HandleAppPaused {
    operator fun invoke()
}
