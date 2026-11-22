package com.quare.blitzsplit.core.settings.domain.usecase

fun interface UpdateLastSyncedAt {
    suspend operator fun invoke(epochMillis: Long)
}
