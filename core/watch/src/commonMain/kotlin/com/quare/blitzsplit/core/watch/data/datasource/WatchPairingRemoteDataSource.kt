package com.quare.blitzsplit.core.watch.data.datasource

import com.quare.blitzsplit.core.watch.data.dto.WatchPairingCodeDto
import com.quare.blitzsplit.core.watch.data.dto.WatchPairingPollResponseDto

interface WatchPairingRemoteDataSource {
    suspend fun requestCode(): WatchPairingCodeDto

    // Why: with the session of the device that approves, phone or any other signed in.
    suspend fun approve(code: String)

    suspend fun fetchStatus(secret: String): WatchPairingPollResponseDto
}
