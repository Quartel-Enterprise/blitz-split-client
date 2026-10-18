package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.model.AccountStatus
import kotlinx.coroutines.flow.Flow

class ObserveAccountStatusUseCase(
    private val repository: AccountRepository,
) : ObserveAccountStatus {
    override fun invoke(): Flow<AccountStatus> = repository.observeStatus()
}
