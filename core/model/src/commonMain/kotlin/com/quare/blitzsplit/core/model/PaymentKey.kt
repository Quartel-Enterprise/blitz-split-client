package com.quare.blitzsplit.core.model

/*
 * Why: a key to receive payments with, its value normalized (11 CPF digits, "+5511912345678", a lowercase
 * address), so every UI formats or masks the same string.
 */
data class PaymentKey(
    val id: String,
    val type: PaymentKeyType,
    val value: String,
    val isDefault: Boolean,
    val addedAt: Long,
)
