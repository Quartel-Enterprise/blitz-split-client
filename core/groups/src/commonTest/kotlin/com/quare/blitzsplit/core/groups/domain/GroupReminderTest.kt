package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.groups.domain.model.ReminderKind
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupReminderTest {
    private val weeklyMonday = GroupReminder.Default
    private val monthly15 = GroupReminder(
        isEnabled = true,
        kind = ReminderKind.MONTHLY,
        day = 15,
        minute = 600,
    )
    private val monthly30 = monthly15.copy(day = 30)
    private val monthly31 = monthly15.copy(day = 31)
    private val saoPaulo = TimeZone.of("America/Sao_Paulo")
    private val tokyo = TimeZone.of("Asia/Tokyo")

    @Test
    fun `GIVEN a new group WHEN reading the default reminder THEN it is on weekly on Monday at 10 00`() {
        // Given
        val reminder = GroupReminder.Default

        // When
        val hour = reminder.hour
        val minuteOfHour = reminder.minuteOfHour

        // Then
        assertTrue(reminder.isEnabled)
        assertEquals(ReminderKind.WEEKLY, reminder.kind)
        assertEquals(1, reminder.day)
        assertEquals(600, reminder.minute)
        assertEquals(10, hour)
        assertEquals(0, minuteOfHour)
    }

    @Test
    fun `GIVEN 605 minutes after midnight WHEN reading the time THEN it is 10 05`() {
        // Given
        val reminder = weeklyMonday.copy(minute = 605)

        // When
        val hour = reminder.hour
        val minuteOfHour = reminder.minuteOfHour

        // Then
        assertEquals(10, hour)
        assertEquals(5, minuteOfHour)
    }

    @Test
    fun `GIVEN a server row without the columns WHEN reading it THEN reads the default schedule`() {
        // Given
        val noFields: Boolean? = null

        // When
        val reminder = GroupReminder.of(
            isEnabled = noFields,
            kindCode = null,
            day = null,
            minute = null,
        )

        // Then
        assertEquals(GroupReminder.Default, reminder)
    }

    @Test
    fun `GIVEN a stored monthly row WHEN reading it THEN keeps every field`() {
        // Given
        val code = "monthly"

        // When
        val reminder = GroupReminder.of(
            isEnabled = false,
            kindCode = code,
            day = 31,
            minute = 1_439,
        )

        // Then
        assertEquals(
            GroupReminder(
                isEnabled = false,
                kind = ReminderKind.MONTHLY,
                day = 31,
                minute = 1_439,
            ),
            reminder,
        )
    }

    @Test
    fun `GIVEN a kind this version does not know WHEN reading the row THEN reads it as weekly`() {
        // Given
        val code = "yearly"

        // When
        val reminder = GroupReminder.of(
            isEnabled = true,
            kindCode = code,
            day = 3,
            minute = 720,
        )

        // Then
        assertEquals(
            GroupReminder(
                isEnabled = true,
                kind = ReminderKind.WEEKLY,
                day = 3,
                minute = 720,
            ),
            reminder,
        )
    }

    @Test
    fun `GIVEN a weekly day over 7 WHEN reading the row THEN stays on Sunday`() {
        // Given
        val day = 9

        // When
        val reminder = GroupReminder.of(
            isEnabled = true,
            kindCode = "weekly",
            day = day,
            minute = 600,
        )

        // Then
        assertEquals(7, reminder.day)
    }

    @Test
    fun `GIVEN a day under 1 WHEN reading the row THEN stays on the first day`() {
        // Given
        val day = 0

        // When
        val reminder = GroupReminder.of(
            isEnabled = true,
            kindCode = "monthly",
            day = day,
            minute = 600,
        )

        // Then
        assertEquals(1, reminder.day)
    }

    @Test
    fun `GIVEN a monthly day over 31 WHEN reading the row THEN stays on the 31st`() {
        // Given
        val day = 45

        // When
        val reminder = GroupReminder.of(
            isEnabled = true,
            kindCode = "monthly",
            day = day,
            minute = 600,
        )

        // Then
        assertEquals(31, reminder.day)
    }

    @Test
    fun `GIVEN minutes out of the day WHEN reading the row THEN stay between midnight and 23 59`() {
        // Given
        val tooLate = 2_000
        val negative = -5

        // When
        val late = GroupReminder.of(
            isEnabled = true,
            kindCode = "weekly",
            day = 1,
            minute = tooLate,
        )
        val early = GroupReminder.of(
            isEnabled = true,
            kindCode = "weekly",
            day = 1,
            minute = negative,
        )

        // Then
        assertEquals(1_439, late.minute)
        assertEquals(0, early.minute)
    }

    @Test
    fun `GIVEN a monthly reminder on the 15th WHEN switching to weekly THEN the day goes back to Monday`() {
        // Given
        val reminder = monthly15

        // When
        val switched = reminder.withKind(ReminderKind.WEEKLY)

        // Then
        assertEquals(ReminderKind.WEEKLY, switched.kind)
        assertEquals(1, switched.day)
        assertEquals(600, switched.minute)
    }

    @Test
    fun `GIVEN a weekly reminder on Friday WHEN switching to monthly THEN the day goes back to the 1st`() {
        // Given
        val reminder = weeklyMonday.copy(day = 5)

        // When
        val switched = reminder.withKind(ReminderKind.MONTHLY)

        // Then
        assertEquals(ReminderKind.MONTHLY, switched.kind)
        assertEquals(1, switched.day)
    }

    @Test
    fun `GIVEN a reminder on Friday WHEN choosing the kind it already has THEN keeps the day`() {
        // Given
        val reminder = weeklyMonday.copy(day = 5)

        // When
        val same = reminder.withKind(ReminderKind.WEEKLY)

        // Then
        assertEquals(reminder, same)
    }

    @Test
    fun `GIVEN a weekly reminder WHEN choosing a day over 7 THEN stays on Sunday`() {
        // Given
        val reminder = weeklyMonday

        // When
        val changed = reminder.withDay(25)

        // Then
        assertEquals(7, changed.day)
    }

    @Test
    fun `GIVEN a monthly reminder WHEN choosing the 20th THEN keeps it`() {
        // Given
        val reminder = monthly15

        // When
        val changed = reminder.withDay(20)

        // Then
        assertEquals(20, changed.day)
    }

    @Test
    fun `GIVEN a monthly reminder WHEN choosing a day over 31 or under 1 THEN stays in the month`() {
        // Given
        val reminder = monthly15

        // When
        val high = reminder.withDay(40)
        val low = reminder.withDay(0)

        // Then
        assertEquals(31, high.day)
        assertEquals(1, low.day)
    }

    @Test
    fun `GIVEN a reminder WHEN choosing a time out of the day THEN stays between midnight and 23 59`() {
        // Given
        val reminder = weeklyMonday

        // When
        val late = reminder.withMinute(1_500)
        val early = reminder.withMinute(-1)
        val noon = reminder.withMinute(720)

        // Then
        assertEquals(1_439, late.minute)
        assertEquals(0, early.minute)
        assertEquals(720, noon.minute)
    }

    @Test
    fun `GIVEN a reminder turned off WHEN asking when it last fired THEN there is none`() {
        // Given
        val reminder = weeklyMonday.copy(isEnabled = false)
        val now = millisAt(2026, 11, 18, 12)

        // When
        val last = reminder.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertNull(last)
    }

    @Test
    fun `GIVEN a weekly Monday reminder WHEN it is Wednesday THEN it last fired on Monday at 10 00`() {
        // Given
        val now = millisAt(2026, 11, 18, 12)

        // When
        val last = weeklyMonday.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2026, 11, 16, 10), last)
    }

    @Test
    fun `GIVEN a weekly Monday reminder WHEN it is Monday before 10 00 THEN it last fired the Monday before`() {
        // Given
        val now = millisAt(2026, 11, 16, 9, 59)

        // When
        val last = weeklyMonday.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2026, 11, 9, 10), last)
    }

    @Test
    fun `GIVEN a weekly Monday reminder WHEN it is exactly Monday at 10 00 THEN that is the occurrence`() {
        // Given
        val now = millisAt(2026, 11, 16, 10)

        // When
        val last = weeklyMonday.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(now, last)
    }

    @Test
    fun `GIVEN a weekly Monday reminder WHEN it is Monday after 10 00 THEN it fired today`() {
        // Given
        val now = millisAt(2026, 11, 16, 10, 1)

        // When
        val last = weeklyMonday.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2026, 11, 16, 10), last)
    }

    @Test
    fun `GIVEN a weekly Sunday reminder WHEN it is Monday THEN it last fired the day before`() {
        // Given
        val reminder = weeklyMonday.copy(day = 7)
        val now = millisAt(2026, 11, 16, 8)

        // When
        val last = reminder.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2026, 11, 15, 10), last)
    }

    @Test
    fun `GIVEN a monthly reminder on the 15th WHEN it is the 17th THEN it fired on the 15th`() {
        // Given
        val now = millisAt(2026, 11, 17, 8)

        // When
        val last = monthly15.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2026, 11, 15, 10), last)
    }

    @Test
    fun `GIVEN a monthly reminder on the 15th WHEN it is the 15th before 10 00 THEN it fired last month`() {
        // Given
        val now = millisAt(2026, 11, 15, 9, 30)

        // When
        val last = monthly15.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2026, 10, 15, 10), last)
    }

    @Test
    fun `GIVEN a monthly reminder on the 15th WHEN it is exactly the 15th at 10 00 THEN that is the occurrence`() {
        // Given
        val now = millisAt(2026, 11, 15, 10)

        // When
        val last = monthly15.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(now, last)
    }

    @Test
    fun `GIVEN a monthly reminder on the 15th WHEN it is January the 10th THEN it fired in December`() {
        // Given
        val now = millisAt(2027, 1, 10, 12)

        // When
        val last = monthly15.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2026, 12, 15, 10), last)
    }

    @Test
    fun `GIVEN a monthly reminder on the 31st WHEN it is April the 30th after 10 00 THEN it fired today`() {
        // Given
        val now = millisAt(2026, 4, 30, 12)

        // When
        val last = monthly31.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2026, 4, 30, 10), last)
    }

    @Test
    fun `GIVEN a monthly 31st reminder WHEN it is April the 30th before 10 00 THEN it fired on March the 31st`() {
        // Given
        val now = millisAt(2026, 4, 30, 9)

        // When
        val last = monthly31.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2026, 3, 31, 10), last)
    }

    @Test
    fun `GIVEN a monthly reminder on the 31st WHEN it is May the 1st THEN it fired on April the 30th`() {
        // Given
        val now = millisAt(2026, 5, 1, 0, 30)

        // When
        val last = monthly31.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2026, 4, 30, 10), last)
    }

    @Test
    fun `GIVEN a monthly reminder on the 30th WHEN it is February the 28th of a common year THEN it fired today`() {
        // Given
        val now = millisAt(2027, 2, 28, 11)

        // When
        val last = monthly30.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2027, 2, 28, 10), last)
    }

    @Test
    fun `GIVEN a monthly reminder on the 30th WHEN it is February the 28th of a leap year THEN it has not fired yet`() {
        // Given
        val now = millisAt(2028, 2, 28, 11)

        // When
        val last = monthly30.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2028, 1, 30, 10), last)
    }

    @Test
    fun `GIVEN a monthly reminder on the 30th WHEN it is February the 29th of a leap year THEN it fired today`() {
        // Given
        val now = millisAt(2028, 2, 29, 11)

        // When
        val last = monthly30.findLastOccurrenceAtOrBefore(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2028, 2, 29, 10), last)
    }

    @Test
    fun `GIVEN a Monday 00 30 reminder WHEN it is Monday in Tokyo but Sunday in UTC THEN reads the Tokyo Monday`() {
        // Given
        val reminder = weeklyMonday.copy(minute = 30)
        val now = millisAt(2026, 11, 15, 20)

        // When
        val last = reminder.findLastOccurrenceAtOrBefore(now, tokyo)

        // Then
        assertEquals(millisAt(2026, 11, 16, 0, 30, tokyo), last)
    }

    @Test
    fun `GIVEN a Monday 22 00 reminder WHEN it is Tuesday in UTC but Monday in Sao Paulo THEN reads that Monday`() {
        // Given
        val reminder = weeklyMonday.copy(minute = 22 * 60)
        val now = millisAt(2026, 11, 17, 2)

        // When
        val last = reminder.findLastOccurrenceAtOrBefore(now, saoPaulo)

        // Then
        assertEquals(millisAt(2026, 11, 16, 22, 0, saoPaulo), last)
    }

    @Test
    fun `GIVEN the same moment WHEN two members read it on their own clocks THEN each gets their own occurrence`() {
        // Given
        val now = millisAt(2026, 11, 16, 10, 30, tokyo)

        // When
        val inTokyo = weeklyMonday.findLastOccurrenceAtOrBefore(now, tokyo)
        val inSaoPaulo = weeklyMonday.findLastOccurrenceAtOrBefore(now, saoPaulo)

        // Then
        assertEquals(millisAt(2026, 11, 16, 10, 0, tokyo), inTokyo)
        assertEquals(millisAt(2026, 11, 9, 10, 0, saoPaulo), inSaoPaulo)
    }

    @Test
    fun `GIVEN a reminder turned off WHEN asking for the next occurrence THEN there is none`() {
        // Given
        val reminder = weeklyMonday.copy(isEnabled = false)

        // When
        val next = reminder.findNextOccurrenceAfter(millisAt(2026, 11, 18, 12), TimeZone.UTC)

        // Then
        assertNull(next)
    }

    @Test
    fun `GIVEN a weekly Monday reminder WHEN it is Wednesday THEN the next one is the coming Monday`() {
        // Given
        val now = millisAt(2026, 11, 18, 12)

        // When
        val next = weeklyMonday.findNextOccurrenceAfter(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2026, 11, 23, 10), next)
    }

    @Test
    fun `GIVEN a weekly Monday reminder WHEN it is Monday before 10 00 THEN the next one is today`() {
        // Given
        val now = millisAt(2026, 11, 16, 9)

        // When
        val next = weeklyMonday.findNextOccurrenceAfter(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2026, 11, 16, 10), next)
    }

    @Test
    fun `GIVEN a weekly Monday reminder WHEN it is exactly Monday at 10 00 THEN the next one is a week later`() {
        // Given
        val now = millisAt(2026, 11, 16, 10)

        // When
        val next = weeklyMonday.findNextOccurrenceAfter(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2026, 11, 23, 10), next)
    }

    @Test
    fun `GIVEN a monthly reminder on the 15th WHEN it is December the 20th THEN the next one is in January`() {
        // Given
        val now = millisAt(2026, 12, 20, 12)

        // When
        val next = monthly15.findNextOccurrenceAfter(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2027, 1, 15, 10), next)
    }

    @Test
    fun `GIVEN a monthly 31st reminder WHEN it is April the 30th after 10 00 THEN the next one is May the 31st`() {
        // Given
        val now = millisAt(2026, 4, 30, 12)

        // When
        val next = monthly31.findNextOccurrenceAfter(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2026, 5, 31, 10), next)
    }

    @Test
    fun `GIVEN a monthly 31st reminder WHEN it is January the 31st at noon THEN the next one is February the 28th`() {
        // Given
        val now = millisAt(2027, 1, 31, 11)

        // When
        val next = monthly31.findNextOccurrenceAfter(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2027, 2, 28, 10), next)
    }

    @Test
    fun `GIVEN a monthly 30th reminder WHEN it is January 31st of a leap year THEN the next is February 29th`() {
        // Given
        val now = millisAt(2028, 1, 31, 11)

        // When
        val next = monthly30.findNextOccurrenceAfter(now, TimeZone.UTC)

        // Then
        assertEquals(millisAt(2028, 2, 29, 10), next)
    }

    @Test
    fun `GIVEN a Monday reminder WHEN it is Monday 05 00 in Tokyo THEN the next one is that Monday at 10 00`() {
        // Given
        val now = millisAt(2026, 11, 15, 20)

        // When
        val next = weeklyMonday.findNextOccurrenceAfter(now, tokyo)

        // Then
        assertEquals(millisAt(2026, 11, 16, 10, 0, tokyo), next)
    }
}
