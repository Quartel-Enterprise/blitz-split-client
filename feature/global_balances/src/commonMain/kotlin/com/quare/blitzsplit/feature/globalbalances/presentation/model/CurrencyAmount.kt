package com.quare.blitzsplit.feature.globalbalances.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: an amount and its currency, since amounts of two currencies are written apart, never added up.
data class CurrencyAmount(
    val currency: GroupCurrency,
    val amountMinor: Long,
)
