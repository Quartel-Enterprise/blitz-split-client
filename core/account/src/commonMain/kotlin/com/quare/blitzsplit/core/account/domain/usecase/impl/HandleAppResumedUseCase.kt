package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.HandleAppResumed

class HandleAppResumedUseCase(
    private val repository: AccountRepository,
) : HandleAppResumed {
    override fun invoke() {
        repository.handleAppResumed()
    }
}
