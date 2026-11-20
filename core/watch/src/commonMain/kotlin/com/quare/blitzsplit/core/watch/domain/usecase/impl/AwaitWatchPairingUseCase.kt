package com.quare.blitzsplit.core.watch.domain.usecase.impl

import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.watch.domain.model.ExpiredWatchPairingException
import com.quare.blitzsplit.core.watch.domain.model.InvalidWatchPairingCodeException
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCode
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingStatus
import com.quare.blitzsplit.core.watch.domain.repository.WatchPairingRepository
import com.quare.blitzsplit.core.watch.domain.usecase.AwaitWatchPairing
import kotlinx.coroutines.delay

class AwaitWatchPairingUseCase(
    private val repository: WatchPairingRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : AwaitWatchPairing {
    override suspend fun invoke(code: WatchPairingCode): Result<String> {
        while (currentTimeMillis() < code.expiresAt) {
            val polled = repository.fetchStatus(code.secret)
            val status = polled.getOrNull()
            if (status is WatchPairingStatus.Approved) return Result.success(status.tokenHash)
            val failure = polled.exceptionOrNull()
            // Why: the server says the code is over; anything else (no answer, a 500) is worth another poll.
            if (failure is ExpiredWatchPairingException || failure is InvalidWatchPairingCodeException) {
                return Result.failure(ExpiredWatchPairingException())
            }
            delay(code.pollInterval)
        }
        return Result.failure(ExpiredWatchPairingException())
    }
}
