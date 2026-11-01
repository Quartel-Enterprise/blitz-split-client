package com.quare.blitzsplit.core.contacts.domain.usecase.impl

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.contacts.domain.repository.GuestMergeRepository
import com.quare.blitzsplit.core.contacts.domain.usecase.RequestGuestMerge

// Why: every member of the guest, the most recently active group's first, whose personal link carries the request.
class RequestGuestMergeUseCase(
    private val repository: GuestMergeRepository,
) : RequestGuestMerge {
    override suspend fun invoke(contact: Contact): Result<GuestMerge> {
        if (!contact.isGuest) return Result.failure(IllegalArgumentException("Only a guest merges into an account"))
        return repository.request(contact.groups.map { it.memberId })
    }
}
