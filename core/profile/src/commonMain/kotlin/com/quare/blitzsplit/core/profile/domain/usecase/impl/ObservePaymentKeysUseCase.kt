package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePaymentKeys
import kotlinx.coroutines.flow.Flow

class ObservePaymentKeysUseCase(
    private val repository: PaymentKeyRepository,
) : ObservePaymentKeys {
    override fun invoke(): Flow<List<PaymentKey>> = repository.observeKeys()
}
