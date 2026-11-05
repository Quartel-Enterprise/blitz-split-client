package com.quare.blitzsplit.feature.groupstats.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.DateSpan
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.PayerOrder
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpenseDay
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpenseRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPayer
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsRangeStyle
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus

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

    /*
     * Why: the period as the statistics wrote it: a month from its first day to its last (or to today, "This month") is
     * "October 2026", up to 3 months its days, longer its months.
     */
    fun rangeStyleOf(
        span: DateSpan,
        today: LocalDate,
    ): StatsRangeStyle {
        val monthStart = LocalDate(span.start.year, span.start.month, 1)
        val monthEnd = monthStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        val isMonth = span.start == monthStart && (span.end == monthEnd || (span.end == today && today < monthEnd))
        return when {
            isMonth -> StatsRangeStyle.MONTH
            span.canSplitBy(StatsGrain.DAY) -> StatsRangeStyle.DAYS
            else -> StatsRangeStyle.MONTHS
        }
    }
}
