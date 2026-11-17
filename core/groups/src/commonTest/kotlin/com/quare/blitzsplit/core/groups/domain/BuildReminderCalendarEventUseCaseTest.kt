package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.groups.domain.usecase.impl.BuildReminderCalendarEventUseCase
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BuildReminderCalendarEventUseCaseTest {
    private lateinit var useCase: BuildReminderCalendarEventUseCase

    @Test
    fun `GIVEN the weekly default WHEN building the event THEN starts at the next occurrence from now`() {
        // Given
        prepareScenario(now = millisAt(2026, 11, 18, 12))

        // When
        val event = useCase(
            groupName = "Viagem Floripa",
            reminder = GroupReminder.Default,
        )

        // Then
        assertNotNull(event)
        assertEquals("Viagem Floripa", event.groupName)
        assertEquals(millisAt(2026, 11, 23, 10), event.startMillis)
    }

    @Test
    fun `GIVEN a device in Tokyo WHEN building the event THEN reads the time on that clock`() {
        // Given
        val tokyo = TimeZone.of("UTC+09:00")
        prepareScenario(
            now = millisAt(2026, 11, 15, 20),
            zone = tokyo,
        )

        // When
        val event = useCase(
            groupName = "Viagem",
            reminder = GroupReminder.Default,
        )

        // Then
        assertEquals("20261116T100000", event?.startLocal)
    }

    @Test
    fun `GIVEN a reminder turned off WHEN building the event THEN there is none`() {
        // Given
        prepareScenario(now = millisAt(2026, 11, 18, 12))
        val reminder = GroupReminder.Default.copy(isEnabled = false)

        // When
        val event = useCase(
            groupName = "Viagem",
            reminder = reminder,
        )

        // Then
        assertNull(event)
    }

    private fun prepareScenario(
        now: Long,
        zone: TimeZone = TimeZone.UTC,
    ) {
        useCase = BuildReminderCalendarEventUseCase(
            currentTimeMillis = { now },
            timeZone = { zone },
        )
    }
}
