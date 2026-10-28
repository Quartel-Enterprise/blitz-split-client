package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyError
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyRules
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyValidation
import com.quare.blitzsplit.core.profile.domain.usecase.ValidatePaymentKey

class ValidatePaymentKeyUseCase : ValidatePaymentKey {
    override fun invoke(
        type: PaymentKeyType,
        input: String,
        otherKeys: List<PaymentKey>,
    ): PaymentKeyValidation {
        val validation = PaymentKeyRules.validate(type, input)
        val isSaved = validation is PaymentKeyValidation.Valid &&
            otherKeys.any { it.type == type && it.value == validation.value }
        return if (isSaved) PaymentKeyValidation.Invalid(PaymentKeyError.ALREADY_SAVED) else validation
    }
}
