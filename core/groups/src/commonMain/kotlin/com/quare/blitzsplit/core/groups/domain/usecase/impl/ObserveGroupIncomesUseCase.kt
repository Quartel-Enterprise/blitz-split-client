package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.GroupIncomeRepository
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroupIncomes
import kotlinx.coroutines.flow.Flow

class ObserveGroupIncomesUseCase(
    private val repository: GroupIncomeRepository,
) : ObserveGroupIncomes {
    override fun invoke(groupId: String): Flow<Map<String, Long>> = repository.observeIncomes(groupId)
}
