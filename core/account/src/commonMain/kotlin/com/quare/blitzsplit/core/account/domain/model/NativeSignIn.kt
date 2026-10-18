package com.quare.blitzsplit.core.account.domain.model

import com.quare.blitzsplit.core.model.AccountProvider

/*
 * Why: which providers this platform's UI signs in to with its own native button and hands back
 * an ID token (Google on Android, Apple on iOS and macOS). The others go through the browser.
 */
data class NativeSignIn(
    val providers: Set<AccountProvider>,
)
