package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.usecase.impl.ObserveActivityUnreadCountUseCase
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveNotificationPreferencesUseCase
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
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
    private lateinit var preferences: FakeNotificationPreferencesRepository
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

    @Test
    fun `GIVEN unread bills and payments and Bills In app off WHEN counting THEN counts only the payments`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "bill-created",
                    seq = 1L,
                    kind = ActivityKind.BILL_CREATED,
                ),
                event(
                    id = "attachment",
                    seq = 2L,
                    kind = ActivityKind.ATTACHMENT_ADDED,
                ),
                event(
                    id = "payment",
                    seq = 3L,
                    kind = ActivityKind.PAYMENT_RECORDED,
                ),
                event(
                    id = "simplify",
                    seq = 4L,
                    kind = ActivityKind.SIMPLIFY_DEBTS_CHANGED,
                ),
            ),
            inApp = NotificationPreferences.DEFAULT.inApp - NotificationType.BILLS,
        )

        // When
        val count = useCase().first()

        // Then
        assertEquals(2, count)
    }

    @Test
    fun `GIVEN unread events and every In app off WHEN counting THEN the badge is zero`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "bill",
                    seq = 1L,
                    kind = ActivityKind.BILL_CREATED,
                ),
                event(
                    id = "comment",
                    seq = 2L,
                    kind = ActivityKind.COMMENT_ADDED,
                ),
                event(
                    id = "charge",
                    seq = 3L,
                    kind = ActivityKind.CHARGE_SENT,
                ),
            ),
            inApp = emptySet(),
        )

        // When
        val count = useCase().first()

        // Then
        assertEquals(0, count)
    }

    @Test
    fun `GIVEN the badge observed WHEN turning Comments In app off THEN leaves the comments out`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "bill",
                    seq = 1L,
                    kind = ActivityKind.BILL_CREATED,
                ),
                event(
                    id = "comment-1",
                    seq = 2L,
                    kind = ActivityKind.COMMENT_ADDED,
                ),
                event(
                    id = "comment-2",
                    seq = 3L,
                    kind = ActivityKind.COMMENT_ADDED,
                ),
            ),
        )
        collectCounts()

        // When
        setInApp(NotificationType.COMMENTS, isEnabled = false)
        runCurrent()

        // Then
        assertEquals(listOf(3, 1), counts)
    }

    @Test
    fun `GIVEN People and groups In app turned off WHEN turning it back on THEN its unread events count again`() =
        runTest {
            // Given
            prepareScenario(
                events = listOf(
                    event(
                        id = "bill",
                        seq = 1L,
                        kind = ActivityKind.BILL_CREATED,
                    ),
                    event(
                        id = "member",
                        seq = 2L,
                        kind = ActivityKind.MEMBER_JOINED,
                    ),
                    event(
                        id = "group",
                        seq = 3L,
                        kind = ActivityKind.GROUP_EDITED,
                    ),
                ),
            )
            collectCounts()
            setInApp(NotificationType.PEOPLE_AND_GROUPS, isEnabled = false)
            runCurrent()

            // When
            setInApp(NotificationType.PEOPLE_AND_GROUPS, isEnabled = true)
            runCurrent()

            // Then
            assertEquals(listOf(3, 1, 3), counts)
        }

    @Test
    fun `GIVEN a charge not up yet and Payment requests In app on WHEN counting THEN it is left out`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "charge-up",
                    seq = 1L,
                    kind = ActivityKind.CHARGE_SENT,
                ),
                event(
                    id = "charge-not-up",
                    seq = null,
                    kind = ActivityKind.CHARGE_SENT,
                ),
            ),
            inApp = setOf(NotificationType.PAYMENT_REQUESTS),
        )

        // When
        val count = useCase().first()

        // Then
        assertEquals(1, count)
    }

    @Test
    fun `GIVEN no account and events of others in rows on and off WHEN counting THEN counts only the rows on`() =
        runTest {
            // Given
            prepareScenario(
                events = listOf(
                    event(
                        id = "bill",
                        seq = 1L,
                        kind = ActivityKind.BILL_EDITED,
                    ),
                    event(
                        id = "payment",
                        seq = 2L,
                        kind = ActivityKind.PAYMENT_UNDONE,
                    ),
                ),
                status = AccountStatus.LocalOnly,
                inApp = setOf(NotificationType.PAYMENTS),
            )

            // When
            val count = useCase().first()

            // Then
            assertEquals(1, count)
        }

    private fun setInApp(
        type: NotificationType,
        isEnabled: Boolean,
    ) {
        preferences.preferences.update { it.with(type, NotificationChannel.IN_APP, isEnabled) }
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
        sharesCount = null,
        body = null,
        isPending = seq == null,
        commentId = null,
        isCommentDeleted = false,
        debtsBefore = emptyList(),
        debtsAfter = emptyList(),
        memberNames = emptyMap(),
    )

    private fun prepareScenario(
        events: List<ActivityEvent>,
        status: AccountStatus = AccountStatus.Connected(
            FakeAccountRepository.CONNECTED_USER_ID,
            AccountProvider.GOOGLE,
        ),
        inApp: Set<NotificationType> = NotificationPreferences.DEFAULT.inApp,
    ) {
        activity = FakeActivityRepository(initialEvents = events)
        preferences = FakeNotificationPreferencesRepository(
            initialPreferences = NotificationPreferences(
                push = NotificationPreferences.DEFAULT.push,
                inApp = inApp,
            ),
        )
        useCase = ObserveActivityUnreadCountUseCase(
            repository = activity,
            observeAccountStatus = ObserveAccountStatusUseCase(FakeAccountRepository(initialStatus = status)),
            observeNotificationPreferences = ObserveNotificationPreferencesUseCase(preferences),
        )
    }
}
