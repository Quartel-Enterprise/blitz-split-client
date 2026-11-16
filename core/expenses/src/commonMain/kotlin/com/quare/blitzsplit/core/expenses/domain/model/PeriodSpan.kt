package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.ExpensePeriod
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/*
 * Why: the days of a period, one rule for Statistics and for All expenses, so a category opened from Statistics lists
 * the bills of the days it counted: this year and this month up to today, last month whole. The whole history and
 * the days picked are the caller's (Statistics' first to last bill, All expenses' none), null when it has none.
 */
fun ExpensePeriod.findSpanOrNull(
    today: LocalDate,
    wholeSpan: DateSpan?,
    customSpan: DateSpan?,
): DateSpan? {
    val monthStart = LocalDate(today.year, today.month, 1)
    return when (this) {
        ExpensePeriod.ALL -> wholeSpan

        ExpensePeriod.THIS_YEAR -> DateSpan(LocalDate(today.year, 1, 1), today)

        ExpensePeriod.THIS_MONTH -> DateSpan(monthStart, today)

        ExpensePeriod.LAST_MONTH -> DateSpan(
            start = monthStart.minus(DatePeriod(months = 1)),
            end = monthStart.minus(DatePeriod(days = 1)),
        )

        ExpensePeriod.CUSTOM -> customSpan
    }
}
