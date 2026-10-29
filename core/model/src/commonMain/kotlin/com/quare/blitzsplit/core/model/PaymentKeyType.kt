package com.quare.blitzsplit.core.model

enum class PaymentKeyType(
    val method: PaymentMethod,
) {
    CPF(PaymentMethod.PIX),
    PHONE(PaymentMethod.PIX),
    EMAIL(PaymentMethod.PIX),
    RANDOM(PaymentMethod.PIX),
    LIGHTNING(PaymentMethod.BITCOIN),
    ONCHAIN(PaymentMethod.BITCOIN),
    ;

    companion object {
        fun of(method: PaymentMethod): List<PaymentKeyType> = entries.filter { it.method == method }
    }
}
