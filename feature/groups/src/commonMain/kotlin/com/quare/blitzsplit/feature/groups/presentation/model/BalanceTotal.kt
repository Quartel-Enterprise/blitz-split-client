package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: "Saldo geral", "A receber" and "A pagar" in one currency, since amounts of two currencies never add up. Each
 * group counts its net: a group where you get 120,00 and owe 218,00 adds 98,00 to "A pagar" only.
 */
data class BalanceTotal(
    val currency: GroupCurrency,
    val netMinor: Long,
    val toReceiveMinor: Long,
    // Why: positive, the sum of what you owe in the groups where your net is negative; each UI writes its minus.
    val toPayMinor: Long,
    // Why: "em 2 grupos".
    val toReceiveGroupCount: Int,
    val toPayGroupCount: Int,
)
