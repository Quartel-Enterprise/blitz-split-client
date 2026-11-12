package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.PaymentKey
import kotlinx.coroutines.flow.Flow

fun interface ObserveAllMemberPaymentKeys {
    // Why: the keys of everyone in the user's groups, by any account, as the last sync cached them.
    operator fun invoke(): Flow<List<PaymentKey>>
}
