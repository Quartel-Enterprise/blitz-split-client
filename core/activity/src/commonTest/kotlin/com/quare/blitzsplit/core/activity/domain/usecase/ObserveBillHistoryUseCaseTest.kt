package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.usecase.impl.ObserveBillHistoryUseCase
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveBillHistoryUseCaseTest {
    private lateinit var activity: FakeActivityRepository
    private lateinit var useCase: ObserveBillHistoryUseCase

    @Test
    fun `GIVEN a bill with edits photos a link and comments WHEN observing its history THEN leaves the comments out`() =
        runTest {
            // Given
            prepareScenario(
                events = listOf(
                    event(
                        id = "comment",
                        kind = ActivityKind.COMMENT_ADDED,
                    ),
                    event(
                        id = "link",
                        kind = ActivityKind.BILL_LINK_CHANGED,
                    ),
                    event(
                        id = "photo-removed",
                        kind = ActivityKind.ATTACHMENT_REMOVED,
                    ),
                    event(
                        id = "photo-added",
                        kind = ActivityKind.ATTACHMENT_ADDED,
                    ),
                    event(
                        id = "edited",
                        kind = ActivityKind.BILL_EDITED,
                    ),
                    event(
                        id = "created",
                        kind = ActivityKind.BILL_CREATED,
                    ),
                ),
            )

            // When
            val history = useCase(BILL_ID).first()

            // Then
            assertEquals(listOf("link", "photo-removed", "photo-added", "edited", "created"), history.map { it.id })
        }

    @Test
    fun `GIVEN events about another bill WHEN observing a bill history THEN shows none of them`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "other",
                    kind = ActivityKind.BILL_CREATED,
                    targetId = "bill-2",
                ),
            ),
        )

        // When
        val history = useCase(BILL_ID).first()

        // Then
        assertTrue(history.isEmpty())
    }

    private fun event(
        id: String,
        kind: ActivityKind,
        targetId: String = BILL_ID,
    ): ActivityEvent = ActivityEvent(
        id = id,
        seq = 1L,
        groupId = "group-1",
        kind = kind,
        actorUserId = "ana-user",
        actorMemberId = "ana",
        actorName = "Ana",
        groupName = "Apê 302",
        targetId = targetId,
        occurredAt = 1_000L,
        subject = "Mercado",
        amountMinor = null,
        code = null,
        fromMemberId = null,
        toMemberId = null,
        changes = emptyList(),
        sharesCount = null,
        body = null,
        isPending = false,
    )

    private fun prepareScenario(events: List<ActivityEvent>) {
        activity = FakeActivityRepository(initialEvents = events)
        useCase = ObserveBillHistoryUseCase(activity)
    }

    private companion object {
        const val BILL_ID = "bill-1"
    }
}
