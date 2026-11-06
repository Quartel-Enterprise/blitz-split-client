package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: a group's pairs in one currency; the balances of a group with bills in two currencies are two of these.
data class CurrencyBalances(
    val currency: GroupCurrency,
    val balances: GroupBalances,
)

// Why: the balance the screen shows for the currency picked; the group's default (the first) while none is, or the pick left.
fun List<CurrencyBalances>.findSelected(currency: GroupCurrency?): CurrencyBalances? =
    firstOrNull { it.currency == currency } ?: firstOrNull()

// Why: the chips of the currency selector, drawn only while the group has money in more than one currency.
val List<CurrencyBalances>.currencies: List<GroupCurrency>
    get() = map { it.currency }

// Why: "Todo mundo acertou" takes every currency settled, and "Ninguém deve nada ainda" none with a bill.
val List<CurrencyBalances>.hasBills: Boolean
    get() = any { it.balances.hasBills }

val List<CurrencyBalances>.isEveryoneSettled: Boolean
    get() = hasBills && all { it.balances.debts.isEmpty() }

// Why: every open pair of the group, each with the currency it is in, the default currency's first.
val List<CurrencyBalances>.openDebts: List<Pair<GroupCurrency, Debt>>
    get() = flatMap { (currency, balances) -> balances.debts.map { currency to it } }
