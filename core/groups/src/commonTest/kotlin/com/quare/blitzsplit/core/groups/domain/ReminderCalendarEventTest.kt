package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.groups.domain.model.ReminderCalendarEvent
import com.quare.blitzsplit.core.groups.domain.model.ReminderKind
import com.quare.blitzsplit.core.groups.domain.model.ReminderRecurrence
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderCalendarEventTest {
    private val weeklyMonday = GroupReminder.Default
    private val monthly15 = GroupReminder(
        isEnabled = true,
        kind = ReminderKind.MONTHLY,
        day = 15,
        minute = 600,
    )
    private val wednesday = millisAt(2026, 11, 18, 12)
    private val tokyo = TimeZone.of("Asia/Tokyo")

    @Test
    fun `GIVEN a reminder turned off WHEN building the calendar event THEN there is none`() {
        // Given
        val reminder = weeklyMonday.copy(isEnabled = false)

        // When
        val event = ReminderCalendarEvent.of("Viagem", reminder, wednesday, TimeZone.UTC)

        // Then
        assertNull(event)
    }

    @Test
    fun `GIVEN a weekly Monday reminder WHEN building the event THEN starts on the next Monday and repeats weekly`() {
        // Given
        val groupName = "Viagem Floripa"

        // When
        val event = ReminderCalendarEvent.of(groupName, weeklyMonday, wednesday, TimeZone.UTC)

        // Then
        assertNotNull(event)
        assertEquals(groupName, event.groupName)
        assertEquals(millisAt(2026, 11, 23, 10), event.startMillis)
        assertEquals("FREQ=WEEKLY;BYDAY=MO", event.rrule)
        assertEquals("UTC", event.timeZoneId)
    }

    @Test
    fun `GIVEN each weekday WHEN building the repeat rule THEN names it from Monday MO to Sunday SU`() {
        // Given
        val days = (1..7)

        // When
        val rules = days.map { ReminderRecurrence.of(weeklyMonday.copy(day = it)).rrule }

        // Then
        assertEquals(
            listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU").map { "FREQ=WEEKLY;BYDAY=$it" },
            rules,
        )
    }

    @Test
    fun `GIVEN a monthly reminder on the 15th WHEN building the repeat rule THEN repeats on that day of the month`() {
        // Given
        val reminder = monthly15

        // When
        val recurrence = ReminderRecurrence.of(reminder)

        // Then
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=15", recurrence.rrule)
    }

    @Test
    fun `GIVEN a monthly 31st reminder WHEN building the repeat rule THEN repeats on each month last day`() {
        // Given
        val reminder = monthly15.copy(day = 31)

        // When
        val recurrence = ReminderRecurrence.of(reminder)

        // Then
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=-1", recurrence.rrule)
    }

    @Test
    fun `GIVEN a monthly reminder on the 30th WHEN building the repeat rule THEN repeats on the 30th only`() {
        // Given
        val reminder = monthly15.copy(day = 30)

        // When
        val recurrence = ReminderRecurrence.of(reminder)

        // Then
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=30", recurrence.rrule)
    }

    @Test
    fun `GIVEN a monthly 30th reminder and February next WHEN building the event THEN starts on March 30th`() {
        // Given
        val reminder = monthly15.copy(day = 30)
        val afterJanuary30th = millisAt(2027, 1, 31, 12)

        // When
        val event = ReminderCalendarEvent.of("Viagem", reminder, afterJanuary30th, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2027, 3, 30, 10), event?.startMillis)
    }

    @Test
    fun `GIVEN a monthly 31st reminder and a short month next WHEN building the event THEN starts on its last day`() {
        // Given
        val reminder = monthly15.copy(day = 31)
        val afterJanuary31st = millisAt(2027, 1, 31, 12)

        // When
        val event = ReminderCalendarEvent.of("Viagem", reminder, afterJanuary31st, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2027, 2, 28, 10), event?.startMillis)
    }

    @Test
    fun `GIVEN a start in Tokyo WHEN writing its local time THEN has the date and time of that clock with no zone`() {
        // Given
        val event = ReminderCalendarEvent.of("Viagem", weeklyMonday, wednesday, tokyo)

        // When
        val local = event?.startLocal

        // Then
        assertEquals("20261123T100000", local)
        assertEquals(millisAt(2026, 11, 23, 10, 0, tokyo), event?.startMillis)
    }

    @Test
    fun `GIVEN a reminder at 10 05 WHEN writing the local time THEN pads the minutes and seconds`() {
        // Given
        val reminder = weeklyMonday.copy(minute = 605)
        val event = ReminderCalendarEvent.of("Viagem", reminder, wednesday, TimeZone.UTC)

        // When
        val local = event?.startLocal

        // Then
        assertEquals("20261123T100500", local)
    }

    @Test
    fun `GIVEN a reminder at midnight on a single digit date WHEN writing the local time THEN pads every part`() {
        // Given
        val reminder = monthly15.copy(
            day = 5,
            minute = 0,
        )
        val event = ReminderCalendarEvent.of("Viagem", reminder, millisAt(2026, 11, 20, 12), TimeZone.UTC)

        // When
        val local = event?.startLocal

        // Then
        assertEquals("20261205T000000", local)
    }

    @Test
    fun `GIVEN an event WHEN writing the ics THEN it is a calendar with one event and CRLF lines`() {
        // Given
        val event = ReminderCalendarEvent.of("Viagem Floripa", weeklyMonday, wednesday, TimeZone.UTC)

        // When
        val ics = event?.toIcs(
            title = "Settle up",
            description = "Time to settle",
            uid = "uid-1",
        )

        // Then
        assertNotNull(ics)
        assertTrue(ics.endsWith("\r\n"))
        assertEquals(
            listOf(
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:-//Blitz Split//Reminder//EN",
                "CALSCALE:GREGORIAN",
                "BEGIN:VEVENT",
                "UID:uid-1",
                "DTSTAMP:20261123T100000Z",
                "DTSTART:20261123T100000",
                "DURATION:PT15M",
                "RRULE:FREQ=WEEKLY;BYDAY=MO",
                "SUMMARY:Settle up",
                "DESCRIPTION:Time to settle",
                "END:VEVENT",
                "END:VCALENDAR",
                "",
            ),
            ics.split("\r\n"),
        )
    }

    @Test
    fun `GIVEN a start in Tokyo WHEN writing the ics THEN DTSTART has no zone and DTSTAMP is in UTC`() {
        // Given
        val event = ReminderCalendarEvent.of("Viagem", weeklyMonday, wednesday, tokyo)

        // When
        val lines = event
            ?.toIcs(
                title = "t",
                description = "d",
                uid = "u",
            )?.split("\r\n")
            .orEmpty()

        // Then
        assertTrue("DTSTART:20261123T100000" in lines)
        assertTrue("DTSTAMP:20261123T010000Z" in lines)
    }

    @Test
    fun `GIVEN a title with commas and line breaks WHEN writing the ics THEN escapes them`() {
        // Given
        val event = ReminderCalendarEvent.of("Viagem", weeklyMonday, wednesday, TimeZone.UTC)
        val title = "Pay, now; or \\ else"
        val description = "Line one\nLine two\r\nLine three"

        // When
        val lines = event
            ?.toIcs(
                title = title,
                description = description,
                uid = "u",
            )?.split("\r\n")
            .orEmpty()

        // Then
        assertTrue("SUMMARY:Pay\\, now\\; or \\\\ else" in lines)
        assertTrue("DESCRIPTION:Line one\\nLine two\\nLine three" in lines)
    }
}
