package com.quare.blitzsplit.core.activity.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActivityReadStateTest {
    private val readState = ActivityReadState(
        readThroughSeq = 10L,
        readEventIds = setOf("opened"),
        hasDownloaded = true,
    )

    @Test
    fun `GIVEN an event of someone else after the mark and never opened WHEN checking it THEN it is unread`() {
        // Given
        val event = event(seq = 11L)

        // When
        val isUnread = readState.isUnread(event, ME)

        // Then
        assertTrue(isUnread)
    }

    @Test
    fun `GIVEN an event this user made WHEN checking it THEN it is read from the start`() {
        // Given
        val event = event(
            seq = 11L,
            actorUserId = ME,
        )

        // When
        val isUnread = readState.isUnread(event, ME)

        // Then
        assertFalse(isUnread)
    }

    @Test
    fun `GIVEN an event at the mark WHEN checking it THEN it is read`() {
        // Given
        val event = event(seq = 10L)

        // When
        val isUnread = readState.isUnread(event, ME)

        // Then
        assertFalse(isUnread)
    }

    @Test
    fun `GIVEN an event after the mark that was opened WHEN checking it THEN it is read`() {
        // Given
        val event = event(
            id = "opened",
            seq = 12L,
        )

        // When
        val isUnread = readState.isUnread(event, ME)

        // Then
        assertFalse(isUnread)
    }

    @Test
    fun `GIVEN a charge not up yet WHEN checking it THEN it is never unread`() {
        // Given
        val event = event(
            seq = null,
            actorUserId = null,
        )

        // When
        val isUnread = readState.isUnread(event, ME)

        // Then
        assertFalse(isUnread)
    }

    @Test
    fun `GIVEN no account on the device WHEN checking an event of someone else THEN it is unread`() {
        // Given
        val event = event(seq = 11L)

        // When
        val isUnread = readState.isUnread(event, myUserId = null)

        // Then
        assertTrue(isUnread)
    }

    @Test
    fun `GIVEN nothing read yet WHEN checking an event of someone else THEN it is unread`() {
        // Given
        val event = event(seq = 1L)

        // When
        val isUnread = ActivityReadState.initial.isUnread(event, ME)

        // Then
        assertTrue(isUnread)
    }

    private fun event(
        id: String = "event-1",
        seq: Long?,
        actorUserId: String? = "ana-user",
    ): ActivityEvent = ActivityEvent(
        id = id,
        seq = seq,
        groupId = "group-1",
        kind = ActivityKind.BILL_CREATED,
        actorUserId = actorUserId,
        actorMemberId = null,
        actorName = "Ana",
        groupName = "Apê 302",
        targetId = "bill-1",
        occurredAt = 1_000L,
        subject = "Mercado",
        amountMinor = 12_000L,
        code = "equal",
        fromMemberId = null,
        toMemberId = null,
        changes = emptyList(),
        isPending = seq == null,
    )

    private companion object {
        const val ME = "me-user"
    }
}
