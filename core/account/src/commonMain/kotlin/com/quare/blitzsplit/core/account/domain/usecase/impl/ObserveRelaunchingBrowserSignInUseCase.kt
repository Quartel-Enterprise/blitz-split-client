package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.ObserveRelaunchingBrowserSignIn
import com.quare.blitzsplit.core.model.AccountProvider
import kotlinx.coroutines.flow.Flow

class ObserveRelaunchingBrowserSignInUseCase(
    private val repository: AccountRepository,
) : ObserveRelaunchingBrowserSignIn {
    override fun invoke(): Flow<AccountProvider?> = repository.observeRelaunchingBrowserSignIn()
}
