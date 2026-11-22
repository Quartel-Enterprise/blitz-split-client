package com.quare.blitzsplit.core.watch.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.sync.domain.usecase.CompleteSignIn
import com.quare.blitzsplit.core.utils.suspendRunCatching
import com.quare.blitzsplit.core.watch.domain.usecase.SignInWithWatchPairing

class SignInWithWatchPairingUseCase(
    private val accountRepository: AccountRepository,
    private val completeSignIn: CompleteSignIn,
) : SignInWithWatchPairing {
    override suspend fun invoke(tokenHash: String): Result<Unit> = suspendRunCatching {
        accountRepository.signInWithPairingToken(tokenHash).getOrThrow()
        completeSignIn().getOrThrow()
    }
}
