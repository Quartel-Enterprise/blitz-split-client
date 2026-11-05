package com.quare.blitzsplit.core.activity.domain.model

import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActivityFeedTest {
    @Test
    fun `GIVEN events read unread own and pending WHEN counting the unread THEN counts only the unread of others`() {
        // Given
        val feed = ActivityFeed(
            events = listOf(
                event(
                    id = "unread-1",
                    seq = 12L,
                ),
                event(
                    id = "unread-2",
                    seq = 11L,
                ),
                event(
                    id = "opened",
                    seq = 13L,
                ),
                event(
                    id = "under-mark",
                    seq = 5L,
                ),
                event(
                    id = "mine",
                    seq = 14L,
                    actorUserId = ME,
                ),
                event(
                    id = "charge-not-up",
                    seq = null,
                    actorUserId = ME,
                ),
            ),
            queued = emptyList(),
            readState = ActivityReadState(
                readThroughSeq = 10L,
                readEventIds = setOf("opened"),
                hasDownloaded = true,
            ),
            targets = ActivityTargets(
                billIds = emptySet(),
                groupIds = emptySet(),
            ),
            myUserId = ME,
            hasOlder = true,
            inAppTypes = NotificationPreferences.DEFAULT.inApp,
        )

        // When
        val unreadCount = feed.unreadCount

        // Then
        assertEquals(2, unreadCount)
    }

    @Test
    fun `GIVEN an unread comment and Comments In app off WHEN asking if it is new THEN it is born read`() {
        // Given
        val comment = event(
            id = "comment",
            seq = 20L,
            kind = ActivityKind.COMMENT_ADDED,
        )
        val feed = feed(
            events = listOf(comment),
            inAppTypes = NotificationPreferences.DEFAULT.inApp - NotificationType.COMMENTS,
        )

        // When
        val isUnread = feed.isUnread(comment)

        // Then
        assertFalse(isUnread)
    }

    @Test
    fun `GIVEN an unread comment and only Comments In app on WHEN asking if it is new THEN it is unread`() {
        // Given
        val comment = event(
            id = "comment",
            seq = 20L,
            kind = ActivityKind.COMMENT_ADDED,
        )
        val feed = feed(
            events = listOf(comment),
            inAppTypes = setOf(NotificationType.COMMENTS),
        )

        // When
        val isUnread = feed.isUnread(comment)

        // Then
        assertTrue(isUnread)
    }

    @Test
    fun `GIVEN unread events of every row and Bills In app off WHEN counting the unread THEN leaves the bills out`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "bill-created",
                    seq = 21L,
                    kind = ActivityKind.BILL_CREATED,
                ),
                event(
                    id = "bill-deleted",
                    seq = 22L,
                    kind = ActivityKind.BILL_DELETED,
                ),
                event(
                    id = "payment",
                    seq = 23L,
                    kind = ActivityKind.PAYMENT_RECORDED,
                ),
                event(
                    id = "comment",
                    seq = 24L,
                    kind = ActivityKind.COMMENT_ADDED,
                ),
                event(
                    id = "member",
                    seq = 25L,
                    kind = ActivityKind.MEMBER_JOINED,
                ),
                event(
                    id = "charge",
                    seq = 26L,
                    kind = ActivityKind.CHARGE_SENT,
                ),
            ),
            inAppTypes = NotificationPreferences.DEFAULT.inApp - NotificationType.BILLS,
        )

        // When
        val unreadCount = feed.unreadCount

        // Then
        assertEquals(4, unreadCount)
    }

    @Test
    fun `GIVEN unread events and every In app off WHEN counting the unread THEN nothing is unread`() {
        // Given
        val events = listOf(
            event(
                id = "bill",
                seq = 21L,
                kind = ActivityKind.BILL_CREATED,
            ),
            event(
                id = "payment",
                seq = 22L,
                kind = ActivityKind.PAYMENT_RECORDED,
            ),
            event(
                id = "charge",
                seq = 23L,
                kind = ActivityKind.CHARGE_SENT,
            ),
        )
        val feed = feed(
            events = events,
            inAppTypes = emptySet(),
        )

        // When
        val unreadCount = feed.unreadCount

        // Then
        assertEquals(0, unreadCount)
        assertTrue(events.none(feed::isUnread))
    }

    @Test
    fun `GIVEN only the balance reminder in In app WHEN counting the unread THEN nothing is unread`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "bill",
                    seq = 21L,
                    kind = ActivityKind.BILL_CREATED,
                ),
                event(
                    id = "payment",
                    seq = 22L,
                    kind = ActivityKind.PAYMENT_RECORDED,
                ),
            ),
            inAppTypes = setOf(NotificationType.BALANCE_REMINDER),
        )

        // When
        val unreadCount = feed.unreadCount

        // Then
        assertEquals(0, unreadCount)
    }

    @Test
    fun `GIVEN Simplify debts changed and only Payments In app on WHEN asking if it is new THEN it is unread`() {
        // Given
        val simplify = event(
            id = "simplify",
            seq = 21L,
            kind = ActivityKind.SIMPLIFY_DEBTS_CHANGED,
        )
        val feed = feed(
            events = listOf(simplify),
            inAppTypes = setOf(NotificationType.PAYMENTS),
        )

        // When
        val isUnread = feed.isUnread(simplify)

        // Then
        assertTrue(isUnread)
    }

    @Test
    fun `GIVEN Simplify debts changed and Payments In app off WHEN asking if it is new THEN it is born read`() {
        // Given
        val simplify = event(
            id = "simplify",
            seq = 21L,
            kind = ActivityKind.SIMPLIFY_DEBTS_CHANGED,
        )
        val feed = feed(
            events = listOf(simplify),
            inAppTypes = NotificationPreferences.DEFAULT.inApp - NotificationType.PAYMENTS,
        )

        // When
        val isUnread = feed.isUnread(simplify)

        // Then
        assertFalse(isUnread)
    }

    @Test
    fun `GIVEN own events and events under the read mark with their row on WHEN asking if new THEN they stay read`() {
        // Given
        val mine = event(
            id = "mine",
            seq = 30L,
            actorUserId = ME,
        )
        val underMark = event(
            id = "under-mark",
            seq = 5L,
        )
        val opened = event(
            id = "opened",
            seq = 31L,
        )
        val feed = feed(
            events = listOf(mine, underMark, opened),
            inAppTypes = setOf(NotificationType.PAYMENTS),
            readState = ActivityReadState(
                readThroughSeq = 10L,
                readEventIds = setOf("opened"),
                hasDownloaded = true,
            ),
        )

        // When
        val unread = feed.events.filter(feed::isUnread)

        // Then
        assertTrue(unread.isEmpty())
    }

    @Test
    fun `GIVEN a pending charge and Payment requests In app on WHEN asking if it is new THEN it is read`() {
        // Given
        val charge = event(
            id = "charge-not-up",
            seq = null,
            kind = ActivityKind.CHARGE_SENT,
        )
        val feed = feed(
            events = listOf(charge),
            inAppTypes = setOf(NotificationType.PAYMENT_REQUESTS),
        )

        // When
        val isUnread = feed.isUnread(charge)

        // Then
        assertFalse(isUnread)
    }

    @Test
    fun `GIVEN no account and events of others in rows on and off WHEN counting THEN counts only the rows on`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "bill",
                    seq = 21L,
                    kind = ActivityKind.BILL_EDITED,
                ),
                event(
                    id = "member",
                    seq = 22L,
                    kind = ActivityKind.MEMBER_ADDED,
                ),
            ),
            inAppTypes = setOf(NotificationType.BILLS),
            myUserId = null,
        )

        // When
        val unreadCount = feed.unreadCount

        // Then
        assertEquals(1, unreadCount)
    }

    private fun feed(
        events: List<ActivityEvent>,
        inAppTypes: Set<NotificationType>,
        myUserId: String? = ME,
        readState: ActivityReadState = ActivityReadState(
            readThroughSeq = 10L,
            readEventIds = emptySet(),
            hasDownloaded = true,
        ),
    ): ActivityFeed = ActivityFeed(
        events = events,
        queued = emptyList(),
        readState = readState,
        targets = ActivityTargets(
            billIds = emptySet(),
            groupIds = emptySet(),
        ),
        myUserId = myUserId,
        hasOlder = false,
        inAppTypes = inAppTypes,
    )

    private fun event(
        id: String,
        seq: Long?,
        kind: ActivityKind = ActivityKind.PAYMENT_RECORDED,
        actorUserId: String? = "ana-user",
    ): ActivityEvent = ActivityEvent(
        id = id,
        seq = seq,
        groupId = "group-1",
        kind = kind,
        actorUserId = actorUserId,
        actorMemberId = null,
        actorName = "Ana",
        groupName = "Apê 302",
        targetId = "payment-1",
        occurredAt = 1_000L,
        subject = null,
        amountMinor = 5_000L,
        code = "pix",
        fromMemberId = "ana",
        toMemberId = "pierre",
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

    private companion object {
        const val ME = "me-user"
    }
}
