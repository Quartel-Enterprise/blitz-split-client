package com.quare.blitzsplit.core.watch.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.watch.domain.model.InvalidWatchPairingCodeException
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCodeFormat
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingNeedsAccountException
import com.quare.blitzsplit.core.watch.domain.repository.WatchPairingRepository
import com.quare.blitzsplit.core.watch.domain.usecase.ApproveWatchPairing
import kotlinx.coroutines.flow.first

class ApproveWatchPairingUseCase(
    private val accountRepository: AccountRepository,
    private val repository: WatchPairingRepository,
) : ApproveWatchPairing {
    override suspend fun invoke(code: String): Result<Unit> {
        if (accountRepository.observeStatus().first() == AccountStatus.LocalOnly) {
            return Result.failure(WatchPairingNeedsAccountException())
        }
        if (!WatchPairingCodeFormat.isComplete(code)) return Result.failure(InvalidWatchPairingCodeException())
        return repository.requestApproval(WatchPairingCodeFormat.normalize(code))
    }
}
