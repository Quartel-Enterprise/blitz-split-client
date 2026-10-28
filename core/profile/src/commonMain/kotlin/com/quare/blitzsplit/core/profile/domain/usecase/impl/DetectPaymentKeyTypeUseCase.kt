package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyRules
import com.quare.blitzsplit.core.profile.domain.usecase.DetectPaymentKeyType

class DetectPaymentKeyTypeUseCase : DetectPaymentKeyType {
    override fun invoke(
        method: PaymentMethod,
        input: String,
    ): PaymentKeyType? = PaymentKeyRules.detect(method, input)
}
