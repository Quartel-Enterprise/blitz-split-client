package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.repository.GuestMergeRepository
import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.ClaimGuestAsMe
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class ClaimGuestAsMeUseCase(
    private val repository: GuestMergeRepository,
    private val coordinator: SyncCoordinator,
) : ClaimGuestAsMe {
    override suspend fun invoke(contact: Contact): Result<Int> {
        if (!contact.isGuest) return Result.failure(IllegalArgumentException("Only a guest can be claimed"))
        val memberIds = contact.groups.map { it.memberId }.distinct()
        return withContext(NonCancellable) {
            coordinator.syncThen { repository.requestClaim(memberIds) }.onSuccess { coordinator.syncNow() }
        }
    }
}
