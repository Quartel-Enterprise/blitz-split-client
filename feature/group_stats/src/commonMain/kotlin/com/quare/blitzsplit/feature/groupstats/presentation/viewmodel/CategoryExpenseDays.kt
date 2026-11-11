package com.quare.blitzsplit.feature.groupstats.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.PayerOrder
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpenseDay
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpenseRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPayer
import kotlinx.datetime.TimeZone

// Why: the lines of "Expenses by category", by day as the group's Expenses lists its bills.
internal object CategoryExpenseDays {
    fun of(
        expenses: List<Expense>,
        group: Group,
        categories: CategoryTree,
        now: Long,
        timeZone: TimeZone,
    ): List<CategoryExpenseDay> {
        val membersById = group.members.associateBy { it.id }
        val meId = group.members.firstOrNull { it.isMe }?.id
        val payerOrder = PayerOrder(group.members)
        return expenses
            .sortedByDescending { it.spentAt }
            .groupBy { SpentDay.dateOf(it.spentAt, timeZone) }
            .map { (date, bills) ->
                CategoryExpenseDay(
                    dayStartMillis = SpentDay.startOf(date, timeZone),
                    relativeDay = SpentDay.relativeDayOf(date, now, timeZone),
                    expenses = bills.map { expense ->
                        CategoryExpenseRow(
                            id = expense.id,
                            description = expense.description,
                            spentAt = expense.spentAt,
                            hasSpentTime = SpentDay.hasTime(expense.spentAt, timeZone),
                            payers = payerOrder.sort(expense.payers.map { it.memberId }).map { id ->
                                StatsPayer(
                                    name = membersById[id]?.name,
                                    isMe = id == meId,
                                )
                            },
                            totalMinor = expense.amountMinor,
                            myBalanceMinor = meId?.let(expense::balanceOf),
                            category = categories.resolve(expense.categoryId),
                            isPending = expense.isPending,
                        )
                    },
                )
            }
    }
}
