package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.ObserveRecoveryCodePrompt
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import kotlinx.coroutines.flow.Flow

class ObserveRecoveryCodePromptUseCase(
    private val repository: AccountRepository,
) : ObserveRecoveryCodePrompt {
    override fun invoke(): Flow<RecoveryCodePrompt> = repository.observeRecoveryCodePrompt()
}
