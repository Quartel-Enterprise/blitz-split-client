package com.quare.blitzsplit.core.watch.domain.repository

import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCode
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingStatus

interface WatchPairingRepository {
    suspend fun requestCode(): Result<WatchPairingCode>

    suspend fun requestApproval(code: String): Result<Unit>

    suspend fun fetchStatus(secret: String): Result<WatchPairingStatus>
}
