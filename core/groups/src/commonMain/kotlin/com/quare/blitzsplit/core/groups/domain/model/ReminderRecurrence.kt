package com.quare.blitzsplit.core.groups.domain.model

import kotlinx.datetime.DayOfWeek

// Why: the repeat rule of a schedule, apart from its time.
data class ReminderRecurrence(
    val rrule: String,
) {
    companion object {
        private const val LAST_DAY = 31

        fun of(reminder: GroupReminder): ReminderRecurrence = ReminderRecurrence(
            when (reminder.kind) {
                ReminderKind.WEEKLY -> "FREQ=WEEKLY;BYDAY=${toWeekdayCode(reminder.day)}"

                ReminderKind.MONTHLY -> if (reminder.day == LAST_DAY) {
                    "FREQ=MONTHLY;BYMONTHDAY=-1"
                } else {
                    "FREQ=MONTHLY;BYMONTHDAY=${reminder.day}"
                }
            },
        )

        private fun toWeekdayCode(isoDay: Int): String = when (DayOfWeek(isoDay)) {
            DayOfWeek.MONDAY -> "MO"
            DayOfWeek.TUESDAY -> "TU"
            DayOfWeek.WEDNESDAY -> "WE"
            DayOfWeek.THURSDAY -> "TH"
            DayOfWeek.FRIDAY -> "FR"
            DayOfWeek.SATURDAY -> "SA"
            DayOfWeek.SUNDAY -> "SU"
        }
    }
}
