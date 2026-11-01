package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import com.quare.blitzsplit.core.expenses.domain.repository.RecordedPaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Why: a notice only matters while the app runs.
class InMemoryRecordedPaymentRepository : RecordedPaymentRepository {
    private val recorded = MutableStateFlow<RecordedPayment?>(null)

    override fun observeRecorded(): Flow<RecordedPayment?> = recorded

    override fun post(recorded: RecordedPayment) {
        this.recorded.value = recorded
    }

    override fun consume(settlementId: String) {
        recorded.update { if (it != null && settlementId in it.settlementIds) null else it }
    }
}
