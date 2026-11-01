package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.usecase.impl.ObserveActivityUnreadCountUseCase
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveActivityUnreadCountUseCaseTest {
    private lateinit var activity: FakeActivityRepository
    private lateinit var useCase: ObserveActivityUnreadCountUseCase
    private val counts = mutableListOf<Int>()

    @Test
    fun `GIVEN events of every kind and read state WHEN counting THEN counts every unread event of others`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "bill",
                    seq = 11L,
                    kind = ActivityKind.BILL_EDITED,
                ),
                event(
                    id = "payment",
                    seq = 12L,
                    kind = ActivityKind.PAYMENT_RECORDED,
                ),
                event(
                    id = "member",
                    seq = 13L,
                    kind = ActivityKind.MEMBER_JOINED,
                ),
                event(
                    id = "under-mark",
                    seq = 9L,
                ),
                event(
                    id = "opened",
                    seq = 14L,
                ),
                event(
                    id = "mine",
                    seq = 15L,
                    actorUserId = FakeAccountRepository.CONNECTED_USER_ID,
                ),
                event(
                    id = "charge-not-up",
                    seq = null,
                    kind = ActivityKind.CHARGE_SENT,
                    actorUserId = FakeAccountRepository.CONNECTED_USER_ID,
                ),
            ),
        )
        activity.readState.update {
            it.copy(
                readThroughSeq = 10L,
                readEventIds = setOf("opened"),
            )
        }

        // When
        val count = useCase().first()

        // Then
        assertEquals(3, count)
    }

    @Test
    fun `GIVEN the badge observed WHEN an event of this user arrives THEN does not emit the same count again`() =
        runTest {
            // Given
            prepareScenario(
                events = listOf(
                    event(
                        id = "theirs",
                        seq = 1L,
                    ),
                ),
            )
            collectCounts()

            // When
            activity.events.update {
                it + event(
                    id = "mine",
                    seq = 2L,
                    actorUserId = FakeAccountRepository.CONNECTED_USER_ID,
                )
            }
            runCurrent()

            // Then
            assertEquals(listOf(1), counts)
        }

    @Test
    fun `GIVEN the badge observed WHEN marking all read THEN goes to zero`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "event-1",
                    seq = 1L,
                ),
                event(
                    id = "event-2",
                    seq = 2L,
                ),
            ),
        )
        collectCounts()

        // When
        activity.saveReadThrough(
            throughSeq = 2L,
            now = 5_000L,
        )
        runCurrent()

        // Then
        assertEquals(listOf(2, 0), counts)
    }

    @Test
    fun `GIVEN the badge observed WHEN opening an unread event THEN counts one less`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "event-1",
                    seq = 1L,
                ),
                event(
                    id = "event-2",
                    seq = 2L,
                ),
            ),
        )
        collectCounts()

        // When
        activity.saveRead("event-2")
        runCurrent()

        // Then
        assertEquals(listOf(2, 1), counts)
    }

    private fun TestScope.collectCounts() {
        backgroundScope.launch { useCase().collect(counts::add) }
        runCurrent()
    }

    private fun event(
        id: String,
        seq: Long?,
        kind: ActivityKind = ActivityKind.BILL_CREATED,
        actorUserId: String? = "ana-user",
    ): ActivityEvent = ActivityEvent(
        id = id,
        seq = seq,
        groupId = "group-1",
        kind = kind,
        actorUserId = actorUserId,
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
        useCase = ObserveActivityUnreadCountUseCase(
            repository = activity,
            observeAccountStatus = ObserveAccountStatusUseCase(
                FakeAccountRepository(
                    initialStatus = AccountStatus.Connected(
                        FakeAccountRepository.CONNECTED_USER_ID,
                        AccountProvider.GOOGLE,
                    ),
                ),
            ),
        )
    }
}
