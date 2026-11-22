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

// Why: Início's big amount and the balance widget's: the first currency with a net, else the first one; null with no group.
fun List<PeopleTotal>.findMain(): PeopleTotal? = firstOrNull { it.netMinor != 0L } ?: firstOrNull()

/*
 * Why: "Te devem" and "Você deve", on Início and the widget, in the first currency with something either way, so
 * neither says R$ 0,00 while another currency has some.
 */
fun List<PeopleTotal>.findToReceive(): PeopleTotal? = firstOrNull { it.toReceiveMinor > 0 }

fun List<PeopleTotal>.findToPay(): PeopleTotal? = firstOrNull { it.toPayMinor > 0 }
