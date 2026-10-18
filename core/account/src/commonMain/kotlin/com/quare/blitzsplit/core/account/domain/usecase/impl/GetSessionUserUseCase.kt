package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.GetSessionUser

class GetSessionUserUseCase(
    private val repository: AccountRepository,
) : GetSessionUser {
    override suspend fun invoke(): SessionUser? = repository.getCurrentUserOrNull()
}
