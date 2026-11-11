package com.quare.blitzsplit.core.expenses.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Why: the day of a bill in the device's time zone, which the group's list groups by and "Quando" names.
object SpentDay {
    fun dateOf(
        epochMillis: Long,
        timeZone: TimeZone,
    ): LocalDate = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).date

    // Why: the first instant of the day, which every UI formats as that day's date.
    fun startOf(
        date: LocalDate,
        timeZone: TimeZone,
    ): Long = date.atStartOfDayIn(timeZone).toEpochMilliseconds()

    /*
     * Why: a bill's time is optional (beta feedback): one without it is saved at the first instant of its day, which
     * every screen shows as the date alone. A time picked as 00:00 reads the same, as no time.
     */
    fun hasTime(
        epochMillis: Long,
        timeZone: TimeZone,
    ): Boolean = epochMillis != startOf(dateOf(epochMillis, timeZone), timeZone)

    fun relativeDayOf(
        date: LocalDate,
        nowMillis: Long,
        timeZone: TimeZone,
    ): RelativeDay {
        val today = dateOf(nowMillis, timeZone)
        return when (date) {
            today -> RelativeDay.TODAY
            today.minus(DatePeriod(days = 1)) -> RelativeDay.YESTERDAY
            else -> RelativeDay.OTHER
        }
    }

    // Why: "Hoje" or "Ontem" for the moment a bill was spent, as a bill's own screen and its form name it.
    fun relativeDayOf(
        epochMillis: Long,
        nowMillis: Long,
        timeZone: TimeZone,
    ): RelativeDay = relativeDayOf(
        date = dateOf(epochMillis, timeZone),
        nowMillis = nowMillis,
        timeZone = timeZone,
    )
}

// Why: injected so tests pick the time zone the days are counted in.
fun interface CurrentTimeZone {
    operator fun invoke(): TimeZone
}
