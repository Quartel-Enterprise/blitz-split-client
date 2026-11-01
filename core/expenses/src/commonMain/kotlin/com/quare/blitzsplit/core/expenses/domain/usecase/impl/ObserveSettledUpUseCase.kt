package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import com.quare.blitzsplit.core.expenses.domain.repository.RecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettledUp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveSettledUpUseCase(
    private val repository: RecordedPaymentRepository,
) : ObserveSettledUp {
    override fun invoke(): Flow<RecordedPayment.SettledUp?> = repository
        .observeRecorded()
        .map { it as? RecordedPayment.SettledUp }
        .distinctUntilChanged()
}
