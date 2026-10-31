package com.quare.blitzsplit.core.utils

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SystemMinuteTicksTest {
    @Test
    fun `GIVEN a clock 20 seconds into a minute WHEN ticking THEN ticks at once and then as each minute starts`() =
        runTest {
            // Given
            val start = 10 * 60_000L + 20_000L
            val ticks = SystemMinuteTicks { start + testScheduler.currentTime }
            val tickedAt = mutableListOf<Long>()

            // When
            ticks().take(3).collect { tickedAt += start + testScheduler.currentTime }

            // Then
            assertEquals(listOf(start, 11 * 60_000L, 12 * 60_000L), tickedAt)
        }

    @Test
    fun `GIVEN a clock right at the start of a minute WHEN ticking THEN waits a whole minute for the next tick`() =
        runTest {
            // Given
            val start = 10 * 60_000L
            val ticks = SystemMinuteTicks { start + testScheduler.currentTime }
            val tickedAt = mutableListOf<Long>()

            // When
            ticks().take(2).collect { tickedAt += start + testScheduler.currentTime }

            // Then
            assertEquals(listOf(start, 11 * 60_000L), tickedAt)
        }
}
