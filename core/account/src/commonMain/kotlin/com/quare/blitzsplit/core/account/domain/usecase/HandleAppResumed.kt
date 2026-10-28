package com.quare.blitzsplit.core.account.domain.usecase

/*
 * Why: each platform reports the app coming back to the foreground, after any OAuth callback it
 * brought, so a browser sign in left without finishing is told apart from one still completing.
 */
fun interface HandleAppResumed {
    operator fun invoke()
}
