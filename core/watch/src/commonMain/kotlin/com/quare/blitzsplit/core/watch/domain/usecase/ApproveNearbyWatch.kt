package com.quare.blitzsplit.core.watch.domain.usecase

fun interface ApproveNearbyWatch {
    // Why: the phone's answer to a nearby watch's code, one of WatchLink's REPLY_ values, the same on Android and iOS.
    suspend operator fun invoke(code: String): String
}
