package com.quare.blitzsplit.core.activity.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

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
        )

        // When
        val unreadCount = feed.unreadCount

        // Then
        assertEquals(2, unreadCount)
    }

    private fun event(
        id: String,
        seq: Long?,
        actorUserId: String? = "ana-user",
    ): ActivityEvent = ActivityEvent(
        id = id,
        seq = seq,
        groupId = "group-1",
        kind = ActivityKind.PAYMENT_RECORDED,
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
        isPending = seq == null,
    )

    private companion object {
        const val ME = "me-user"
    }
}
