package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.ObserveReturningBrowserSignIn
import com.quare.blitzsplit.core.model.AccountProvider
import kotlinx.coroutines.flow.Flow

class ObserveReturningBrowserSignInUseCase(
    private val repository: AccountRepository,
) : ObserveReturningBrowserSignIn {
    override fun invoke(): Flow<AccountProvider?> = repository.observeReturningBrowserSignIn()
}
