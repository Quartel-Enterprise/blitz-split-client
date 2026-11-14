package com.quare.blitzsplit.core.model

enum class PaymentKeyType(
    val method: PaymentMethod,
    // Why: a person has one CPF/CNPJ, one phone and one email key at most; random keys and Bitcoin may repeat.
    val isOnePerPerson: Boolean = false,
) {
    CPF(PaymentMethod.PIX, isOnePerPerson = true),
    PHONE(PaymentMethod.PIX, isOnePerPerson = true),
    EMAIL(PaymentMethod.PIX, isOnePerPerson = true),
    RANDOM(PaymentMethod.PIX),
    LIGHTNING(PaymentMethod.BITCOIN),
    ONCHAIN(PaymentMethod.BITCOIN),
    ;

    companion object {
        fun of(method: PaymentMethod): List<PaymentKeyType> = entries.filter { it.method == method }
    }
}
