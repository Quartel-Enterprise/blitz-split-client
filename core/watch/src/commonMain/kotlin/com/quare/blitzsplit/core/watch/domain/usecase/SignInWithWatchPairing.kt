package com.quare.blitzsplit.core.watch.domain.usecase

fun interface SignInWithWatchPairing {
    // Why: the watch's own session for the same user, then what the account already has, as every sign in pulls.
    suspend operator fun invoke(tokenHash: String): Result<Unit>
}
