package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.RenameMember
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

class RenameMemberUseCase(
    private val repository: GroupRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : RenameMember {
    override suspend fun invoke(
        memberId: String,
        name: String,
    ): Result<Unit> = repository.updateMemberName(
        memberId = memberId,
        name = GroupNameRules.toCapped(name.trim()),
        updatedAt = currentTimeMillis(),
    )
}
