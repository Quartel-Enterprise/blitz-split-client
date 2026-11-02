package com.quare.blitzsplit.feature.billdetail.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class BillCommentsTest {
    @Test
    fun `GIVEN comments and replies WHEN gathering the threads THEN replies go under their comment in order`() {
        // Given
        val comments = listOf(
            comment(
                id = "c1",
                postedAt = now - 4 * HOUR,
            ),
            comment(
                id = "c2",
                postedAt = now - 3 * HOUR,
            ),
            comment(
                id = "r1",
                threadId = "c1",
                postedAt = now - 2 * HOUR,
            ),
            comment(
                id = "r2",
                threadId = "c1",
                postedAt = now - HOUR,
            ),
        )

        // When
        val threads = threadsOf(comments, group, now, TimeZone.UTC)

        // Then
        assertEquals(listOf("c1", "c2"), threads.map { it.comment.id })
        assertEquals(listOf("r1", "r2"), threads[0].replies.map { it.id })
        assertTrue(threads[1].replies.isEmpty())
    }

    @Test
    fun `GIVEN a reply whose comment is not on the device WHEN gathering THEN it shows as a comment`() {
        // Given
        val comments = listOf(
            comment(
                id = "c1",
                postedAt = now - 2 * HOUR,
            ),
            comment(
                id = "r1",
                threadId = "never-came-down",
                postedAt = now - HOUR,
            ),
        )

        // When
        val threads = threadsOf(comments, group, now, TimeZone.UTC)

        // Then
        assertEquals(listOf("c1", "r1"), threads.map { it.comment.id })
        assertTrue(threads.all { it.replies.isEmpty() })
    }

    @Test
    fun `GIVEN Ana renamed since commenting WHEN gathering the threads THEN shows her name and color now`() {
        // Given
        val comments = listOf(
            comment(
                authorMemberId = ANA,
                authorName = "Ana",
            ),
        )

        // When
        val row = threadsOf(comments, group, now, TimeZone.UTC).single().comment

        // Then
        assertEquals("Ana Paula", row.authorName)
        assertEquals("AP", row.initials)
        assertEquals(AvatarColor.PEACH, row.avatarColor)
        assertFalse(row.isMe)
        assertFalse(row.viaLink)
    }

    @Test
    fun `GIVEN a comment by someone who left WHEN gathering the threads THEN shows the name it was written under`() {
        // Given
        val comments = listOf(
            comment(
                authorMemberId = FORMER,
                authorName = "Carla",
            ),
        )

        // When
        val row = threadsOf(comments, group, now, TimeZone.UTC).single().comment

        // Then
        assertEquals("Carla", row.authorName)
        assertEquals("CA", row.initials)
        assertEquals(AvatarColor.default, row.avatarColor)
        assertFalse(row.isMe)
    }

    @Test
    fun `GIVEN a comment through the open link WHEN gathering the threads THEN says so with its name`() {
        // Given
        val comments = listOf(
            comment(
                authorMemberId = null,
                authorName = "Diego",
                viaLink = true,
            ),
        )

        // When
        val row = threadsOf(comments, group, now, TimeZone.UTC).single().comment

        // Then
        assertTrue(row.viaLink)
        assertEquals("Diego", row.authorName)
        assertFalse(row.isMe)
    }

    @Test
    fun `GIVEN a comment with no member and no name WHEN gathering the threads THEN it has no name nor initials`() {
        // Given
        val comments = listOf(
            comment(
                authorMemberId = null,
                authorName = null,
            ),
        )

        // When
        val row = threadsOf(comments, group, now, TimeZone.UTC).single().comment

        // Then
        assertNull(row.authorName)
        assertEquals("", row.initials)
    }

    @Test
    fun `GIVEN a comment by the device user not up yet WHEN gathering the threads THEN it is theirs and pending`() {
        // Given
        val comments = listOf(
            comment(
                authorMemberId = PIERRE,
                authorName = "Pierre",
                body = "Paguei no cartão",
                isPending = true,
            ),
        )

        // When
        val row = threadsOf(comments, group, now, TimeZone.UTC).single().comment

        // Then
        assertTrue(row.isMe)
        assertTrue(row.isPending)
        assertEquals("Pierre", row.authorName)
        assertEquals("P", row.initials)
        assertEquals(AvatarColor.BLUE, row.avatarColor)
        assertEquals("Paguei no cartão", row.body)
    }

    @Test
    fun `GIVEN a reply to a reply WHEN gathering the threads THEN the row keeps whom it answers`() {
        // Given
        val comments = listOf(
            comment(
                id = "c1",
                postedAt = now - 2 * HOUR,
            ),
            comment(
                id = "r1",
                threadId = "c1",
                replyToName = "Bruno",
                postedAt = now - HOUR,
            ),
        )

        // When
        val reply = threadsOf(comments, group, now, TimeZone.UTC).single().replies.single()

        // Then
        assertEquals("Bruno", reply.replyToName)
    }

    @Test
    fun `GIVEN comments of today yesterday and last week WHEN gathering the threads THEN each says its day`() {
        // Given
        val comments = listOf(
            comment(
                id = "week",
                postedAt = now - 10 * DAY,
            ),
            comment(
                id = "yesterday",
                postedAt = now - DAY,
            ),
            comment(
                id = "today",
                postedAt = now - HOUR,
            ),
        )

        // When
        val threads = threadsOf(comments, group, now, TimeZone.UTC)

        // Then
        assertEquals(
            listOf(RelativeDay.OTHER, RelativeDay.YESTERDAY, RelativeDay.TODAY),
            threads.map { it.comment.postedDay },
        )
        assertEquals(now - HOUR, threads[2].comment.postedAt)
    }

    @Test
    fun `GIVEN a comment of yesterday in the device time zone WHEN gathering the threads THEN it is of yesterday`() {
        // Given
        val saoPaulo = UtcOffset(hours = -3).asTimeZone()
        val earlyToday = Instant.parse("2026-10-30T01:00:00Z").toEpochMilliseconds()
        val comments = listOf(comment(postedAt = earlyToday))

        // When
        val row = threadsOf(comments, group, now, saoPaulo).single().comment

        // Then
        assertEquals(RelativeDay.YESTERDAY, row.postedDay)
    }

    @Test
    fun `GIVEN an author with a photo WHEN gathering the threads THEN the comment shows it in the face`() {
        // Given
        val withPhoto = group.copy(
            members = group.members.map { if (it.id == ANA) it.copy(photoUrl = "https://photos/ana.jpg") else it },
        )
        val comments = listOf(comment(authorMemberId = ANA))

        // When
        val row = threadsOf(comments, withPhoto, now, TimeZone.UTC).single().comment

        // Then
        assertEquals("https://photos/ana.jpg", row.photoUrl)
    }

    private val now = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()

    private val group = Group(
        id = GROUP_ID,
        name = "Viagem de Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        currency = GroupCurrency.BRL,
        members = listOf(
            member(ANA, "Ana Paula", AvatarColor.PEACH),
            member(PIERRE, "Pierre", AvatarColor.BLUE, isMe = true),
            member(BRUNO, "Bruno", AvatarColor.GREEN),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    private fun member(
        id: String,
        name: String,
        color: AvatarColor,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = color,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun comment(
        id: String = "comment-1",
        authorMemberId: String? = BRUNO,
        authorName: String? = "Bruno",
        threadId: String? = null,
        replyToName: String? = null,
        body: String = "Quem trouxe o carvão?",
        viaLink: Boolean = false,
        postedAt: Long = now - HOUR,
        isPending: Boolean = false,
    ): BillComment = BillComment(
        id = id,
        expenseId = EXPENSE_ID,
        groupId = GROUP_ID,
        authorUserId = null,
        authorMemberId = authorMemberId,
        authorName = authorName,
        threadId = threadId,
        replyToName = replyToName,
        body = body,
        viaLink = viaLink,
        postedAt = postedAt,
        isPending = isPending,
    )

    private companion object {
        const val GROUP_ID = "group-1"
        const val EXPENSE_ID = "expense-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val FORMER = "member-9"
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
    }
}
