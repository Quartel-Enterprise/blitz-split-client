package com.quare.blitzsplit.core.account.domain.usecase

import com.quare.blitzsplit.core.model.RecoveryCodePrompt

fun interface UpdateRecoveryCodePrompt {
    suspend operator fun invoke(prompt: RecoveryCodePrompt)
}
