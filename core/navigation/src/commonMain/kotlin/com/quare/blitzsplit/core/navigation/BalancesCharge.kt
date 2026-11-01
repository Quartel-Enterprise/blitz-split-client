package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: Contatos' "Cobrar" (11.2): Saldos' message (8.2) for that person (PersonBalance.key) in the currency shown.
data class BalancesCharge(
    val personKey: String,
    val currency: GroupCurrency,
)
