package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.PaymentKey
import kotlinx.coroutines.flow.Flow

fun interface ObserveMemberPaymentKeys {
    // Why: "Pagar Ana" (4.5): the keys of someone in the user's groups, by their account, as the last sync cached them.
    operator fun invoke(userId: String): Flow<List<PaymentKey>>
}
