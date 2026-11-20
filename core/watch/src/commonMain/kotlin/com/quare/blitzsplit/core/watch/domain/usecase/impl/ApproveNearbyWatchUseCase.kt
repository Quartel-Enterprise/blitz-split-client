package com.quare.blitzsplit.core.watch.domain.usecase.impl

import com.quare.blitzsplit.core.watch.domain.model.WatchPairingNeedsAccountException
import com.quare.blitzsplit.core.watch.domain.usecase.ApproveNearbyWatch
import com.quare.blitzsplit.core.watch.domain.usecase.ApproveWatchPairing
import com.quare.blitzsplit.core.watch.link.WatchLink

class ApproveNearbyWatchUseCase(
    private val approveWatchPairing: ApproveWatchPairing,
) : ApproveNearbyWatch {
    override suspend fun invoke(code: String): String {
        val result = approveWatchPairing(code)
        return when {
            result.isSuccess -> WatchLink.REPLY_APPROVED
            result.exceptionOrNull() is WatchPairingNeedsAccountException -> WatchLink.REPLY_LOCAL_ONLY
            else -> WatchLink.REPLY_FAILED
        }
    }
}
