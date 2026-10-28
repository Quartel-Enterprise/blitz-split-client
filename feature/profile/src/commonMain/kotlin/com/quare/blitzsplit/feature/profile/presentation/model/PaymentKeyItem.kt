package com.quare.blitzsplit.feature.profile.presentation.model

import com.quare.blitzsplit.core.model.PaymentKeyFormat
import com.quare.blitzsplit.core.model.PaymentKeyType

data class PaymentKeyItem(
    val id: String,
    val type: PaymentKeyType,
    // Why: "Bitcoin · On-chain" reads "bc1p5d7r…sxg3297 · Taproot", so the row needs the format too.
    val format: PaymentKeyFormat,
    // Why: masked here, so every UI hides the same characters; the settle screen shows it whole.
    val maskedValue: String,
    val isDefault: Boolean,
)
