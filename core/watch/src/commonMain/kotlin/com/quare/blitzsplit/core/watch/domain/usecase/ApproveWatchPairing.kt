package com.quare.blitzsplit.core.watch.domain.usecase

fun interface ApproveWatchPairing {
    // Why: a nearby phone on its own, or Profile › Connect watch (15.14) on any device with the account.
    suspend operator fun invoke(code: String): Result<Unit>
}
