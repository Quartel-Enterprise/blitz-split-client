package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.RemoveMember
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

class RemoveMemberUseCase(
    private val repository: GroupRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : RemoveMember {
    override suspend fun invoke(memberId: String): Result<Unit> = repository.removeMember(
        memberId = memberId,
        removedAt = currentTimeMillis(),
    )
}
