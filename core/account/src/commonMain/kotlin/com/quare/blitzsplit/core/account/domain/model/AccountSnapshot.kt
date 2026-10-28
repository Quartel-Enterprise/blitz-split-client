package com.quare.blitzsplit.core.account.domain.model

import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt

data class AccountSnapshot(
    val status: AccountStatus,
    val recoveryCode: String?,
    val recoveryCodePrompt: RecoveryCodePrompt,
)
