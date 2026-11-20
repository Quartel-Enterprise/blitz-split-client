package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.groups.domain.model.Group

/*
 * Why: what the balance widget (the iPhone's Home Screen and Lock Screen, later the watch's complication and Smart
 * Stack card) shows, worked out by the app from the device's data and handed to a process that has no Room: the overall
 * balance as Início shows it, with "You're owed" and "You owe" in its currency, and the groups where this device's
 * user has an open balance.
 */
data class BalanceSnapshot(
    // Why: Início's big amount, the first currency with a net, else the first one; null with no group.
    val overall: PeopleTotal?,
    // Why: the groups with an open balance, the most recently active first; a settled group never shows.
    val openGroups: List<OpenGroupBalance>,
    // Why: "All settled": nobody owes you and you owe nobody, in any currency, or there is no group yet.
    val isSettled: Boolean,
) {
    companion object {
        // Why: groups already in the home's order, so the widget's groups are the ones Início lists first.
        fun of(
            groups: List<Group>,
            money: Map<String, GroupMoney>,
        ): BalanceSnapshot {
            val totals = PeopleBalances.of(groups, money).totals
            return BalanceSnapshot(
                overall = totals.firstOrNull { it.netMinor != 0L } ?: totals.firstOrNull(),
                openGroups = groups.flatMap { group -> openBalancesOf(group, money[group.id] ?: GroupMoney.NONE) },
                isSettled = totals.all { it.toReceiveMinor == 0L && it.toPayMinor == 0L },
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
