package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.UnlinkAccount

class UnlinkAccountUseCase(
    private val repository: AccountRepository,
) : UnlinkAccount {
    override suspend fun invoke(): Result<Unit> = repository.removeSocialIdentity()
}
