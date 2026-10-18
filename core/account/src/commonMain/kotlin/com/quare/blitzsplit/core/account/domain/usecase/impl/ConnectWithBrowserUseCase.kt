package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithBrowser
import com.quare.blitzsplit.core.model.AccountProvider

class ConnectWithBrowserUseCase(
    private val repository: AccountRepository,
) : ConnectWithBrowser {
    override suspend fun invoke(provider: AccountProvider): Result<Unit> = repository.signInWithBrowser(provider)
}
