package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.PersonBalance
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.FaceStack
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.model.toStackedFaces
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
            photoUrl = person.face?.photoUrl,
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
    categories: CategoryTree,
    clock: ActivityClock,
): List<HomeBillDay> = billDaysOf(homeBillsOf(latestExpensesOf(groups), categories), clock)

// Why: read again only when the bills change; the five kept are all that read the categories.
internal fun latestExpensesOf(groups: List<GroupWithMoney>): List<LatestExpense> = groups
    .flatMap { item ->
        val meId = item.group.members
            .firstOrNull { it.isMe }
            ?.id ?: return@flatMap emptyList()
        item.expenses.mapNotNull { expense ->
            LatestExpense(
                group = item.group,
                expense = expense,
                myBalanceMinor = expense.balanceOf(meId) ?: return@mapNotNull null,
            )
        }
    }.sortedByDescending { it.expense.spentAt }
    .take(MAX_BILLS)

/*
 * Why: the categories are every group's, merged, as Atividade reads them: a group's own category has the id only it
 * uses.
 */
internal fun homeBillsOf(
    latest: List<LatestExpense>,
    categories: CategoryTree,
): List<DatedHomeBill> = latest.map { (group, expense, myBalanceMinor) ->
    val payersOf = PayersOfGroup(group)
    val payerIds = payersOf.sortedIdsOf(expense)
    DatedHomeBill(
        spentAt = expense.spentAt,
        bill = HomeBill(
            id = expense.id,
            groupId = group.id,
            groupName = group.name,
            groupInitials = Initials.of(group.name),
            groupColor = group.color,
            groupPhotoUrl = group.photo?.toImageUrl(),
            description = expense.description,
            totalMinor = expense.amountMinor,
            currency = expense.currency,
            payers = payersOf.namesOf(payerIds),
            payerFaces = payersOf.facesOf(payerIds.toStackedFaces()),
            extraPayerCount = FaceStack.extraCountOf(payerIds.size),
            myBalanceMinor = myBalanceMinor,
            category = categories.resolve(expense.categoryId),
            isPending = expense.isPending,
        ),
    )
}

internal fun billDaysOf(
    bills: List<DatedHomeBill>,
    clock: ActivityClock,
): List<HomeBillDay> = bills
    .groupBy { SpentDay.dateOf(it.spentAt, clock.timeZone) }
    .map { (date, dated) ->
        HomeBillDay(
            dayStartMillis = SpentDay.startOf(date, clock.timeZone),
            relativeDay = SpentDay.relativeDayOf(date, clock.nowMillis, clock.timeZone),
            bills = dated.map { it.bill },
        )
    }
