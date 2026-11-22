package com.quare.blitzsplit.core.watch.domain.usecase

import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCode

fun interface RequestWatchPairing {
    // Why: the watch's first step either way: the code it hands a nearby phone, or shows for another device (15.12).
    suspend operator fun invoke(): Result<WatchPairingCode>
}
