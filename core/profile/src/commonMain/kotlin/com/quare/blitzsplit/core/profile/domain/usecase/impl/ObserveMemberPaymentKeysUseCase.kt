package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveMemberPaymentKeys
import kotlinx.coroutines.flow.Flow

class ObserveMemberPaymentKeysUseCase(
    private val repository: PaymentKeyRepository,
) : ObserveMemberPaymentKeys {
    override fun invoke(userId: String): Flow<List<PaymentKey>> = repository.observeKeysOf(userId)
}
