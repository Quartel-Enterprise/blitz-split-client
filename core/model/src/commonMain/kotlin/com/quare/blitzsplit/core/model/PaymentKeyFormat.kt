package com.quare.blitzsplit.core.model

// Why: what a valid key turned out to be, finer than its type, so the UIs can say "Taproot (bc1p)" or "LNURL".
enum class PaymentKeyFormat(
    val type: PaymentKeyType,
) {
    CPF(PaymentKeyType.CPF),
    PHONE(PaymentKeyType.PHONE),
    EMAIL(PaymentKeyType.EMAIL),
    RANDOM(PaymentKeyType.RANDOM),
    LIGHTNING_ADDRESS(PaymentKeyType.LIGHTNING),
    LNURL(PaymentKeyType.LIGHTNING),
    SEGWIT(PaymentKeyType.ONCHAIN),
    TAPROOT(PaymentKeyType.ONCHAIN),
}
