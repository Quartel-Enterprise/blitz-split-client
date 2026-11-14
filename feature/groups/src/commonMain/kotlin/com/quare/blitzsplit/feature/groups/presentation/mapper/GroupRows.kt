package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.CurrencyBalances
import com.quare.blitzsplit.core.expenses.domain.model.GroupActivity
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.hasBills
import com.quare.blitzsplit.core.expenses.domain.model.isEveryoneSettled
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.FaceStack
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.model.toStackedFaces
import com.quare.blitzsplit.feature.groups.presentation.model.ActivityAge
import com.quare.blitzsplit.feature.groups.presentation.model.BalanceTotal
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSummary
import com.quare.blitzsplit.feature.groups.presentation.model.LastActivity
import com.quare.blitzsplit.feature.groups.presentation.model.MemberFace
import kotlinx.datetime.daysUntil

// Why: "1 semana" up to "3 semanas", and from the 28th day on, the date.
private const val DAYS_IN_WEEK = 7
private const val MAX_WEEKS = 4

// Why: macOS's Membros column names the first three.
private const val LISTED_NAMES = 3

// Why: in the order "Rachar em qual grupo?" lists them too, as the design's "Em aberto" and "Quitados" both list them.
internal fun rowsOf(
    groups: List<GroupWithMoney>,
    clock: ActivityClock,
): List<GroupRow> = groups
    .map { rowOf(it, clock) }
    .sortedWith(GroupActivity.compareMostRecentFirst { it.lastActivity.epochMillis })

internal fun rowOf(
    item: GroupWithMoney,
    clock: ActivityClock,
): GroupRow {
    val group = item.group
    val byCurrency = GroupBalances.getByCurrency(group, item.expenses, item.settlements)
    val me = group.members.firstOrNull { it.isMe }
    // Why: a row has one amount: the group's default currency, or the first one with something open for this person.
    val shown = byCurrency.firstOrNull { me != null && !it.balances.balanceOf(me.id).isSettled } ?: byCurrency.first()
    val myBalance = me?.let { shown.balances.balanceOf(it.id) }
    val receives = myBalance?.receivesMinor ?: 0L
    val owes = myBalance?.owesMinor ?: 0L
    val pendingBill = item.expenses.filter { it.isPending }.maxByOrNull { it.updatedAt }
    val activityAt = GroupActivity.latestAtOf(
        groupUpdatedAt = group.updatedAt,
        expenses = item.expenses,
        settlements = item.settlements,
    )
    return GroupRow(
        id = group.id,
        name = group.name,
        initials = Initials.of(group.name),
        color = group.color,
        photoUrl = group.photo?.toImageUrl(),
        faces = group.members.toStackedFaces().map {
            MemberFace(
                initial = Initials.firstOf(it.name),
                color = it.avatarColor,
                photoUrl = it.photoUrl,
            )
        },
        extraMemberCount = FaceStack.extraCountOf(group.members.size),
        memberCount = group.members.size,
        memberNames = group.members.take(LISTED_NAMES).map { it.name },
        isPending = group.isPending ||
            pendingBill != null ||
            item.settlements.any { it.isPending } ||
            item.pendingDeletionCount > 0,
        pendingBillDescription = pendingBill?.description,
        currency = shown.currency,
        balanceKind = balanceKindOf(byCurrency, receives - owes),
        netMinor = receives - owes,
        receivesMinor = receives,
        owesMinor = owes,
        lastActivity = activityOf(activityAt, clock),
    )
}

/*
 * Why: quitado is nobody owing anybody once there were bills, as the group's own Saldos says; until then a group
 * with only payments left (their bill deleted) still counts as open.
 */
private fun balanceKindOf(
    balances: List<CurrencyBalances>,
    netMinor: Long,
): GroupBalanceKind = when {
    balances.isEveryoneSettled -> GroupBalanceKind.EVERYONE_SETTLED
    !balances.hasBills && balances.all { it.balances.debts.isEmpty() } -> GroupBalanceKind.NO_EXPENSES
    netMinor > 0 -> GroupBalanceKind.TO_RECEIVE
    netMinor < 0 -> GroupBalanceKind.YOU_OWE
    else -> GroupBalanceKind.YOU_ARE_SETTLED
}

// Why: how long ago each row last changed, counted again now; the order stays, since it goes by the time of the change.
internal fun agedRowsOf(
    rows: List<GroupRow>,
    clock: ActivityClock,
): List<GroupRow> = rows.map { it.copy(lastActivity = activityOf(it.lastActivity.epochMillis, clock)) }

internal fun activityOf(
    epochMillis: Long,
    clock: ActivityClock,
): LastActivity {
    val day = SpentDay.dateOf(epochMillis, clock.timeZone)
    val today = SpentDay.dateOf(clock.nowMillis, clock.timeZone)
    // Why: a clock set back shows a change "from the future" as today, never as days ahead.
    val days = day.daysUntil(today).coerceAtLeast(0)
    val weeks = days / DAYS_IN_WEEK
    val (age, count) = when {
        days == 0 -> ActivityAge.TODAY to 0
        days == 1 -> ActivityAge.YESTERDAY to 0
        days < DAYS_IN_WEEK -> ActivityAge.DAYS to days
        weeks < MAX_WEEKS -> ActivityAge.WEEKS to weeks
        else -> ActivityAge.DATE to 0
    }
    return LastActivity(
        epochMillis = epochMillis,
        age = age,
        count = count,
    )
}

/*
 * Why: "Saldo geral" is the global Saldos' totals (PeopleBalances), one per currency, so the two never differ: the
 * same pairs, netted per person. The group counts are the groups where this device's user has a net in that currency,
 * each group's own (a group's pair with someone whose groups offset each other still counts for its group).
 */
internal fun summaryOf(
    people: PeopleBalances,
    groups: List<GroupWithMoney>,
): GroupsSummary {
    val groupNets = groups.flatMap { item ->
        val me = item.group.members.firstOrNull { it.isMe }
        GroupBalances.getByCurrency(item.group, item.expenses, item.settlements).mapNotNull { (currency, balances) ->
            me?.let { currency to balances.balanceOf(it.id).netMinor }
        }
    }
    return GroupsSummary(
        totals = people.totals.map { total ->
            val nets = groupNets.filter { (currency, _) -> currency == total.currency }.map { (_, net) -> net }
            BalanceTotal(
                currency = total.currency,
                netMinor = total.netMinor,
                toReceiveMinor = total.toReceiveMinor,
                toPayMinor = total.toPayMinor,
                toReceiveGroupCount = nets.count { it > 0 },
                toPayGroupCount = nets.count { it < 0 },
            )
        },
    )
}

// Why: "na fila" counts each group, bill and payment waiting to go up as one change, a deleted one included.
internal fun pendingChangeCountOf(groups: List<GroupWithMoney>): Int = groups.sumOf { item ->
    (if (item.group.isPending) 1 else 0) +
        item.expenses.count { it.isPending } +
        item.settlements.count { it.isPending } +
        item.pendingDeletionCount
}
