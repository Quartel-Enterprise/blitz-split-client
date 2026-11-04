package com.quare.blitzsplit.core.account.domain.model

import com.quare.blitzsplit.core.model.AccountProvider

// Why: the Google or Apple account the name and photo may come from, and its own picture once the session was read.
data class AccountPhoto(
    val provider: AccountProvider?,
    val photoUrl: String?,
)
