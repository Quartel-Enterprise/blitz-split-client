package com.quare.blitzsplit.feature.globalbalances.presentation.model

import com.quare.blitzsplit.core.model.PaymentKey

data class ChargeUiState(
    // Why: null until Room is read; the dialog draws once the person's message is known.
    val charge: ChargeDraft?,
    // Why: the device user's Pix key that Cobrar offers to include, the default one; null without a Pix key.
    val pixKey: PaymentKey?,
)
