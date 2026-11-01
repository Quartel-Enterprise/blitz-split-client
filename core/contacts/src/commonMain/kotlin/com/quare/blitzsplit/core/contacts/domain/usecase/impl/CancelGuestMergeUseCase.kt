package com.quare.blitzsplit.core.contacts.domain.usecase.impl

import com.quare.blitzsplit.core.contacts.domain.repository.GuestMergeRepository
import com.quare.blitzsplit.core.contacts.domain.usecase.CancelGuestMerge

class CancelGuestMergeUseCase(
    private val repository: GuestMergeRepository,
) : CancelGuestMerge {
    override suspend fun invoke(mergeId: String): Result<Unit> = repository.cancel(mergeId)
}
