package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.RecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSettlement
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

// Why: the payments leave the list and the balances at once, offline included, and every device once synced.
class DeleteSettlementUseCase(
    private val repository: SettlementRepository,
    private val recordedPayments: RecordedPaymentRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : DeleteSettlement {
    override suspend fun invoke(settlementIds: List<String>): Result<Unit> = repository
        .deleteSettlements(
            ids = settlementIds,
            deletedAt = currentTimeMillis(),
        ).onSuccess { settlementIds.forEach(recordedPayments::consume) }
}
