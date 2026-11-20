package com.quare.blitzsplit.core.watch.data.repository

import com.quare.blitzsplit.core.utils.suspendRunCatching
import com.quare.blitzsplit.core.watch.data.datasource.WatchPairingRemoteDataSource
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCode
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingStatus
import com.quare.blitzsplit.core.watch.domain.repository.WatchPairingRepository
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class WatchPairingRepositoryImpl(
    private val remote: WatchPairingRemoteDataSource,
) : WatchPairingRepository {
    override suspend fun requestCode(): Result<WatchPairingCode> = suspendRunCatching {
        val dto = remote.requestCode()
        WatchPairingCode(
            code = dto.code,
            secret = dto.secret,
            expiresAt = Instant.parse(dto.expiresAt).toEpochMilliseconds(),
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
