package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.PaymentKey
import kotlinx.coroutines.flow.Flow

fun interface ObservePaymentKeys {
    operator fun invoke(): Flow<List<PaymentKey>>
}
