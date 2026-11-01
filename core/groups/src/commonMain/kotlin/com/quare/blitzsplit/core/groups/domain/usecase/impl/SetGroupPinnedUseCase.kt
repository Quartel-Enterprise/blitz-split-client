package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.GroupPinRepository
import com.quare.blitzsplit.core.groups.domain.usecase.SetGroupPinned
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

class SetGroupPinnedUseCase(
    private val repository: GroupPinRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : SetGroupPinned {
    override suspend fun invoke(
        groupId: String,
        isPinned: Boolean,
    ): Result<Unit> = repository.updatePinned(
        groupId = groupId,
        isPinned = isPinned,
        updatedAt = currentTimeMillis(),
    )
}
