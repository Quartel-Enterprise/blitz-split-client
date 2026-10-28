package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyInput

fun interface FormatPaymentKeyInput {
    operator fun invoke(
        type: PaymentKeyType,
        input: String,
    ): PaymentKeyInput
}
