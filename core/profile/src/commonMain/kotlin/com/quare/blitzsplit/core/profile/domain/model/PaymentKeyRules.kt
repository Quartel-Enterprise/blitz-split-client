package com.quare.blitzsplit.core.profile.domain.model

import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod

internal object PaymentKeyRules {
    fun validate(
        type: PaymentKeyType,
        input: String,
    ): PaymentKeyValidation = when (type.method) {
        PaymentMethod.PIX -> PixKeyRules.validate(type, input)
        PaymentMethod.BITCOIN -> BitcoinKeyRules.validate(type, input)
    }

    fun detect(
        method: PaymentMethod,
        input: String,
    ): PaymentKeyType? = when (method) {
        PaymentMethod.PIX -> PixKeyRules.detect(input)
        PaymentMethod.BITCOIN -> BitcoinKeyRules.detect(input)
    }

    fun formatInput(
        type: PaymentKeyType,
        input: String,
    ): PaymentKeyInput = when (type.method) {
        PaymentMethod.PIX -> PixKeyRules.formatInput(type, input)

        PaymentMethod.BITCOIN -> PaymentKeyInput(
            text = input,
            isComplete = false,
        )
    }

    fun formatValue(
        type: PaymentKeyType,
        value: String,
    ): String = when (type.method) {
        PaymentMethod.PIX -> PixKeyRules.formatValue(type, value)
        PaymentMethod.BITCOIN -> value
    }
}
