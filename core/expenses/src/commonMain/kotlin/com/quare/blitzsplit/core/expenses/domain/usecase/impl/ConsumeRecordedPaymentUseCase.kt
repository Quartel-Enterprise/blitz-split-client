package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.RecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment

class ConsumeRecordedPaymentUseCase(
    private val repository: RecordedPaymentRepository,
) : ConsumeRecordedPayment {
    override fun invoke(settlementId: String) {
        repository.consume(settlementId)
    }
}
