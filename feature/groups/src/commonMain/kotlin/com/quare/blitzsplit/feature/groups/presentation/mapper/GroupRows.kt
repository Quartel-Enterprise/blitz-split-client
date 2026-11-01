package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.GroupActivity
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.groups.presentation.model.ActivityAge
import com.quare.blitzsplit.feature.groups.presentation.model.BalanceTotal
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSummary
import com.quare.blitzsplit.feature.groups.presentation.model.LastActivity
import com.quare.blitzsplit.feature.groups.presentation.model.MemberFace
import kotlinx.datetime.daysUntil

// Why: three faces on a row, the rest "+N"; "1 semana" up to "3 semanas", and from the 28th day on, the date.
private const val MAX_FACES = 3
private const val DAYS_IN_WEEK = 7
private const val MAX_WEEKS = 4

/*
 * Why: the most recent activity first, the groups' own order (the latest change first) breaking a tie, as the
 * design's "Em aberto" and "Quitados" both list them.
 */
internal fun rowsOf(
    groups: List<GroupWithMoney>,
    clock: ActivityClock,
): List<GroupRow> = groups
    .map { rowOf(it, clock) }
    .withIndex()
    .sortedWith(compareByDescending<IndexedValue<GroupRow>> { it.value.lastActivity.epochMillis }.thenBy { it.index })
    .map { it.value }

internal fun rowOf(
    item: GroupWithMoney,
    clock: ActivityClock,
): GroupRow {
    val group = item.group
    val balances = GroupBalances.of(item.expenses, item.settlements)
    val me = group.members.firstOrNull { it.isMe }
    val myBalance = me?.let { balances.balanceOf(it.id) }
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
        faces = group.members.take(MAX_FACES).map {
            MemberFace(
                initial = Initials.firstOf(it.name),
                color = it.avatarColor,
            )
        },
        extraMemberCount = (group.members.size - MAX_FACES).coerceAtLeast(0),
        memberCount = group.members.size,
        memberNames = group.members.take(MAX_FACES).map { it.name },
        isPending = group.isPending ||
            pendingBill != null ||
            item.settlements.any { it.isPending } ||
            item.pendingDeletionCount > 0,
        pendingBillDescription = pendingBill?.description,
        currency = group.currency,
        balanceKind = balanceKindOf(balances, receives - owes),
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
    balances: GroupBalances,
    netMinor: Long,
): GroupBalanceKind = when {
    balances.isEveryoneSettled -> GroupBalanceKind.EVERYONE_SETTLED
    !balances.hasBills && balances.debts.isEmpty() -> GroupBalanceKind.NO_EXPENSES
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

// Why: one total per currency, the currency with the most groups first and then by its code, so the order never flips.
internal fun summaryOf(rows: List<GroupRow>): GroupsSummary = GroupsSummary(
    totals = rows
        .groupBy { it.currency }
        .entries
        .sortedWith(
            compareByDescending<Map.Entry<GroupCurrency, List<GroupRow>>> { it.value.size }.thenBy { it.key.code },
        ).map { (currency, ofCurrency) ->
            val receiving = ofCurrency.filter { it.netMinor > 0 }
            val paying = ofCurrency.filter { it.netMinor < 0 }
            BalanceTotal(
                currency = currency,
                netMinor = ofCurrency.sumOf { it.netMinor },
                toReceiveMinor = receiving.sumOf { it.netMinor },
                toPayMinor = -paying.sumOf { it.netMinor },
                toReceiveGroupCount = receiving.size,
                toPayGroupCount = paying.size,
            )
        },
)

// Why: "na fila" counts each group, bill and payment waiting to go up as one change, a deleted one included.
internal fun pendingChangeCountOf(groups: List<GroupWithMoney>): Int = groups.sumOf { item ->
    (if (item.group.isPending) 1 else 0) +
        item.expenses.count { it.isPending } +
        item.settlements.count { it.isPending } +
        item.pendingDeletionCount
}
