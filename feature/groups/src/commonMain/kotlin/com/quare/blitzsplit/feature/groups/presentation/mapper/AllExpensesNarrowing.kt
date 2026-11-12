package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.utils.TypoTolerantQuery
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesFilters
import com.quare.blitzsplit.feature.groups.presentation.model.ExpenseSort
import com.quare.blitzsplit.feature.groups.presentation.model.FilterGroupOption
import com.quare.blitzsplit.feature.groups.presentation.model.PayerFilter
import com.quare.blitzsplit.feature.groups.presentation.model.ShareFilter

/*
 * Why: each bill with its group and what it does to this device's user's balance; a bill whose group isn't read yet, or
 * which they aren't in after all, is left out.
 */
internal fun myExpensesOf(
    expenses: List<Expense>,
    groups: List<Group>,
): List<LatestExpense> {
    val groupsById = groups.associateBy { it.id }
    return expenses.mapNotNull { expense ->
        val group = groupsById[expense.groupId] ?: return@mapNotNull null
        val meId = group.members.firstOrNull { it.isMe }?.id ?: return@mapNotNull null
        LatestExpense(
            group = group,
            expense = expense,
            myBalanceMinor = expense.balanceOf(meId) ?: return@mapNotNull null,
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
): List<LatestExpense> {
    val filtered = bills.filter { it.isPassing(filters, categories) }
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

private fun LatestExpense.isPassing(
    filters: AllExpensesFilters,
    categories: CategoryTree,
): Boolean {
    val meId = group.members.firstOrNull { it.isMe }?.id
    return (filters.groupIds.isEmpty() || group.id in filters.groupIds) &&
        (
            filters.categoryIds.isEmpty() ||
                categories.mainOf(categories.resolve(expense.categoryId)).id in filters.categoryIds
        ) &&
        when (filters.share) {
            ShareFilter.ALL -> true
            ShareFilter.TO_RECEIVE -> myBalanceMinor > 0
            ShareFilter.TO_PAY -> myBalanceMinor < 0
        } &&
        when (filters.payer) {
            PayerFilter.ALL -> true
            PayerFilter.YOU -> meId != null && expense.paidAmountOf(meId) > 0
            PayerFilter.OTHERS -> meId == null || expense.paidAmountOf(meId) == 0L
        }
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
