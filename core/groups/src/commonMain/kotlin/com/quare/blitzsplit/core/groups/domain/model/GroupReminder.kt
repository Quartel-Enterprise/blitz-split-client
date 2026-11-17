package com.quare.blitzsplit.core.groups.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/*
 * Why: the group's "Lembrete de acerto", the four columns of groups on the server (reminder_enabled, reminder_kind,
 * reminder_day, reminder_minute). [day] is the ISO weekday (1 Monday to 7 Sunday) while weekly, the day of the month
 * (1 to 31) while monthly; [minute] is the minutes after midnight, 600 being 10:00, read on each member's own clock.
 * A month without the day fires on its last day, as the server does.
 */
data class GroupReminder(
    val isEnabled: Boolean,
    val kind: ReminderKind,
    val day: Int,
    val minute: Int,
) {
    val hour: Int
        get() = minute / MINUTES_PER_HOUR

    val minuteOfHour: Int
        get() = minute % MINUTES_PER_HOUR

    // Why: the same schedule with the kind switched, whose day goes back to the first one the kind allows.
    fun withKind(kind: ReminderKind): GroupReminder = if (kind == this.kind) {
        this
    } else {
        copy(
            kind = kind,
            day = DEFAULT_DAY,
        )
    }

    // Why: a day the kind doesn't have (the 25th while weekly) stays in the kind's range.
    fun withDay(day: Int): GroupReminder = copy(day = day.coerceIn(DEFAULT_DAY, maxDayOf(kind)))

    fun withMinute(minute: Int): GroupReminder = copy(minute = minute.coerceIn(0, MINUTES_PER_DAY - 1))

    /*
     * Why: the latest time this schedule fired at or before [nowMillis] on a clock in [timeZone], in epoch millis; the
     * banner's "it arrived or passed". Null while the reminder is off.
     */
    fun findLastOccurrenceAtOrBefore(
        nowMillis: Long,
        timeZone: TimeZone,
    ): Long? {
        if (!isEnabled) return null
        val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
        return when (kind) {
            ReminderKind.WEEKLY -> {
                val sinceDay = (today.dayOfWeek.isoDayNumber - day + DAYS_PER_WEEK) % DAYS_PER_WEEK
                val thisWeek = toOccurrenceOn(today.minus(DatePeriod(days = sinceDay)), timeZone)
                if (thisWeek <= nowMillis) {
                    thisWeek
                } else {
                    toOccurrenceOn(
                        today.minus(DatePeriod(days = sinceDay + DAYS_PER_WEEK)),
                        timeZone,
                    )
                }
            }

            ReminderKind.MONTHLY -> {
                val thisMonth = toOccurrenceOn(findClampedInMonth(today.year, today.month.number), timeZone)
                if (thisMonth <= nowMillis) {
                    thisMonth
                } else {
                    val previous = LocalDate(today.year, today.month.number, 1).minus(DatePeriod(months = 1))
                    toOccurrenceOn(findClampedInMonth(previous.year, previous.month.number), timeZone)
                }
            }
        }
    }

    // Why: the first time after [nowMillis] this schedule fires, for the calendar's first event; null while off.
    fun findNextOccurrenceAfter(
        nowMillis: Long,
        timeZone: TimeZone,
    ): Long? {
        val last = findLastOccurrenceAtOrBefore(nowMillis, timeZone) ?: return null
        val lastDate = Instant.fromEpochMilliseconds(last).toLocalDateTime(timeZone).date
        val nextDate = when (kind) {
            ReminderKind.WEEKLY -> lastDate.plus(DatePeriod(days = DAYS_PER_WEEK))

            ReminderKind.MONTHLY -> {
                val next = LocalDate(lastDate.year, lastDate.month.number, 1).plus(DatePeriod(months = 1))
                findClampedInMonth(next.year, next.month.number)
            }
        }
        return toOccurrenceOn(nextDate, timeZone)
    }

    private fun toOccurrenceOn(
        date: LocalDate,
        timeZone: TimeZone,
    ): Long = date
        .atTime(LocalTime(hour, minuteOfHour))
        .toInstant(timeZone)
        .toEpochMilliseconds()

    private fun findClampedInMonth(
        year: Int,
        month: Int,
    ): LocalDate {
        val first = LocalDate(year, month, 1)
        val lastDay = first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)).day
        return LocalDate(year, month, day.coerceAtMost(lastDay))
    }

    companion object {
        const val MINUTES_PER_HOUR = 60
        const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
        const val DEFAULT_DAY = 1
        const val MAX_WEEKDAY = 7
        const val MAX_MONTH_DAY = 31
        private const val DAYS_PER_WEEK = 7
        private const val DEFAULT_MINUTE = 10 * MINUTES_PER_HOUR

        // Why: every group keeps the Monday 10:00 weekly reminder until its members change it.
        val Default = GroupReminder(
            isEnabled = true,
            kind = ReminderKind.WEEKLY,
            day = DEFAULT_DAY,
            minute = DEFAULT_MINUTE,
        )

        /*
         * Why: a row read from the server or from Room, kept in the ranges the server's checks allow, so a value a
         * newer app wrote never throws; null fields (a server without the columns) read as the default.
         */
        fun of(
            isEnabled: Boolean?,
            kindCode: String?,
            day: Int?,
            minute: Int?,
        ): GroupReminder {
            val kind = ReminderKind.fromCodeOrNull(kindCode) ?: Default.kind
            return GroupReminder(
                isEnabled = isEnabled ?: Default.isEnabled,
                kind = kind,
                day = (day ?: DEFAULT_DAY).coerceIn(DEFAULT_DAY, maxDayOf(kind)),
                minute = (minute ?: DEFAULT_MINUTE).coerceIn(0, MINUTES_PER_DAY - 1),
            )
        }

        fun maxDayOf(kind: ReminderKind): Int = when (kind) {
            ReminderKind.WEEKLY -> MAX_WEEKDAY
            ReminderKind.MONTHLY -> MAX_MONTH_DAY
        }
    }
}
