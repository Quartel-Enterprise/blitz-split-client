package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePendingPaymentKeyChanges
import kotlinx.coroutines.flow.Flow

class ObservePendingPaymentKeyChangesUseCase(
    private val repository: PaymentKeyRepository,
) : ObservePendingPaymentKeyChanges {
    override fun invoke(): Flow<Boolean> = repository.observeHasPendingChanges()
}
