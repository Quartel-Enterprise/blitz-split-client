package com.quare.blitzsplit.android.platform

sealed interface GoogleSignInResult {
    data class Success(
        val idToken: String,
        val rawNonce: String,
    ) : GoogleSignInResult

    data object Cancelled : GoogleSignInResult

    data object Failed : GoogleSignInResult
}
