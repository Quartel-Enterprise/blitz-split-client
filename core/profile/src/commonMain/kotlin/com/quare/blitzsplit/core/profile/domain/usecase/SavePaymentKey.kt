package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.PaymentKeyType

fun interface SavePaymentKey {
    // Why: keyId null adds a key; value is the normalized one ValidatePaymentKey returned.
    suspend operator fun invoke(
        keyId: String?,
        type: PaymentKeyType,
        value: String,
        isDefault: Boolean,
    )
}
