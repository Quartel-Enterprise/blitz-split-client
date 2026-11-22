package com.quare.blitzsplit.core.watch.domain.usecase.impl

import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCode
import com.quare.blitzsplit.core.watch.domain.repository.WatchPairingRepository
import com.quare.blitzsplit.core.watch.domain.usecase.RequestWatchPairing

class RequestWatchPairingUseCase(
    private val repository: WatchPairingRepository,
) : RequestWatchPairing {
    override suspend fun invoke(): Result<WatchPairingCode> = repository.requestCode()
}
