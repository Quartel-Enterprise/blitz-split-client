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
    // Why: "a receber, já compensado", "a pagar, já compensado" or "tudo acertado", from the first currency with a net.
    val kind: GroupBalanceKind
        get() {
            val net = totals.firstOrNull { it.netMinor != 0L }?.netMinor ?: 0L
            return when {
                net > 0 -> GroupBalanceKind.TO_RECEIVE
                net < 0 -> GroupBalanceKind.YOU_OWE
                else -> GroupBalanceKind.YOU_ARE_SETTLED
            }
        }

    // Why: "Nada a receber" and "Nada a pagar", disabled while there is nothing either way.
    val canOpenToReceive: Boolean
        get() = totals.any { it.toReceiveMinor > 0 }

    val canOpenToPay: Boolean
        get() = totals.any { it.toPayMinor > 0 }
}
