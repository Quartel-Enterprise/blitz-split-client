package com.quare.blitzsplit.feature.profile.presentation.mapper

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyValidation
import com.quare.blitzsplit.core.profile.domain.usecase.ValidatePaymentKey
import com.quare.blitzsplit.feature.profile.presentation.model.PaymentKeyItem

// Why: the rules tell the format the row names ("· Taproot"); a key they reject has no row.
internal fun PaymentKey.toItemOrNull(validatePaymentKey: ValidatePaymentKey): PaymentKeyItem? {
    val validation = validatePaymentKey(type, value, otherKeys = emptyList())
    val format = (validation as? PaymentKeyValidation.Valid)?.format ?: return null
    return PaymentKeyItem(
        id = id,
        type = type,
        format = format,
        maskedValue = value.toMaskedPaymentKey(format),
        isDefault = isDefault,
    )
}
