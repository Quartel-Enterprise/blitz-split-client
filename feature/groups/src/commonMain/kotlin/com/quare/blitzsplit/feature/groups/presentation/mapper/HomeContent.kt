package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.PersonBalance
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.HomeBill
import com.quare.blitzsplit.feature.groups.presentation.model.HomeBillDay
import com.quare.blitzsplit.feature.groups.presentation.model.HomeGroup
import com.quare.blitzsplit.feature.groups.presentation.model.HomePerson
import kotlin.math.abs

// Why: Início's cards and rows: three groups, five bills.
private const val MAX_GROUPS = 3
private const val MAX_BILLS = 5

/*
 * Why: whom you owe comes first, then who owes you, the largest first in each; among equals, the order Saldos lists
 * them in, so the row never flips.
 */
internal fun homePeopleOf(balances: PeopleBalances): List<HomePerson> = balances.people
    .withIndex()
    .sortedWith(
        compareBy<IndexedValue<PersonBalance>> { it.value.netMinor > 0 }
            .thenByDescending { abs(it.value.netMinor) }
            .thenBy { it.index },
    ).map { (_, person) ->
        HomePerson(
            key = person.key,
            name = person.name,
            initials = person.name?.let(Initials::of).orEmpty(),
            color = person.face?.color,
            currency = person.currency,
            netMinor = person.netMinor,
            isGuest = person.isGuest,
        )
    }

// Why: the pinned groups first, then the latest activity (the rows' order); a group where everyone settled stays out.
internal fun homeGroupsOf(
    rows: List<GroupRow>,
    pinnedGroupIds: Set<String>,
): List<HomeGroup> = rows
    .filter { !it.isSettled }
    .sortedBy { it.id !in pinnedGroupIds }
    .take(MAX_GROUPS)
    .map {
        HomeGroup(
            row = it,
            isPinned = it.id in pinnedGroupIds,
        )
    }

/*
 * Why: the five latest bills this device's user paid or has a part in, from every group, by their "Quando"; the
 * payments stay in Atividade.
 */
internal fun homeBillDaysOf(
    groups: List<GroupWithMoney>,
    clock: ActivityClock,
): List<HomeBillDay> = groups
    .flatMap { item ->
        val meId = item.group.members
            .firstOrNull { it.isMe }
            ?.id ?: return@flatMap emptyList()
        val payersOf = PayersOfGroup(item.group)
        item.expenses.mapNotNull { expense ->
            val myBalance = expense.balanceOf(meId) ?: return@mapNotNull null
            expense.spentAt to HomeBill(
                id = expense.id,
                groupId = item.group.id,
                groupName = item.group.name,
                description = expense.description,
                totalMinor = expense.amountMinor,
                currency = item.group.currency,
                payers = payersOf.of(expense),
                myBalanceMinor = myBalance,
                isPending = expense.isPending,
            )
        }
    }.sortedByDescending { (spentAt, _) -> spentAt }
    .take(MAX_BILLS)
    .groupBy { (spentAt, _) -> SpentDay.dateOf(spentAt, clock.timeZone) }
    .map { (date, bills) ->
        HomeBillDay(
            dayStartMillis = SpentDay.startOf(date, clock.timeZone),
            relativeDay = SpentDay.relativeDayOf(date, clock.nowMillis, clock.timeZone),
            bills = bills.map { (_, bill) -> bill },
        )
    }
