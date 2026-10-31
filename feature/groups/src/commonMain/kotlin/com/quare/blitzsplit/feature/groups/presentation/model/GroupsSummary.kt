package com.quare.blitzsplit.feature.groups.presentation.model

/*
 * Why: the "Saldo geral" card. One total per currency, the currency with the most groups first, so a person with a
 * group abroad sees each currency's sum and never a sum of two currencies.
 */
data class GroupsSummary(
    val totals: List<BalanceTotal>,
) {
    // Why: "a receber, somando os grupos" or "a pagar, somando os grupos", from the first currency's net.
    val kind: GroupBalanceKind
        get() {
            val net = totals.firstOrNull()?.netMinor ?: 0L
            return when {
                net > 0 -> GroupBalanceKind.TO_RECEIVE
                net < 0 -> GroupBalanceKind.YOU_OWE
                else -> GroupBalanceKind.YOU_ARE_SETTLED
            }
        }

    // Why: "A receber" and "A pagar" are buttons, disabled while there is nothing to receive or to pay.
    val canOpenToReceive: Boolean
        get() = totals.any { it.toReceiveMinor > 0 }

    val canOpenToPay: Boolean
        get() = totals.any { it.toPayMinor > 0 }

    val toReceiveGroupCount: Int
        get() = totals.sumOf { it.toReceiveGroupCount }

    val toPayGroupCount: Int
        get() = totals.sumOf { it.toPayGroupCount }
}
