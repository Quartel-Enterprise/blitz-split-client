package com.quare.blitzsplit.core.sync.domain.usecase

fun interface SyncNow {
    suspend operator fun invoke(): Result<Unit>
}
