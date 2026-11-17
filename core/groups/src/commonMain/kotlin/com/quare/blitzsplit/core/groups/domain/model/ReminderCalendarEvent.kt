package com.quare.blitzsplit.core.groups.domain.model

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
            return "${local.year.toPadded(4)}${local.month.number.toPadded(2)}${local.day.toPadded(2)}" +
                "T${local.hour.toPadded(2)}${local.minute.toPadded(2)}00"
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
        "DTSTAMP:$utcStamp",
        "DTSTART:$startLocal",
        "DURATION:PT15M",
        "RRULE:$rrule",
        "SUMMARY:${toEscaped(title)}",
        "DESCRIPTION:${toEscaped(description)}",
        "END:VEVENT",
        "END:VCALENDAR",
    ).joinToString(
        separator = "\r\n",
        postfix = "\r\n",
    )

    // Why: DTSTAMP is in UTC by the RFC, here the start's instant as "yyyyMMddTHHmmssZ".
    private val utcStamp: String
        get() {
            val utc = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(TimeZone.UTC)
            return "${utc.year.toPadded(4)}${utc.month.number.toPadded(2)}${utc.day.toPadded(2)}" +
                "T${utc.hour.toPadded(2)}${utc.minute.toPadded(2)}00Z"
        }

    private fun toEscaped(text: String): String = text
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")

    private fun Int.toPadded(length: Int): String = toString().padStart(length, '0')

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
            val start = reminder.findNextOccurrenceAfter(nowMillis, timeZone) ?: return null
            return ReminderCalendarEvent(
                groupName = groupName,
                startMillis = start,
                recurrence = ReminderRecurrence.of(reminder),
                timeZoneId = timeZone.id,
            )
        }
    }
}
