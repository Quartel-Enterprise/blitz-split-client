package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.DateSpan
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.faceOf
import com.quare.blitzsplit.core.expenses.domain.model.findSpanOrNull
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.utils.TextFolding
import com.quare.blitzsplit.core.utils.TypoTolerantQuery
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesFilters
import com.quare.blitzsplit.feature.groups.presentation.model.ExpenseSort
import com.quare.blitzsplit.feature.groups.presentation.model.FilterGroupOption
import com.quare.blitzsplit.feature.groups.presentation.model.FilterPayerOption
import com.quare.blitzsplit.feature.groups.presentation.model.PAYER_KEY_ME
import com.quare.blitzsplit.feature.groups.presentation.model.ShareFilter
import kotlinx.datetime.TimeZone

/*
 * Why: each bill with its group and what it does to this device's user's balance; a bill whose group isn't read yet, or
 * which they aren't in after all, is left out, but in everyBillGroupId (opened from its statistics), where a bill of
 * others does nothing to their balance.
 */
internal fun myExpensesOf(
    expenses: List<Expense>,
    groups: List<Group>,
    everyBillGroupId: String?,
): List<LatestExpense> {
    val groupsById = groups.associateBy { it.id }
    return expenses.mapNotNull { expense ->
        val group = groupsById[expense.groupId] ?: return@mapNotNull null
        val isWholeGroup = expense.groupId == everyBillGroupId
        // Why: in the whole group even before its members say which is the user's (still syncing), as a bill of others.
        val meId = group.members.firstOrNull { it.isMe }?.id
        LatestExpense(
            group = group,
            expense = expense,
            myBalanceMinor = meId?.let(expense::balanceOf)
                ?: 0L.takeIf { isWholeGroup }
                ?: return@mapNotNull null,
        )
    }
}

/*
 * Why: "All expenses" narrowed by its filters, in the order picked. The text finds a bill by its description as the
 * home's search does (TypoTolerantQuery): what holds it as typed, and only when nothing does, what comes a typo or two
 * away (Pierre's call: the same search, over the bills only).
 */
internal fun narrowedOf(
    bills: List<LatestExpense>,
    filters: AllExpensesFilters,
    categories: CategoryTree,
    // Why: the days of the period picked, in the device's zone; null for the whole history.
    period: DatedSpan?,
): List<LatestExpense> {
    // Why: each group's members' Who paid keys worked out once, not again for every payer of every bill.
    val payerKeysByGroup = if (filters.payerKeys.isEmpty()) {
        emptyMap()
    } else {
        bills
            .map { it.group }
            .distinctBy { it.id }
            .associate { group -> group.id to group.members.associate { it.id to payerKeyOf(it) } }
    }
    val filtered = bills.filter {
        it.isPassing(filters, categories, payerKeysByGroup[it.group.id].orEmpty()) &&
            (period == null || it.expense.spentAt in period)
    }
    val typed = TypoTolerantQuery.of(filters.query)
    val found = if (typed.isEmpty) {
        filtered
    } else {
        val exact = typed.toExactOnly()
        filtered
            .filter { exact.findIn(it.expense.description) != null }
            .ifEmpty { filtered.filter { typed.findIn(it.expense.description) != null } }
    }
    return found.sortedWith(comparatorOf(filters.sort))
}

// Why: a period's days with the zone they are counted in, so a bill falls in it by the day it was spent there.
internal class DatedSpan(
    private val span: DateSpan,
    private val timeZone: TimeZone,
) {
    operator fun contains(epochMillis: Long): Boolean = SpentDay.dateOf(epochMillis, timeZone) in span
}

// Why: the period's days as the statistics count them (findSpanOrNull); null for the whole history.
internal fun AllExpensesFilters.periodSpanOrNull(
    nowMillis: Long,
    timeZone: TimeZone,
): DatedSpan? {
    val start = customStartMillis
    val end = customEndMillis
    val customSpan = if (start != null && end != null) {
        DateSpan(SpentDay.dateOf(start, timeZone), SpentDay.dateOf(end, timeZone))
    } else {
        null
    }
    return period
        .findSpanOrNull(
            SpentDay.dateOf(nowMillis, timeZone),
            wholeSpan = null,
            customSpan = customSpan,
        )?.let { DatedSpan(it, timeZone) }
}

private fun LatestExpense.isPassing(
    filters: AllExpensesFilters,
    categories: CategoryTree,
    // Why: the Who paid key of each member of the bill's group, by member id.
    payerKeys: Map<String, String>,
): Boolean = (filters.groupIds.isEmpty() || group.id in filters.groupIds) &&
    (
        filters.categoryIds.isEmpty() ||
            categories.mainOf(categories.resolve(expense.categoryId)).id in filters.categoryIds
    ) &&
    when (filters.share) {
        ShareFilter.ALL -> true
        ShareFilter.TO_RECEIVE -> myBalanceMinor > 0
        ShareFilter.TO_PAY -> myBalanceMinor < 0
    } &&
    (filters.payerKeys.isEmpty() || isPaidByAny(filters.payerKeys, payerKeys))

// Why: any of the people picked paid part of the bill; someone no longer in the group is no one's pick.
private fun LatestExpense.isPaidByAny(
    picked: Set<String>,
    payerKeys: Map<String, String>,
): Boolean = expense.payers.any { payer -> payer.amountMinor > 0 && payerKeys[payer.memberId] in picked }

// Why: the user is PAYER_KEY_ME in every group; anyone else the same person in every group, as the global Balances.
private fun payerKeyOf(member: GroupMember): String = if (member.isMe) PAYER_KEY_ME else PeopleBalances.keyOf(member)

// Why: everyone Who paid may hold, of every group: a pick outside it is someone gone from every group.
internal fun payerKeysOf(groups: List<Group>): Set<String> =
    groups.flatMapTo(HashSet()) { group -> group.members.map(::payerKeyOf) }

/*
 * Why: Who paid's people: the user first, then everyone of the groups picked (none picked: of every group) by name, once
 * each, named and drawn as the first group in the device's order has them; anyone picked stays offered, so a group let
 * go of never hides a pick that still narrows the list.
 */
internal fun payerOptionsOf(
    groups: List<Group>,
    filters: AllExpensesFilters,
): List<FilterPayerOption> {
    val byKey = LinkedHashMap<String, FilterPayerOption>()
    groups
        .sortedByDescending { filters.groupIds.isEmpty() || it.id in filters.groupIds }
        .forEach { group ->
            val isOffered = filters.groupIds.isEmpty() || group.id in filters.groupIds
            group.members.forEach { member ->
                val key = payerKeyOf(member)
                if ((isOffered || key in filters.payerKeys) && key !in byKey) {
                    byKey[key] = FilterPayerOption(
                        key = key,
                        name = member.name,
                        isMe = member.isMe,
                        face = faceOf(member),
                    )
                }
            }
        }
    // Why: "Álvaro" with the A's, as Contatos sorts names.
    return byKey.values.sortedWith(
        compareByDescending<FilterPayerOption> { it.isMe }.thenBy { TextFolding.toFolded(it.name) },
    )
}

// Why: by date as each group's Expenses orders them; by amount, the most recent first among equal totals.
private fun comparatorOf(sort: ExpenseSort): Comparator<LatestExpense> {
    val newest = compareByDescending<LatestExpense> { it.expense.spentAt }.thenBy { it.expense.id }
    return when (sort) {
        ExpenseSort.NEWEST -> newest
        ExpenseSort.OLDEST -> compareBy<LatestExpense> { it.expense.spentAt }.thenBy { it.expense.id }
        ExpenseSort.LARGEST -> compareByDescending<LatestExpense> { it.expense.amountMinor }.then(newest)
        ExpenseSort.SMALLEST -> compareBy<LatestExpense> { it.expense.amountMinor }.then(newest)
    }
}

// Why: the Group filter's options, each with its square as Home's rows draw it, in the device's order.
internal fun groupOptionsOf(groups: List<Group>): List<FilterGroupOption> = groups.map { group ->
    FilterGroupOption(
        id = group.id,
        name = group.name,
        initials = Initials.of(group.name),
        color = group.color,
        photoUrl = group.photo?.toImageUrl(),
    )
}
