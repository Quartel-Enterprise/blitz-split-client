package com.quare.blitzsplit.core.groups.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/*
 * Why: the group's reminder as a repeating calendar event, offered when the group is created and when someone joins
 * it ("Add to calendar"). Each platform hands it to its own calendar: Google Calendar or the system's on Android,
 * an .ics (or EventKit) on Apple, an .ics download on the web. The time is floating, local to whoever opens it, as
 * the push is read on each member's own clock.
 */
data class ReminderCalendarEvent(
    val groupName: String,
    // Why: the first occurrence from now, in epoch millis, which the calendar starts the series on.
    val startMillis: Long,
    val recurrence: ReminderRecurrence,
    // Why: the zone the start is read in to write its date and time: the device's.
    val timeZoneId: String,
) {
    // Why: the event's start as the calendar's local "yyyyMMddTHHmmss", with no zone so it follows the person's clock.
    val startLocal: String
        get() {
            val local = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(TimeZone.of(timeZoneId))
            return "${local.year.pad(4)}${local.month.number.pad(2)}${local.day.pad(2)}" +
                "T${local.hour.pad(2)}${local.minute.pad(2)}00"
        }

    // Why: RFC 5545's RRULE, which Google Calendar's insert intent and the .ics both take.
    val rrule: String
        get() = recurrence.rrule

    // Why: the whole .ics file, its words handed in by the UI in the person's language.
    fun toIcs(
        title: String,
        description: String,
        uid: String,
    ): String = listOf(
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//Blitz Split//Reminder//EN",
        "CALSCALE:GREGORIAN",
        "BEGIN:VEVENT",
        "UID:$uid",
        "DTSTAMP:${utcStamp()}",
        "DTSTART:$startLocal",
        "DURATION:PT15M",
        "RRULE:$rrule",
        "SUMMARY:${escape(title)}",
        "DESCRIPTION:${escape(description)}",
        "END:VEVENT",
        "END:VCALENDAR",
    ).joinToString(separator = "\r\n", postfix = "\r\n")

    // Why: DTSTAMP is in UTC by the RFC, here the start's instant as "yyyyMMddTHHmmssZ".
    private fun utcStamp(): String {
        val utc = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(TimeZone.UTC)
        return "${utc.year.pad(4)}${utc.month.number.pad(2)}${utc.day.pad(2)}T${utc.hour.pad(2)}${utc.minute.pad(2)}00Z"
    }

    private fun escape(text: String): String = text
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")

    private fun Int.pad(length: Int): String = toString().padStart(length, '0')

    companion object {
        /*
         * Why: null while the reminder is off. A monthly day 31 repeats on each month's last day, as the server fires
         * it; a 29th or 30th is on that day only, so a calendar skips the months that lack it (February).
         */
        fun of(
            groupName: String,
            reminder: GroupReminder,
            nowMillis: Long,
            timeZone: TimeZone,
        ): ReminderCalendarEvent? {
            val start = reminder.nextOccurrenceAfter(nowMillis, timeZone) ?: return null
            return ReminderCalendarEvent(
                groupName = groupName,
                startMillis = start,
                recurrence = ReminderRecurrence.of(reminder),
                timeZoneId = timeZone.id,
            )
        }
    }
}

// Why: the repeat rule of a schedule, apart from its time.
data class ReminderRecurrence(
    val rrule: String,
) {
    companion object {
        private const val LAST_DAY = 31

        fun of(reminder: GroupReminder): ReminderRecurrence = ReminderRecurrence(
            when (reminder.kind) {
                ReminderKind.WEEKLY -> "FREQ=WEEKLY;BYDAY=${weekdayCode(reminder.day)}"

                ReminderKind.MONTHLY -> if (reminder.day == LAST_DAY) {
                    "FREQ=MONTHLY;BYMONTHDAY=-1"
                } else {
                    "FREQ=MONTHLY;BYMONTHDAY=${reminder.day}"
                }
            },
        )

        private fun weekdayCode(isoDay: Int): String = when (DayOfWeek(isoDay)) {
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
