package com.quare.blitzsplit.core.profile.domain.model

data class PaymentKeyInput(
    val text: String,
    // Why: a CPF or phone with every digit typed can be judged, while a shorter one is still being typed.
    val isComplete: Boolean,
)
