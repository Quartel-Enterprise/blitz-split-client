package com.quare.blitzsplit.core.account.domain.model

/*
 * Why: what an Apple sign in hands the app, which the server keeps so deleting the account can revoke it,
 * as the App Store asks. Apple gives it only at sign in.
 */
sealed interface AppleToken {
    // Why: from the native sheet on iOS and macOS; the server trades it for a refresh token.
    data class AuthorizationCode(
        val code: String,
    ) : AppleToken

    // Why: from Supabase's browser sign in, on Android and the web.
    data class RefreshToken(
        val token: String,
    ) : AppleToken
}
