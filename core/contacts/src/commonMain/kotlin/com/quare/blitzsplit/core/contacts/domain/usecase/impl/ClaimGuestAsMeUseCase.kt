package com.quare.blitzsplit.core.contacts.domain.usecase.impl

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.repository.GuestMergeRepository
import com.quare.blitzsplit.core.contacts.domain.usecase.ClaimGuestAsMe

// Why: every member of the guest, in every group, passes to the caller's own member there.
class ClaimGuestAsMeUseCase(
    private val repository: GuestMergeRepository,
) : ClaimGuestAsMe {
    override suspend fun invoke(contact: Contact): Result<Unit> {
        if (!contact.isGuest) return Result.failure(IllegalArgumentException("Only a guest can be claimed"))
        return repository.requestClaim(contact.groups.map { it.memberId }.distinct())
    }
}
