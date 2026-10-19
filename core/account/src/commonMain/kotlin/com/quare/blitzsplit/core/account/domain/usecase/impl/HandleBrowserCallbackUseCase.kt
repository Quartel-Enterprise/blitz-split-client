package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.HandleBrowserCallback

class HandleBrowserCallbackUseCase(
    private val repository: AccountRepository,
) : HandleBrowserCallback {
    override suspend fun invoke(url: String): Result<Unit> = repository.handleBrowserCallback(url)
}
