package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveAllMemberPaymentKeys
import kotlinx.coroutines.flow.Flow

class ObserveAllMemberPaymentKeysUseCase(
    private val repository: PaymentKeyRepository,
) : ObserveAllMemberPaymentKeys {
    override fun invoke(): Flow<List<PaymentKey>> = repository.observeAllMemberKeys()
}
