package com.quare.blitzsplit.core.account.domain.model

import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt

data class AccountSnapshot(
    val status: AccountStatus,
    val recoveryCode: String?,
    val recoveryCodePrompt: RecoveryCodePrompt,
    // Why: the Google or Apple account's address, null while there is none.
    val email: String?,
    val signOutWhenSynced: Boolean,
)
