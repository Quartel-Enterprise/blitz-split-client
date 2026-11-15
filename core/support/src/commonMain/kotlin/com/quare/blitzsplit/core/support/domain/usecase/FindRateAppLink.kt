package com.quare.blitzsplit.core.support.domain.usecase

// Why: where Profile's "Rate the app" goes on this platform; null where no store lists the app yet.
fun interface FindRateAppLink {
    operator fun invoke(): String?
}
