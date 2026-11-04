package com.quare.blitzsplit.core.inappupdate.domain.usecase

// Why: Google Play's flexible update flow, its own dialog asking first; nothing on the other platforms.
fun interface StartUpdate {
    suspend operator fun invoke()
}
