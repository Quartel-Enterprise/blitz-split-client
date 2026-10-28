package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyValidation

fun interface ValidatePaymentKey {
    // Why: otherKeys are the user's keys but the one being edited, so the same key can't be saved twice.
    operator fun invoke(
        type: PaymentKeyType,
        input: String,
        otherKeys: List<PaymentKey>,
    ): PaymentKeyValidation
}
