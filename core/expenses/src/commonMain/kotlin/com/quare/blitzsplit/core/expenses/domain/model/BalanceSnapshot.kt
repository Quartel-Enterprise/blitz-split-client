package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.groups.domain.model.Group

/*
 * Why: what the balance widget (the iPhone's Home Screen and Lock Screen, later the watch's complication and Smart
 * Stack card) shows, worked out by the app from the device's data and handed to a process that has no Room: the overall
 * balance as Início shows it, with its "Owe you" and "You owe", and the groups where this device's user has an open
 * balance.
 */
data class BalanceSnapshot(
    // Why: Início's big amount (findMain); null with no group.
    val overall: PeopleTotal?,
    // Why: Início's "Te devem" and "Você deve", each in the first currency with something that way; null for none.
    val toReceive: PeopleTotal?,
    val toPay: PeopleTotal?,
    // Why: the groups with an open balance, the most recently active first; a settled group never shows.
    val openGroups: List<OpenGroupBalance>,
    /*
     * Why: "All settled": nobody owes you and you owe nobody, in any currency, and no group is open either (someone who
     * owes you in one group what you owe them in another nets to zero, yet both groups are still open).
     */
    val isSettled: Boolean,
) {
    companion object {
        // Why: groups already in the home's order, so the widget's groups are the ones Início lists first.
        fun of(
            groups: List<Group>,
            money: Map<String, GroupMoney>,
        ): BalanceSnapshot {
            val totals = PeopleBalances.of(groups, money).totals
            val openGroups = groups.flatMap { group -> openBalancesOf(group, money[group.id] ?: GroupMoney.NONE) }
            return BalanceSnapshot(
                overall = totals.findMain(),
                toReceive = totals.findToReceive(),
                toPay = totals.findToPay(),
                openGroups = openGroups,
                isSettled = openGroups.isEmpty() && totals.all { it.toReceiveMinor == 0L && it.toPayMinor == 0L },
            )
        }

        // Why: a group with bills in two currencies is open in each one that has a net, as the group's Balances.
        private fun openBalancesOf(
            group: Group,
            money: GroupMoney,
        ): List<OpenGroupBalance> {
            val me = group.members.firstOrNull { it.isMe } ?: return emptyList()
            return GroupBalances
                .getByCurrency(group, money.expenses, money.settlements)
                .map { (currency, balances) ->
                    OpenGroupBalance(
                        groupId = group.id,
                        name = group.name,
                        currency = currency,
                        netMinor = balances.balanceOf(me.id).netMinor,
                    )
                }.filter { it.netMinor != 0L }
        }
    }
}
