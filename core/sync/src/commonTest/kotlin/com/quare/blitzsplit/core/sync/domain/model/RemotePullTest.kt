package com.quare.blitzsplit.core.sync.domain.model

import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemotePullTest {
    private val userId = "pierre"

    @Test
    fun `GIVEN events the device may already have WHEN planning THEN still pulls what they tell of`() {
        // Given
        val changes = listOf(
            event(
                kind = ActivityKind.BILL_EDITED,
            ),
            event(
                kind = ActivityKind.COMMENT_ADDED,
            ),
        )

        // When
        val pull = RemotePull.of(changes, userId)

        // Then
        assertEquals(
            expectedPull(
                groupIds = setOf("floripa"),
                pullsActivity = true,
                pullsComments = true,
            ),
            pull,
        )
    }

    @Test
    fun `GIVEN a bill edited in a group WHEN planning THEN pulls that group and the activity`() {
        // Given
        val changes = listOf(
            event(
                kind = ActivityKind.BILL_EDITED,
                groupId = "floripa",
            ),
        )

        // When
        val pull = RemotePull.of(changes, userId)

        // Then
        assertEquals(
            expectedPull(
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
                        kind = kind,
                    ),
                ),
                userId,
            )
        }

        // Then
        pulls.forEach { pull ->
            assertEquals(
                expectedPull(
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
                kind = ActivityKind.COMMENT_ADDED,
            ),
        )

        // When
        val pull = RemotePull.of(changes, userId)

        // Then
        assertEquals(
            expectedPull(
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
                        kind = kind,
                    ),
                ),
                userId,
            )
        }

        // Then
        pulls.forEach { pull ->
            assertEquals(
                expectedPull(
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
                        kind = kind,
                    ),
                ),
                userId,
            )
        }

        // Then
        pulls.forEach { pull -> assertEquals(expectedPull(pullsActivity = true), pull) }
    }

    @Test
    fun `GIVEN each kind that removes or brings in something WHEN planning THEN asks for a full sync`() {
        // Given
        val kinds = listOf(
            ActivityKind.GROUP_DELETED,
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
                        kind = kind,
                    ),
                ),
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
                kind = null,
            ),
        )

        // When
        val pull = RemotePull.of(changes, userId)

        // Then
        assertTrue(pull.isFullSync)
    }

    @Test
    fun `GIVEN a member added to a group WHEN planning THEN pulls the group with the members photos and keys`() {
        // Given
        val changes = listOf(
            event(
                kind = ActivityKind.MEMBER_ADDED,
                groupId = "floripa",
            ),
        )

        // When
        val pull = RemotePull.of(changes, userId)

        // Then
        assertEquals(
            expectedPull(
                groupIds = setOf("floripa"),
                pullsActivity = true,
                pullsMemberPhotos = true,
                pullsMemberKeys = true,
            ),
            pull,
        )
    }

    @Test
    fun `GIVEN the own profile saved on another device WHEN planning THEN pulls the own profile only`() {
        // Given
        val changes = listOf(RemoteChange.ProfileChanged(userId))

        // When
        val pull = RemotePull.of(changes, userId)

        // Then
        assertEquals(expectedPull(pullsOwnProfile = true), pull)
    }

    @Test
    fun `GIVEN someone else saved their profile WHEN planning THEN pulls the members photos only`() {
        // Given
        val changes = listOf(RemoteChange.ProfileChanged("ana"))

        // When
        val pull = RemotePull.of(changes, userId)

        // Then
        assertEquals(expectedPull(pullsMemberPhotos = true), pull)
    }

    @Test
    fun `GIVEN both profiles changed WHEN planning THEN pulls the own profile and the members photos`() {
        // Given
        val changes = listOf(
            RemoteChange.ProfileChanged(userId),
            RemoteChange.ProfileChanged("ana"),
        )

        // When
        val pull = RemotePull.of(changes, userId)

        // Then
        assertEquals(
            expectedPull(
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
                kind = ActivityKind.BILL_CREATED,
            ),
            event(
                kind = ActivityKind.PAYMENT_RECORDED,
            ),
            event(
                kind = ActivityKind.GROUP_EDITED,
            ),
        )

        // When
        val pull = RemotePull.of(changes, userId)

        // Then
        assertEquals(setOf("floripa"), pull.groupIds)
        assertFalse(pull.isFullSync)
    }

    @Test
    fun `GIVEN events of two groups WHEN planning THEN pulls both groups`() {
        // Given
        val changes = listOf(
            event(
                kind = ActivityKind.BILL_CREATED,
                groupId = "floripa",
            ),
            event(
                kind = ActivityKind.PAYMENT_RECORDED,
                groupId = "ape-302",
            ),
        )

        // When
        val pull = RemotePull.of(changes, userId)

        // Then
        assertEquals(setOf("floripa", "ape-302"), pull.groupIds)
    }

    @Test
    fun `GIVEN a comment and a bill of another group WHEN planning THEN pulls the bill group and the comments`() {
        // Given
        val changes = listOf(
            event(
                kind = ActivityKind.COMMENT_ADDED,
                groupId = "ape-302",
            ),
            event(
                kind = ActivityKind.BILL_EDITED,
                groupId = "floripa",
            ),
        )

        // When
        val pull = RemotePull.of(changes, userId)

        // Then
        assertEquals(
            expectedPull(
                groupIds = setOf("floripa"),
                pullsActivity = true,
                pullsComments = true,
            ),
            pull,
        )
    }

    private fun event(
        kind: ActivityKind?,
        groupId: String = "floripa",
    ): RemoteChange.GroupEvent = RemoteChange.GroupEvent(
        groupId = groupId,
        kind = kind,
    )

    private fun expectedPull(
        isFullSync: Boolean = false,
        groupIds: Set<String> = emptySet(),
        pullsActivity: Boolean = false,
        pullsComments: Boolean = false,
        pullsAttachments: Boolean = false,
        pullsOwnProfile: Boolean = false,
        pullsMemberPhotos: Boolean = false,
        pullsMemberKeys: Boolean = false,
    ): RemotePull = RemotePull(
        isFullSync = isFullSync,
        groupIds = groupIds,
        pullsActivity = pullsActivity,
        pullsComments = pullsComments,
        pullsAttachments = pullsAttachments,
        pullsOwnProfile = pullsOwnProfile,
        pullsMemberPhotos = pullsMemberPhotos,
        pullsMemberKeys = pullsMemberKeys,
    )
}
