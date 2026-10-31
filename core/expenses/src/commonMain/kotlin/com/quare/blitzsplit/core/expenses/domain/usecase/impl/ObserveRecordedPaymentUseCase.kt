package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.repository.RecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveRecordedPayment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveRecordedPaymentUseCase(
    private val repository: RecordedPaymentRepository,
) : ObserveRecordedPayment {
    override fun invoke(groupId: String): Flow<Settlement?> = repository
        .observeRecorded()
        .map { it?.takeIf { settlement -> settlement.groupId == groupId } }
        .distinctUntilChanged()
}
