package com.quare.blitzsplit.core.profile.domain.model

import com.quare.blitzsplit.core.model.PaymentKeyError
import com.quare.blitzsplit.core.model.PaymentKeyFormat

sealed interface PaymentKeyValidation {
    data class Valid(
        val value: String,
        val format: PaymentKeyFormat,
    ) : PaymentKeyValidation

    data class Invalid(
        val error: PaymentKeyError,
    ) : PaymentKeyValidation
}
