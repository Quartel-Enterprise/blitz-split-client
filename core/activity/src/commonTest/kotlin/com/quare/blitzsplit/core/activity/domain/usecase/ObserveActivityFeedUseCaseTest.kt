package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityFeed
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.model.ActivityReadState
import com.quare.blitzsplit.core.activity.domain.model.ActivityTargets
import com.quare.blitzsplit.core.activity.domain.model.QueuedChange
import com.quare.blitzsplit.core.activity.domain.usecase.impl.ObserveActivityFeedUseCase
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
import kotlin.test.assertNull

class ObserveActivityFeedUseCaseTest {
    private lateinit var account: FakeAccountRepository
    private lateinit var activity: FakeActivityRepository
    private lateinit var preferences: FakeNotificationPreferencesRepository
    private lateinit var useCase: ObserveActivityFeedUseCase
    private val feeds = mutableListOf<ActivityFeed>()

    @Test
    fun `GIVEN a Google account and its feed WHEN observing THEN gives everything the feed draws in one`() = runTest {
        // Given
        prepareScenario(status = google)
        val queued = QueuedChange(
            id = "bill-9",
            groupId = GROUP_ID,
            kind = ActivityKind.BILL_CREATED,
            subject = "Pizza",
            amountMinor = 9_000L,
            currency = null,
            code = "equal",
            fromMemberId = null,
            toMemberId = null,
            savedAt = 3_000L,
        )
        val readState = ActivityReadState(
            readThroughSeq = 1L,
            readEventIds = setOf("event-2"),
            hasDownloaded = true,
        )
        val targets = ActivityTargets(
            billIds = setOf("bill-1"),
            groupIds = setOf(GROUP_ID),
        )
        activity.events.value = listOf(event(id = "event-1"))
        activity.queued.value = listOf(queued)
        activity.readState.value = readState
        activity.targets.value = targets
        activity.hasOlder.value = false

        // When
        val feed = useCase().first()

        // Then
        assertEquals(
            ActivityFeed(
                events = listOf(event(id = "event-1")),
                queued = listOf(queued),
                readState = readState,
                targets = targets,
                myUserId = FakeAccountRepository.CONNECTED_USER_ID,
                hasOlder = false,
                inAppTypes = NotificationPreferences.DEFAULT.inApp,
            ),
            feed,
        )
    }

    @Test
    fun `GIVEN an anonymous account WHEN observing THEN its own events are read`() = runTest {
        // Given
        prepareScenario(status = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID))
        activity.events.value = listOf(
            event(
                id = "mine",
                actorUserId = FakeAccountRepository.ANONYMOUS_USER_ID,
            ),
            event(id = "theirs"),
        )

        // When
        val feed = useCase().first()

        // Then
        assertEquals(FakeAccountRepository.ANONYMOUS_USER_ID, feed.myUserId)
        assertEquals(1, feed.unreadCount)
    }

    @Test
    fun `GIVEN no account yet WHEN observing THEN there is no user of the device`() = runTest {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        val feed = useCase().first()

        // Then
        assertNull(feed.myUserId)
    }

    @Test
    fun `GIVEN the feed observed with no account WHEN the first sync registers the device THEN gives its user`() =
        runTest {
            // Given
            prepareScenario(status = AccountStatus.LocalOnly)
            collectFeeds()

            // When
            account.status = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID)
            runCurrent()

            // Then
            assertEquals(listOf(null, FakeAccountRepository.ANONYMOUS_USER_ID), feeds.map { it.myUserId })
        }

    @Test
    fun `GIVEN the feed observed WHEN an event of someone else arrives THEN counts it unread`() = runTest {
        // Given
        prepareScenario(status = google)
        collectFeeds()

        // When
        activity.events.value = listOf(event(id = "event-1"))
        runCurrent()

        // Then
        assertEquals(1, feeds.last().unreadCount)
    }

    @Test
    fun `GIVEN only Bills and Comments In app on WHEN observing THEN the feed carries those rows`() = runTest {
        // Given
        prepareScenario(
            status = google,
            inApp = setOf(NotificationType.BILLS, NotificationType.COMMENTS),
        )

        // When
        val feed = useCase().first()

        // Then
        assertEquals(setOf(NotificationType.BILLS, NotificationType.COMMENTS), feed.inAppTypes)
    }

    @Test
    fun `GIVEN the feed observed WHEN turning Bills In app off THEN gives a feed whose bills are read`() = runTest {
        // Given
        prepareScenario(status = google)
        activity.events.value = listOf(
            event(id = "bill"),
            event(
                id = "payment",
                kind = ActivityKind.PAYMENT_RECORDED,
            ),
        )
        collectFeeds()

        // When
        setInApp(NotificationType.BILLS, isEnabled = false)
        runCurrent()

        // Then
        assertEquals(2, feeds.size)
        assertEquals(NotificationPreferences.DEFAULT.inApp - NotificationType.BILLS, feeds.last().inAppTypes)
        assertEquals(listOf(2, 1), feeds.map { it.unreadCount })
        assertEquals(2, feeds.last().events.size)
    }

    @Test
    fun `GIVEN Bills In app turned off WHEN turning it back on THEN gives a feed whose bills are unread again`() =
        runTest {
            // Given
            prepareScenario(status = google)
            activity.events.value = listOf(
                event(id = "bill-1"),
                event(id = "bill-2"),
            )
            collectFeeds()
            setInApp(NotificationType.BILLS, isEnabled = false)
            runCurrent()

            // When
            setInApp(NotificationType.BILLS, isEnabled = true)
            runCurrent()

            // Then
            assertEquals(listOf(2, 0, 2), feeds.map { it.unreadCount })
            assertEquals(NotificationPreferences.DEFAULT.inApp, feeds.last().inAppTypes)
        }

    @Test
    fun `GIVEN the feed observed WHEN only a push toggle changes THEN gives no new feed`() = runTest {
        // Given
        prepareScenario(status = google)
        collectFeeds()

        // When
        preferences.preferences.update {
            it.with(NotificationType.BILLS, NotificationChannel.PUSH, isEnabled = false)
        }
        runCurrent()

        // Then
        assertEquals(1, feeds.size)
    }

    @Test
    fun `GIVEN no account and Comments In app off WHEN observing THEN only the comments of others are read`() =
        runTest {
            // Given
            prepareScenario(
                status = AccountStatus.LocalOnly,
                inApp = NotificationPreferences.DEFAULT.inApp - NotificationType.COMMENTS,
            )
            activity.events.value = listOf(
                event(id = "bill"),
                event(
                    id = "comment",
                    kind = ActivityKind.COMMENT_ADDED,
                ),
            )

            // When
            val feed = useCase().first()

            // Then
            assertEquals(listOf("bill"), feed.events.filter(feed::isUnread).map { it.id })
        }

    private fun setInApp(
        type: NotificationType,
        isEnabled: Boolean,
    ) {
        preferences.preferences.update { it.with(type, NotificationChannel.IN_APP, isEnabled) }
    }

    private fun TestScope.collectFeeds() {
        backgroundScope.launch { useCase().collect(feeds::add) }
        runCurrent()
    }

    private val google = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)

    private fun event(
        id: String,
        actorUserId: String? = "ana-user",
        kind: ActivityKind = ActivityKind.BILL_CREATED,
    ): ActivityEvent = ActivityEvent(
        id = id,
        seq = 5L,
        groupId = GROUP_ID,
        kind = kind,
        actorUserId = actorUserId,
        actorMemberId = "ana",
        actorName = "Ana",
        groupName = "Apê 302",
        targetId = "bill-1",
        occurredAt = 1_000L,
        subject = "Mercado",
        amountMinor = 12_000L,
        currency = null,
        previousCurrency = null,
        code = "equal",
        fromMemberId = null,
        toMemberId = null,
        changes = emptyList(),
        sharesCount = null,
        body = null,
        isPending = false,
        commentId = null,
        isCommentDeleted = false,
        debtsBefore = emptyList(),
        debtsAfter = emptyList(),
        memberNames = emptyMap(),
    )

    private fun prepareScenario(
        status: AccountStatus,
        inApp: Set<NotificationType> = NotificationPreferences.DEFAULT.inApp,
    ) {
        account = FakeAccountRepository(initialStatus = status)
        activity = FakeActivityRepository()
        preferences = FakeNotificationPreferencesRepository(
            initialPreferences = NotificationPreferences(
                push = NotificationPreferences.DEFAULT.push,
                inApp = inApp,
            ),
        )
        useCase = ObserveActivityFeedUseCase(
            repository = activity,
            observeAccountStatus = ObserveAccountStatusUseCase(account),
            observeNotificationPreferences = ObserveNotificationPreferencesUseCase(preferences),
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
    }
}
