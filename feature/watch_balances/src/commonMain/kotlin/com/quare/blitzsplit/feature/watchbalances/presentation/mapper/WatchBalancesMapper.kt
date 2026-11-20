package com.quare.blitzsplit.feature.watchbalances.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.PersonBalance
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchDebt
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupRow
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchTotal
import kotlin.math.abs

/*
 * Why: the watch draws the same numbers as the global Balances, regrouped by group: each group's net is the sum of
 * its pairs with you, so the totals on top always add up with the rows. A group shows while any pair with you is
 * open, even when they offset each other to a net of zero, since each still has its own "I paid".
 */
internal object WatchBalancesMapper {
    fun totalsOf(balances: PeopleBalances): List<WatchTotal> = balances.totals
        .filter { it.toReceiveMinor != 0L || it.toPayMinor != 0L }
        .map { total ->
            WatchTotal(
                currency = total.currency,
                netMinor = total.netMinor,
                toReceiveMinor = total.toReceiveMinor,
                toPayMinor = total.toPayMinor,
            )
        }

    fun groupsOf(
        balances: PeopleBalances,
        money: Map<String, GroupMoney>,
    ): List<WatchGroupRow> = everyoneOf(balances)
        .flatMap { person -> person.groups.map { part -> person.currency to part } }
        .groupBy { (currency, part) -> part.groupId to currency }
        .map { (key, parts) ->
            val (groupId, currency) = key
            val first = parts.first().second
            val net = parts.sumOf { (_, part) -> part.amountMinor }
            WatchGroupRow(
                groupId = groupId,
                name = first.groupName,
                initials = first.groupInitials,
                color = first.groupColor,
                photoUrl = first.groupPhotoUrl,
                currency = currency,
                netMinor = net,
                isQueued = isQueued(money[groupId], currency),
            )
        }.sortedByDescending { abs(it.netMinor) }

    fun debtsOf(
        balances: PeopleBalances,
        groupId: String,
        currency: GroupCurrency,
    ): List<WatchDebt> = everyoneOf(balances)
        .filter { it.currency == currency }
        .flatMap { person ->
            person.groups
                .filter { it.groupId == groupId && it.amountMinor != 0L }
                .map { part ->
                    WatchDebt(
                        memberId = part.memberId,
                        name = person.name,
                        initials = person.face?.initials ?: person.name?.let(Initials::firstOf).orEmpty(),
                        color = person.face?.color,
                        photoUrl = person.face?.photoUrl,
                        amountMinor = part.amountMinor,
                    )
                }
        }.sortedByDescending { abs(it.amountMinor) }

    // Why: the pair's two ends in the group, as "Acertar" pays them: you, and the member you owe.
    fun meMemberIdOf(
        balances: PeopleBalances,
        groupId: String,
    ): String? = everyoneOf(balances)
        .flatMap { it.groups }
        .firstOrNull { it.groupId == groupId }
        ?.meMemberId

    // Why: whoever has an open pair with you in some group, also when their groups offset each other to zero overall.
    fun everyoneOf(balances: PeopleBalances): List<PersonBalance> = balances.people + balances.evenPeople

    fun isQueued(
        money: GroupMoney?,
        currency: GroupCurrency,
    ): Boolean = money?.settlements.orEmpty().any { it.isPending && it.currency == currency }

    fun queuedCountOf(money: Map<String, GroupMoney>): Int = money.values.sumOf { group ->
        group.settlements.count { it.isPending }
    }
}
