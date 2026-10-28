package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.ObserveBrowserSignInReturningToProfile
import com.quare.blitzsplit.core.model.AccountProvider
import kotlinx.coroutines.flow.Flow

class ObserveBrowserSignInReturningToProfileUseCase(
    private val repository: AccountRepository,
) : ObserveBrowserSignInReturningToProfile {
    override fun invoke(): Flow<AccountProvider?> = repository.observeBrowserSignInReturningToProfile()
}
