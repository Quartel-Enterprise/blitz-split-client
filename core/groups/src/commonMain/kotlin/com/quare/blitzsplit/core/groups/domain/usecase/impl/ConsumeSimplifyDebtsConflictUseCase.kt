package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.ConsumeSimplifyDebtsConflict

class ConsumeSimplifyDebtsConflictUseCase(
    private val repository: GroupRepository,
) : ConsumeSimplifyDebtsConflict {
    override suspend fun invoke(groupId: String): Result<Unit> = repository.consumeSimplifyDebtsConflict(groupId)
}
