package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: a group's pairs in one currency; the balances of a group with bills in two currencies are two of these.
data class CurrencyBalances(
    val currency: GroupCurrency,
    val balances: GroupBalances,
)
