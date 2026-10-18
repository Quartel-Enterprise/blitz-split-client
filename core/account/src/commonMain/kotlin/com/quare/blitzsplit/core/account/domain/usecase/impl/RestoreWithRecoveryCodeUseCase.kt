package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.RestoreWithRecoveryCode

class RestoreWithRecoveryCodeUseCase(
    private val repository: AccountRepository,
) : RestoreWithRecoveryCode {
    override suspend fun invoke(code: String): Result<Unit> = repository.restoreWithRecoveryCode(code)
}
