package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.GetSplitIncomes

class GetSplitIncomesUseCase(
    private val repository: GroupRepository,
) : GetSplitIncomes {
    override suspend fun invoke(groupId: String): Result<Map<String, Long>> = repository.getSplitIncomes(groupId)
}
