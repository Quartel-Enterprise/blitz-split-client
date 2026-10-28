package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import kotlinx.coroutines.flow.Flow

class ObserveAccountUseCase(
    private val repository: AccountRepository,
) : ObserveAccount {
    override fun invoke(): Flow<AccountSnapshot> = repository.observeAccount()
}
