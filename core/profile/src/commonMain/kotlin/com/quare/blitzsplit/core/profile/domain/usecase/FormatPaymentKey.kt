package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.PaymentKey

fun interface FormatPaymentKey {
    // Why: a stored key as people read it ("123.456.789-09", "+55 (11) 91234-5678"), to edit it or copy it.
    operator fun invoke(key: PaymentKey): String
}
