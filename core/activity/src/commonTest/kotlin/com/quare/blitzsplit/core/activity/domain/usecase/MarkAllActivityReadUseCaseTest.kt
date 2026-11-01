package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.usecase.impl.MarkAllActivityReadUseCase
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkAllActivityReadUseCaseTest {
    private lateinit var activity: FakeActivityRepository
    private lateinit var useCase: MarkAllActivityReadUseCase

    @Test
    fun `GIVEN events on the device WHEN marking all read THEN marks through the newest at the current time`() =
        runTest {
            // Given
            prepareScenario(
                events = listOf(
                    event(
                        id = "newest",
                        seq = 30L,
                    ),
                    event(
                        id = "older",
                        seq = 12L,
                    ),
                ),
            )

            // When
            val result = useCase()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(30L to NOW), activity.markAllCalls)
        }

    @Test
    fun `GIVEN an older page loaded WHEN marking all read THEN its events are covered too`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "kept",
                    seq = 100L,
                ),
            ),
        )
        activity.olderPage = listOf(
            event(
                id = "older",
                seq = 40L,
            ),
        )
        activity.loadOlder()

        // When
        useCase()

        // Then
        assertEquals(listOf(100L to NOW), activity.markAllCalls)
        assertEquals(100L, activity.readState.value.readThroughSeq)
    }

    @Test
    fun `GIVEN a charge not up yet as the newest WHEN marking all read THEN marks through the newest with a seq`() =
        runTest {
            // Given
            prepareScenario(
                events = listOf(
                    event(
                        id = "charge",
                        seq = null,
                    ),
                    event(
                        id = "bill",
                        seq = 8L,
                    ),
                ),
            )

            // When
            useCase()

            // Then
            assertEquals(listOf(8L to NOW), activity.markAllCalls)
        }

    @Test
    fun `GIVEN no event with a seq WHEN marking all read THEN succeeds with nothing to mark`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "charge",
                    seq = null,
                ),
            ),
        )

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(activity.markAllCalls.isEmpty())
    }

    private fun event(
        id: String,
        seq: Long?,
    ): ActivityEvent = ActivityEvent(
        id = id,
        seq = seq,
        groupId = "group-1",
        kind = ActivityKind.BILL_CREATED,
        actorUserId = "ana-user",
        actorMemberId = "ana",
        actorName = "Ana",
        groupName = "Apê 302",
        targetId = "target-1",
        occurredAt = 1_000L,
        subject = null,
        amountMinor = null,
        code = null,
        fromMemberId = null,
        toMemberId = null,
        changes = emptyList(),
        isPending = seq == null,
    )

    private fun prepareScenario(events: List<ActivityEvent>) {
        activity = FakeActivityRepository(initialEvents = events)
        useCase = MarkAllActivityReadUseCase(
            repository = activity,
            currentTimeMillis = { NOW },
        )
    }

    private companion object {
        const val NOW = 5_000L
    }
}
