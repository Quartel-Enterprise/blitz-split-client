package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.HandleAppPaused

class HandleAppPausedUseCase(
    private val repository: AccountRepository,
) : HandleAppPaused {
    override fun invoke() {
        repository.handleAppPaused()
    }
}
