package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyInput
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyRules
import com.quare.blitzsplit.core.profile.domain.usecase.FormatPaymentKeyInput

class FormatPaymentKeyInputUseCase : FormatPaymentKeyInput {
    override fun invoke(
        type: PaymentKeyType,
        input: String,
    ): PaymentKeyInput = PaymentKeyRules.formatInput(type, input)
}
