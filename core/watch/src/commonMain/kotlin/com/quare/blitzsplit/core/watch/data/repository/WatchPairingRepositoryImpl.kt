package com.quare.blitzsplit.core.watch.data.repository

import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import com.quare.blitzsplit.core.watch.data.datasource.WatchPairingRemoteDataSource
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCode
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingStatus
import com.quare.blitzsplit.core.watch.domain.repository.WatchPairingRepository
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class WatchPairingRepositoryImpl(
    private val remote: WatchPairingRemoteDataSource,
    private val currentTimeMillis: CurrentTimeMillis,
) : WatchPairingRepository {
    // Why: how long the server keeps a code.
    private val codeLifetime = 5.minutes

    override suspend fun requestCode(): Result<WatchPairingCode> = suspendRunCatching {
        val dto = remote.requestCode()
        WatchPairingCode(
            code = dto.code,
            secret = dto.secret,
            /*
             * Why: on the watch's own clock, from when the code came, since a watch's clock may be minutes off the
             * server's and every code would then seem over; the server keeps each code 5 minutes.
             */
            expiresAt = currentTimeMillis() + codeLifetime.inWholeMilliseconds,
            pollInterval = dto.pollIntervalSeconds.coerceAtLeast(MIN_POLL_SECONDS).seconds,
        )
    }

    override suspend fun requestApproval(code: String): Result<Unit> = suspendRunCatching { remote.approve(code) }

    override suspend fun fetchStatus(secret: String): Result<WatchPairingStatus> = suspendRunCatching {
        val dto = remote.fetchStatus(secret)
        val tokenHash = dto.tokenHash
        if (dto.status == APPROVED &&
            tokenHash != null
        ) {
            WatchPairingStatus.Approved(tokenHash)
        } else {
            WatchPairingStatus.Pending
        }
    }

    private companion object {
        const val APPROVED = "approved"

        // Why: a server answering 0 never turns the watch's wait into a busy loop.
        const val MIN_POLL_SECONDS = 1
    }
}
