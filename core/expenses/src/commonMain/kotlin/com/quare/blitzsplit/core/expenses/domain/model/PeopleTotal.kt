package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: "Tudo", "Você deve" and "Te devem" in one currency, since amounts of two currencies never add up. Each person
 * counts their net, so the net always matches the home's "Saldo geral".
 */
data class PeopleTotal(
    val currency: GroupCurrency,
    val netMinor: Long,
    val toReceiveMinor: Long,
    // Why: positive, the sum of what you owe the people you owe; each UI writes its minus.
    val toPayMinor: Long,
    // Why: "4 pessoas".
    val toReceiveCount: Int,
    val toPayCount: Int,
)
