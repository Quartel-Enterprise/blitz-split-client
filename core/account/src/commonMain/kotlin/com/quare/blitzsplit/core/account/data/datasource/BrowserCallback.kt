package com.quare.blitzsplit.core.account.data.datasource

// Why: what the OAuth callback URL says when the browser comes back, read by the app itself.
enum class BrowserCallback {
    SIGNED_IN,
    CANCELLED,
    IDENTITY_ALREADY_LINKED,
    FAILED,
}
