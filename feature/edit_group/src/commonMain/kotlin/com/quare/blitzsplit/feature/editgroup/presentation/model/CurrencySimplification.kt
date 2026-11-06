package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.DebtSimplificationSummary
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: one row of the line under "Simplificar dívidas": "BRL · 7 → 5", each currency simplifies on its own.
data class CurrencySimplification(
    val currency: GroupCurrency,
    val summary: DebtSimplificationSummary,
)
