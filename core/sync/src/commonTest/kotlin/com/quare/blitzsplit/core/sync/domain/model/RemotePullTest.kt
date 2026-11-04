package com.quare.blitzsplit.core.sync.domain.model

import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemotePullTest {
    private val userId = "pierre"
    private val newestSeq = 10L

    @Test
    fun `GIVEN only events already on the device WHEN planning THEN pulls nothing`() {
        // Given
        val changes = listOf(
            event(
                seq = 9L,
                kind = ActivityKind.BILL_EDITED,
            ),
            event(
                seq = 10L,
                kind = ActivityKind.COMMENT_ADDED,
            ),
        )

        // When
        val pull = RemotePull.of(changes, newestSeq, userId)

        // Then
        assertEquals(RemotePull(), pull)
    }

    @Test
    fun `GIVEN a bill edited in a group WHEN planning THEN pulls that group and the activity`() {
        // Given
        val changes = listOf(
            event(
                seq = 11L,
                kind = ActivityKind.BILL_EDITED,
                groupId = "floripa",
            ),
        )

        // When
        val pull = RemotePull.of(changes, newestSeq, userId)

        // Then
        assertEquals(
            RemotePull(
                groupIds = setOf("floripa"),
                pullsActivity = true,
            ),
            pull,
        )
    }

    @Test
    fun `GIVEN each kind reaching one group WHEN planning THEN pulls that group and the activity`() {
        // Given
        val kinds = listOf(
            ActivityKind.BILL_CREATED,
            ActivityKind.BILL_EDITED,
            ActivityKind.BILL_DELETED,
            ActivityKind.PAYMENT_RECORDED,
            ActivityKind.PAYMENT_UNDONE,
            ActivityKind.GROUP_CREATED,
            ActivityKind.GROUP_EDITED,
            ActivityKind.INVITE_LINK_CHANGED,
            ActivityKind.MEMBER_RENAMED,
        )

        // When
        val pulls = kinds.map { kind ->
            RemotePull.of(
                listOf(
                    event(
                        seq = 11L,
                        kind = kind,
                    ),
                ),
                newestSeq,
                userId,
            )
        }

        // Then
        pulls.forEach { pull ->
            assertEquals(
                RemotePull(
                    groupIds = setOf("floripa"),
                    pullsActivity = true,
                ),
                pull,
            )
        }
    }

    @Test
    fun `GIVEN a comment added WHEN planning THEN pulls the comments and the activity but no group`() {
        // Given
        val changes = listOf(
            event(
                seq = 11L,
                kind = ActivityKind.COMMENT_ADDED,
            ),
        )

        // When
        val pull = RemotePull.of(changes, newestSeq, userId)

        // Then
        assertEquals(
            RemotePull(
                pullsComments = true,
                pullsActivity = true,
            ),
            pull,
        )
    }

    @Test
    fun `GIVEN a photo added or removed WHEN planning THEN pulls the attachments and the activity but no group`() {
        // Given
        val kinds = listOf(ActivityKind.ATTACHMENT_ADDED, ActivityKind.ATTACHMENT_REMOVED)

        // When
        val pulls = kinds.map { kind ->
            RemotePull.of(
                listOf(
                    event(
                        seq = 11L,
                        kind = kind,
                    ),
                ),
                newestSeq,
                userId,
            )
        }

        // Then
        pulls.forEach { pull ->
            assertEquals(
                RemotePull(
                    pullsAttachments = true,
                    pullsActivity = true,
                ),
                pull,
            )
        }
    }

    @Test
    fun `GIVEN a charge sent or a share link changed WHEN planning THEN pulls only the activity`() {
        // Given
        val kinds = listOf(ActivityKind.CHARGE_SENT, ActivityKind.BILL_LINK_CHANGED)

        // When
        val pulls = kinds.map { kind ->
            RemotePull.of(
                listOf(
                    event(
                        seq = 11L,
                        kind = kind,
                    ),
                ),
                newestSeq,
                userId,
            )
        }

        // Then
        pulls.forEach { pull -> assertEquals(RemotePull(pullsActivity = true), pull) }
    }

    @Test
    fun `GIVEN each kind that removes or brings in something WHEN planning THEN asks for a full sync`() {
        // Given
        val kinds = listOf(
            ActivityKind.GROUP_DELETED,
            ActivityKind.MEMBER_ADDED,
            ActivityKind.MEMBER_JOINED,
            ActivityKind.MEMBER_LEFT,
            ActivityKind.MEMBER_REMOVED,
            ActivityKind.MEMBER_RELEASED,
        )

        // When
        val pulls = kinds.map { kind ->
            RemotePull.of(
                listOf(
                    event(
                        seq = 11L,
                        kind = kind,
                    ),
                ),
                newestSeq,
                userId,
            )
        }

        // Then
        pulls.forEach { pull -> assertTrue(pull.isFullSync) }
    }

    @Test
    fun `GIVEN a kind a newer server records WHEN planning THEN asks for a full sync`() {
        // Given
        val changes = listOf(
            event(
                seq = 11L,
                kind = null,
            ),
        )

        // When
        val pull = RemotePull.of(changes, newestSeq, userId)

        // Then
        assertTrue(pull.isFullSync)
    }

    @Test
    fun `GIVEN the channel joined among other changes WHEN planning THEN asks for a full sync`() {
        // Given
        val changes = listOf(
            event(
                seq = 11L,
                kind = ActivityKind.BILL_EDITED,
            ),
            RemoteChange.Joined,
        )

        // When
        val pull = RemotePull.of(changes, newestSeq, userId)

        // Then
        assertTrue(pull.isFullSync)
    }

    @Test
    fun `GIVEN a member who joined already on the device WHEN planning THEN does not ask for a full sync`() {
        // Given
        val changes = listOf(
            event(
                seq = 10L,
                kind = ActivityKind.MEMBER_JOINED,
            ),
            event(
                seq = 11L,
                kind = ActivityKind.BILL_CREATED,
            ),
        )

        // When
        val pull = RemotePull.of(changes, newestSeq, userId)

        // Then
        assertEquals(
            RemotePull(
                groupIds = setOf("floripa"),
                pullsActivity = true,
            ),
            pull,
        )
    }

    @Test
    fun `GIVEN the own profile saved on another device WHEN planning THEN pulls the own profile only`() {
        // Given
        val changes = listOf(RemoteChange.ProfileChanged(userId))

        // When
        val pull = RemotePull.of(changes, newestSeq, userId)

        // Then
        assertEquals(RemotePull(pullsOwnProfile = true), pull)
    }

    @Test
    fun `GIVEN someone else saved their profile WHEN planning THEN pulls the members photos only`() {
        // Given
        val changes = listOf(RemoteChange.ProfileChanged("ana"))

        // When
        val pull = RemotePull.of(changes, newestSeq, userId)

        // Then
        assertEquals(RemotePull(pullsMemberPhotos = true), pull)
    }

    @Test
    fun `GIVEN both profiles changed alongside old events WHEN planning THEN pulls both whatever the seq`() {
        // Given
        val changes = listOf(
            event(
                seq = 3L,
                kind = ActivityKind.BILL_EDITED,
            ),
            RemoteChange.ProfileChanged(userId),
            RemoteChange.ProfileChanged("ana"),
        )

        // When
        val pull = RemotePull.of(changes, newestSeq, userId)

        // Then
        assertEquals(
            RemotePull(
                pullsOwnProfile = true,
                pullsMemberPhotos = true,
            ),
            pull,
        )
    }

    @Test
    fun `GIVEN several events of the same group WHEN planning THEN pulls the group once`() {
        // Given
        val changes = listOf(
            event(
                seq = 11L,
                kind = ActivityKind.BILL_CREATED,
            ),
            event(
                seq = 12L,
                kind = ActivityKind.PAYMENT_RECORDED,
            ),
            event(
                seq = 13L,
                kind = ActivityKind.GROUP_EDITED,
            ),
        )

        // When
        val pull = RemotePull.of(changes, newestSeq, userId)

        // Then
        assertEquals(setOf("floripa"), pull.groupIds)
        assertFalse(pull.isFullSync)
    }

    @Test
    fun `GIVEN events of two groups WHEN planning THEN pulls both groups`() {
        // Given
        val changes = listOf(
            event(
                seq = 11L,
                kind = ActivityKind.BILL_CREATED,
                groupId = "floripa",
            ),
            event(
                seq = 12L,
                kind = ActivityKind.PAYMENT_RECORDED,
                groupId = "ape-302",
            ),
        )

        // When
        val pull = RemotePull.of(changes, newestSeq, userId)

        // Then
        assertEquals(setOf("floripa", "ape-302"), pull.groupIds)
    }

    @Test
    fun `GIVEN a comment and a bill of another group WHEN planning THEN pulls the bill group and the comments`() {
        // Given
        val changes = listOf(
            event(
                seq = 11L,
                kind = ActivityKind.COMMENT_ADDED,
                groupId = "ape-302",
            ),
            event(
                seq = 12L,
                kind = ActivityKind.BILL_EDITED,
                groupId = "floripa",
            ),
        )

        // When
        val pull = RemotePull.of(changes, newestSeq, userId)

        // Then
        assertEquals(
            RemotePull(
                groupIds = setOf("floripa"),
                pullsActivity = true,
                pullsComments = true,
            ),
            pull,
        )
    }

    private fun event(
        seq: Long,
        kind: ActivityKind?,
        groupId: String = "floripa",
    ): RemoteChange.GroupEvent = RemoteChange.GroupEvent(
        seq = seq,
        groupId = groupId,
        kind = kind,
    )
}
