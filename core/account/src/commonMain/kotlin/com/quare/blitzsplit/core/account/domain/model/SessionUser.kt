package com.quare.blitzsplit.core.account.domain.model

import com.quare.blitzsplit.core.model.AccountProvider

data class SessionUser(
    val userId: String,
    val socialProvider: AccountProvider?,
    val displayName: String?,
    val photoUrl: String?,
    val email: String?,
)
