package com.quare.blitzsplit.core.profile.domain.usecase

fun interface DeletePaymentKey {
    suspend operator fun invoke(keyId: String)
}
