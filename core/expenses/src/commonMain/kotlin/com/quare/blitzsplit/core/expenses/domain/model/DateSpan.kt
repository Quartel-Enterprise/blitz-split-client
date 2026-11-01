package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.StatsGrain
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/*
 * Why: a period of a group's statistics, from its first day to its last, both counted, in the device's time zone.
 * "Ao longo do tempo" splits it by day, week (Monday to Sunday), month or year.
 */
data class DateSpan(
    val start: LocalDate,
    val end: LocalDate,
) {
    operator fun contains(date: LocalDate): Boolean = date in start..end

    /*
     * Why: "Dia vale para períodos de até 3 meses; Semana, Mês e Ano aparecem quando o período tem pelo menos 2
     * deles."
     */
    fun canSplitBy(grain: StatsGrain): Boolean = when (grain) {
        StatsGrain.DAY -> end <= start.plus(DatePeriod(months = MAX_MONTHS_BY_DAY))
        StatsGrain.WEEK -> weekStartOf(end) > weekStartOf(start)
        StatsGrain.MONTH -> end.year > start.year || end.month > start.month
        StatsGrain.YEAR -> end.year > start.year
    }

    // Why: each day, week, month or year the period touches, whole even where the period starts or ends inside it.
    fun splitBy(grain: StatsGrain): List<DateSpan> {
        val spans = mutableListOf<DateSpan>()
        var from = startOf(start, grain)
        while (from <= end) {
            val next = from.plus(stepOf(grain))
            spans += DateSpan(from, next.minus(DatePeriod(days = 1)))
            from = next
        }
        return spans
    }

    companion object {
        private const val MAX_MONTHS_BY_DAY = 3

        fun weekStartOf(date: LocalDate): LocalDate =
            date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber))

        private fun startOf(
            date: LocalDate,
            grain: StatsGrain,
        ): LocalDate = when (grain) {
            StatsGrain.DAY -> date
            StatsGrain.WEEK -> weekStartOf(date)
            StatsGrain.MONTH -> LocalDate(date.year, date.month, 1)
            StatsGrain.YEAR -> LocalDate(date.year, 1, 1)
        }

        private fun stepOf(grain: StatsGrain): DatePeriod = when (grain) {
            StatsGrain.DAY -> DatePeriod(days = 1)
            StatsGrain.WEEK -> DatePeriod(days = 7)
            StatsGrain.MONTH -> DatePeriod(months = 1)
            StatsGrain.YEAR -> DatePeriod(years = 1)
        }
    }
}
