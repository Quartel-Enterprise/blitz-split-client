package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettlements
import kotlinx.coroutines.flow.Flow

class ObserveSettlementsUseCase(
    private val repository: SettlementRepository,
) : ObserveSettlements {
    override fun invoke(groupId: String): Flow<List<Settlement>> = repository.observeSettlements(groupId)
}
