package com.quare.blitzsplit.feature.payingmethod.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType

// Why: what the form saves, compared with how it opened to tell unsaved input.
data class PayingMethodInput(
    val type: PayingMethodType,
    val name: String,
    val cardColor: CardColor?,
    val isDefault: Boolean,
)
