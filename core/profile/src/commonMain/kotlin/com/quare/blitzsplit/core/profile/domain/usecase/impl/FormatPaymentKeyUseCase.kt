package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyRules
import com.quare.blitzsplit.core.profile.domain.usecase.FormatPaymentKey

class FormatPaymentKeyUseCase : FormatPaymentKey {
    override fun invoke(key: PaymentKey): String = PaymentKeyRules.formatValue(key.type, key.value)
}
