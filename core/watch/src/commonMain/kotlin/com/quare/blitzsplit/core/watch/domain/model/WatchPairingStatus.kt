package com.quare.blitzsplit.core.watch.domain.model

// Why: what a poll of the watch's secret answers: still waiting, or approved with the token hash to sign in with.
sealed interface WatchPairingStatus {
    data object Pending : WatchPairingStatus

    data class Approved(
        val tokenHash: String,
    ) : WatchPairingStatus
}
