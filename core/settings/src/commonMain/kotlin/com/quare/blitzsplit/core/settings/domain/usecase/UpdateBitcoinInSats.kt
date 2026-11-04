package com.quare.blitzsplit.core.settings.domain.usecase

fun interface UpdateBitcoinInSats {
    suspend operator fun invoke(isOn: Boolean)
}
