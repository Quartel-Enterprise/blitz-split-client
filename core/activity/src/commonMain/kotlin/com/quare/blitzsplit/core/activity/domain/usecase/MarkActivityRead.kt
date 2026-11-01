package com.quare.blitzsplit.core.activity.domain.usecase

fun interface MarkActivityRead {
    suspend operator fun invoke(eventId: String): Result<Unit>
}
