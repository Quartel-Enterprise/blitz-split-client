package com.quare.blitzsplit.core.watch.domain.usecase

import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCode

fun interface AwaitWatchPairing {
    /*
     * Why: polls the code's secret until a device approves it, with the token hash to sign in with; fails once the
     * code expires. A poll that gets no answer is tried again: the watch may lose its connection for a moment.
     */
    suspend operator fun invoke(code: WatchPairingCode): Result<String>
}
