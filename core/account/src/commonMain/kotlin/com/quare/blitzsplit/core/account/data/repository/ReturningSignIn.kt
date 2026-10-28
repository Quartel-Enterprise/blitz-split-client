package com.quare.blitzsplit.core.account.data.repository

import com.quare.blitzsplit.core.model.AccountProvider

internal data class ReturningSignIn(
    val provider: AccountProvider?,
    val isSettled: Boolean,
    // Why: the callback started the app again (the web reloads the page) on a trip that left from the profile.
    val returnsToProfile: Boolean,
)
