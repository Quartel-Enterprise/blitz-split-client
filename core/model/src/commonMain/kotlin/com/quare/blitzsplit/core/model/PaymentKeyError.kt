package com.quare.blitzsplit.core.model

// Why: each UI words what went wrong and how to fix it; the shared code only says which case it is.
enum class PaymentKeyError {
    INVALID_CPF,
    INVALID_PHONE,
    INVALID_EMAIL,
    INVALID_RANDOM,
    INVALID_LIGHTNING,
    LIGHTNING_INVOICE,
    INVALID_ONCHAIN,
    TESTNET_ADDRESS,
    ALREADY_SAVED,
}
