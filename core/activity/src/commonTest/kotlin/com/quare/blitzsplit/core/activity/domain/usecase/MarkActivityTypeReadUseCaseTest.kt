package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.model.ActivityReadState
import com.quare.blitzsplit.core.activity.domain.usecase.impl.MarkActivityTypeReadUseCase
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.NotificationType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarkActivityTypeReadUseCaseTest {
    private lateinit var account: FakeAccountRepository
    private lateinit var activity: FakeActivityRepository
    private lateinit var useCase: MarkActivityTypeReadUseCase

    @Test
    fun `GIVEN unread bills from others WHEN marking the bills read THEN saves each of them read`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "bill-1",
                    seq = 11L,
                ),
                event(
                    id = "bill-2",
                    seq = 12L,
                ),
            ),
        )

        // When
        val result = useCase(NotificationType.BILLS)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("bill-1", "bill-2"), activity.savedReadIds)
        assertEquals(setOf("bill-1", "bill-2"), activity.readState.value.readEventIds)
    }

    @Test
    fun `GIVEN unread events of other rows WHEN marking the bills read THEN leaves the other rows unread`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "bill",
                    seq = 11L,
                ),
                event(
                    id = "comment",
                    seq = 12L,
                    kind = ActivityKind.COMMENT_ADDED,
                ),
                event(
                    id = "payment",
                    seq = 13L,
                    kind = ActivityKind.PAYMENT_RECORDED,
                ),
                event(
                    id = "member",
                    seq = 14L,
                    kind = ActivityKind.MEMBER_JOINED,
                ),
            ),
        )

        // When
        useCase(NotificationType.BILLS)

        // Then
        assertEquals(listOf("bill"), activity.savedReadIds)
    }

    @Test
    fun `GIVEN simplify debts turned on WHEN marking the payments read THEN saves it read with the payments`() =
        runTest {
            // Given
            prepareScenario(
                events = listOf(
                    event(
                        id = "payment",
                        seq = 11L,
                        kind = ActivityKind.PAYMENT_RECORDED,
                    ),
                    event(
                        id = "simplify",
                        seq = 12L,
                        kind = ActivityKind.SIMPLIFY_DEBTS_CHANGED,
                    ),
                ),
            )

            // When
            useCase(NotificationType.PAYMENTS)

            // Then
            assertEquals(listOf("payment", "simplify"), activity.savedReadIds)
        }

    @Test
    fun `GIVEN simplify debts turned on WHEN marking people and groups read THEN leaves it unread`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "member",
                    seq = 11L,
                    kind = ActivityKind.MEMBER_JOINED,
                ),
                event(
                    id = "simplify",
                    seq = 12L,
                    kind = ActivityKind.SIMPLIFY_DEBTS_CHANGED,
                ),
            ),
        )

        // When
        useCase(NotificationType.PEOPLE_AND_GROUPS)

        // Then
        assertEquals(listOf("member"), activity.savedReadIds)
    }

    @Test
    fun `GIVEN a charge someone sent WHEN marking the payment requests read THEN saves it read`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "charge",
                    seq = 11L,
                    kind = ActivityKind.CHARGE_SENT,
                ),
                event(
                    id = "payment",
                    seq = 12L,
                    kind = ActivityKind.PAYMENT_RECORDED,
                ),
            ),
        )

        // When
        useCase(NotificationType.PAYMENT_REQUESTS)

        // Then
        assertEquals(listOf("charge"), activity.savedReadIds)
    }

    @Test
    fun `GIVEN a Google account and its own bill WHEN marking the bills read THEN leaves its own bill alone`() =
        runTest {
            // Given
            prepareScenario(
                status = google,
                events = listOf(
                    event(
                        id = "mine",
                        seq = 11L,
                        actorUserId = FakeAccountRepository.CONNECTED_USER_ID,
                    ),
                    event(
                        id = "ana",
                        seq = 12L,
                    ),
                ),
            )

            // When
            useCase(NotificationType.BILLS)

            // Then
            assertEquals(listOf("ana"), activity.savedReadIds)
        }

    @Test
    fun `GIVEN an anonymous user and its own bill WHEN marking the bills read THEN leaves its own bill alone`() =
        runTest {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID),
                events = listOf(
                    event(
                        id = "mine",
                        seq = 11L,
                        actorUserId = FakeAccountRepository.ANONYMOUS_USER_ID,
                    ),
                    event(
                        id = "ana",
                        seq = 12L,
                    ),
                ),
            )

            // When
            useCase(NotificationType.BILLS)

            // Then
            assertEquals(listOf("ana"), activity.savedReadIds)
        }

    @Test
    fun `GIVEN a local only device WHEN marking the bills read THEN saves the bills of others and not its own`() =
        runTest {
            // Given
            prepareScenario(
                status = AccountStatus.LocalOnly,
                events = listOf(
                    event(
                        id = "mine",
                        seq = 11L,
                        actorUserId = null,
                    ),
                    event(
                        id = "ana",
                        seq = 12L,
                    ),
                ),
            )

            // When
            val result = useCase(NotificationType.BILLS)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("ana"), activity.savedReadIds)
        }

    @Test
    fun `GIVEN bills under the read mark WHEN marking the bills read THEN saves only the ones above it`() = runTest {
        // Given
        prepareScenario(
            readState = ActivityReadState.initial.copy(readThroughSeq = 20L),
            events = listOf(
                event(
                    id = "below",
                    seq = 19L,
                ),
                event(
                    id = "at",
                    seq = 20L,
                ),
                event(
                    id = "above",
                    seq = 21L,
                ),
            ),
        )

        // When
        useCase(NotificationType.BILLS)

        // Then
        assertEquals(listOf("above"), activity.savedReadIds)
    }

    @Test
    fun `GIVEN a bill already opened WHEN marking the bills read THEN does not save it again`() = runTest {
        // Given
        prepareScenario(
            readState = ActivityReadState.initial.copy(readEventIds = setOf("opened")),
            events = listOf(
                event(
                    id = "opened",
                    seq = 11L,
                ),
                event(
                    id = "new",
                    seq = 12L,
                ),
            ),
        )

        // When
        useCase(NotificationType.BILLS)

        // Then
        assertEquals(listOf("new"), activity.savedReadIds)
        assertEquals(setOf("opened", "new"), activity.readState.value.readEventIds)
    }

    @Test
    fun `GIVEN a charge not up yet WHEN marking the payment requests read THEN leaves it alone`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "pending",
                    seq = null,
                    kind = ActivityKind.CHARGE_SENT,
                ),
                event(
                    id = "synced",
                    seq = 12L,
                    kind = ActivityKind.CHARGE_SENT,
                ),
            ),
        )

        // When
        useCase(NotificationType.PAYMENT_REQUESTS)

        // Then
        assertEquals(listOf("synced"), activity.savedReadIds)
    }

    @Test
    fun `GIVEN no unread event of the row WHEN marking it read THEN succeeds without saving anything`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "comment",
                    seq = 11L,
                    kind = ActivityKind.COMMENT_ADDED,
                ),
            ),
        )

        // When
        val result = useCase(NotificationType.BILLS)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(activity.savedReadIds.isEmpty())
    }

    @Test
    fun `GIVEN the device fails to save WHEN marking the bills read THEN returns the failure`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "bill",
                    seq = 11L,
                ),
            ),
        )
        activity.markReadError = IllegalStateException("database closed")

        // When
        val result = useCase(NotificationType.BILLS)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private val google = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)

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
        status: AccountStatus = AccountStatus.LocalOnly,
        readState: ActivityReadState = ActivityReadState.initial,
    ) {
        account = FakeAccountRepository(initialStatus = status)
        activity = FakeActivityRepository(initialEvents = events).also { it.readState.value = readState }
        useCase = MarkActivityTypeReadUseCase(
            repository = activity,
            observeAccountStatus = ObserveAccountStatusUseCase(account),
        )
    }
}
