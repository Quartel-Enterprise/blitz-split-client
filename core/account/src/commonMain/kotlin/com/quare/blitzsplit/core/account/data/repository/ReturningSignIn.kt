package com.quare.blitzsplit.core.account.data.repository

import com.quare.blitzsplit.core.model.AccountProvider

internal data class ReturningSignIn(
    val provider: AccountProvider?,
    val isSettled: Boolean,
    // Why: the callback started the app again (the web reloads the page), so the screen it left from is gone.
    val isRelaunch: Boolean,
)
