package com.quare.blitzsplit.core.account.data.repository

import com.quare.blitzsplit.core.model.AccountProvider

internal data class ReturningSignIn(
    val provider: AccountProvider?,
    val isSettled: Boolean,
)
