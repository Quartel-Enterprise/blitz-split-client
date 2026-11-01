package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal

/*
 * Why: Início's "Saldo geral", with "Te devem" and "Você deve" beside it. Its net is Grupos' Saldo geral; the two
 * buttons sum the people as Saldos (flow 8) does, since they open Saldos on Te devem and Você deve. One total per
 * currency, the currency with the most groups first.
 */
data class HomeSummary(
    val totals: List<PeopleTotal>,
) {
    /*
     * Why: the big amount: the first currency with a net, else the first one (R$ 0,00, "tudo acertado"); the other
     * currencies go on lines under it. Null with no group.
     */
    val main: PeopleTotal?
        get() = totals.firstOrNull { it.netMinor != 0L } ?: totals.firstOrNull()

    // Why: the other currencies with a net, on lines under the big amount, as Grupos' Saldo geral lists them.
    val others: List<PeopleTotal>
        get() = totals.filter { it != main && it.netMinor != 0L }

    // Why: "a receber, já compensado", "a pagar, já compensado" or "tudo acertado", the big amount's.
    val kind: GroupBalanceKind
        get() {
            val net = main?.netMinor ?: 0L
            return when {
                net > 0 -> GroupBalanceKind.TO_RECEIVE
                net < 0 -> GroupBalanceKind.YOU_OWE
                else -> GroupBalanceKind.YOU_ARE_SETTLED
            }
        }

    /*
     * Why: "Te devem" and "Você deve" with their amount, in the first currency with something either way, so neither
     * says R$ 0,00 while another currency has some; null is "Nada a receber" and "Nada a pagar", disabled.
     */
    val toReceive: PeopleTotal?
        get() = totals.firstOrNull { it.toReceiveMinor > 0 }

    val toPay: PeopleTotal?
        get() = totals.firstOrNull { it.toPayMinor > 0 }

    val canOpenToReceive: Boolean
        get() = toReceive != null

    val canOpenToPay: Boolean
        get() = toPay != null
}
