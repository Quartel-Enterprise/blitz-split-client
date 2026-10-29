package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod

fun interface DetectPaymentKeyType {
    // Why: null when the input could be the type already picked, so a valid key never switches it.
    operator fun invoke(
        method: PaymentMethod,
        input: String,
    ): PaymentKeyType?
}
