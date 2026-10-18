package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.ObserveRecoveryCode
import kotlinx.coroutines.flow.Flow

class ObserveRecoveryCodeUseCase(
    private val repository: AccountRepository,
) : ObserveRecoveryCode {
    override fun invoke(): Flow<String?> = repository.observeRecoveryCode()
}
