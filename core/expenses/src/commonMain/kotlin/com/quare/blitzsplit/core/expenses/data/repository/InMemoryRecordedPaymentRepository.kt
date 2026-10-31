package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.repository.RecordedPaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Why: a notice only matters while the app runs.
class InMemoryRecordedPaymentRepository : RecordedPaymentRepository {
    private val recorded = MutableStateFlow<Settlement?>(null)

    override fun observeRecorded(): Flow<Settlement?> = recorded

    override fun post(settlement: Settlement) {
        recorded.value = settlement
    }

    override fun consume(settlementId: String) {
        recorded.update { if (it?.id == settlementId) null else it }
    }
}
