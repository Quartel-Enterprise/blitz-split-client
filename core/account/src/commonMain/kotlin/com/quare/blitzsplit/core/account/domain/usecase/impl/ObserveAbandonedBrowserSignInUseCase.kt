package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.model.AbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAbandonedBrowserSignIn
import kotlinx.coroutines.flow.Flow

class ObserveAbandonedBrowserSignInUseCase(
    private val repository: AccountRepository,
) : ObserveAbandonedBrowserSignIn {
    override fun invoke(): Flow<AbandonedBrowserSignIn> = repository.observeAbandonedBrowserSignIn()
}
