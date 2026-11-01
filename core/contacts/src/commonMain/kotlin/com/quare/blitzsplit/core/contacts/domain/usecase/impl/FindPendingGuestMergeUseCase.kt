package com.quare.blitzsplit.core.contacts.domain.usecase.impl

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.contacts.domain.repository.GuestMergeRepository
import com.quare.blitzsplit.core.contacts.domain.usecase.FindPendingGuestMerge

class FindPendingGuestMergeUseCase(
    private val repository: GuestMergeRepository,
) : FindPendingGuestMerge {
    override suspend fun invoke(contact: Contact): Result<GuestMerge?> {
        if (!contact.isGuest) return Result.success(null)
        return repository.findPending(contact.groups.map { it.memberId })
    }
}
