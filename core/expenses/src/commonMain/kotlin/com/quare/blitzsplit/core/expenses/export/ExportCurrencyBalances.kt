package com.quare.blitzsplit.core.expenses.export

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: positive when the others owe the member, negative when they owe, in one currency.
data class ExportCurrencyBalances(
    val currency: GroupCurrency,
    val members: List<ExportAmount>,
)
