package com.quare.blitzsplit.core.contacts.domain.usecase

fun interface CancelGuestMerge {
    suspend operator fun invoke(mergeId: String): Result<Unit>
}
