package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.UpdateRecoveryCodePrompt
import com.quare.blitzsplit.core.model.RecoveryCodePrompt

class UpdateRecoveryCodePromptUseCase(
    private val repository: AccountRepository,
) : UpdateRecoveryCodePrompt {
    override suspend fun invoke(prompt: RecoveryCodePrompt) {
        repository.updateRecoveryCodePrompt(prompt)
    }
}
