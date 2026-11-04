package com.quare.blitzsplit.feature.billdetail.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLike
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentLiker
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
        assertEquals("C", row.initials)
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

    @Test
    fun `GIVEN no member known WHEN gathering the threads THEN each comment goes by the name it was written under`() {
        // Given
        val comments = listOf(
            comment(
                id = "c1",
                authorMemberId = ANA,
                authorName = "Ana",
                postedAt = now - 2 * HOUR,
            ),
            comment(
                id = "r1",
                authorMemberId = null,
                authorName = "Diego",
                threadId = "c1",
                replyToName = "Ana",
                viaLink = true,
                postedAt = now - HOUR,
            ),
        )

        // When
        val thread = threadsOf(comments, emptyList(), now, TimeZone.UTC).single()

        // Then
        assertEquals("Ana", thread.comment.authorName)
        assertEquals("A", thread.comment.initials)
        assertEquals(AvatarColor.default, thread.comment.avatarColor)
        assertNull(thread.comment.photoUrl)
        assertFalse(thread.comment.isMe)
        val reply = thread.replies.single()
        assertEquals("Diego", reply.authorName)
        assertTrue(reply.viaLink)
        assertEquals("Ana", reply.replyToName)
    }

    @Test
    fun `GIVEN the members listed WHEN gathering the threads THEN the comment of the device user is theirs`() {
        // Given
        val comments = listOf(
            comment(
                authorMemberId = PIERRE,
                authorName = "Pierre V",
            ),
        )

        // When
        val row = threadsOf(comments, group.members, now, TimeZone.UTC).single().comment

        // Then
        assertTrue(row.isMe)
        assertEquals("Pierre", row.authorName)
        assertEquals("P", row.initials)
        assertEquals(AvatarColor.BLUE, row.avatarColor)
    }

    @Test
    fun `GIVEN no member known and no name WHEN gathering the threads THEN it has no name nor initials`() {
        // Given
        val comments = listOf(
            comment(
                authorMemberId = null,
                authorName = null,
            ),
        )

        // When
        val row = threadsOf(comments, emptyList(), now, TimeZone.UTC).single().comment

        // Then
        assertNull(row.authorName)
        assertEquals("", row.initials)
    }

    @Test
    fun `GIVEN a comment by Ana in the group WHEN gathering the threads THEN its author opens Ana`() {
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
        assertEquals(ANA, row.authorMemberId)
    }

    @Test
    fun `GIVEN a comment by the device user WHEN gathering the threads THEN its author opens them too`() {
        // Given
        val comments = listOf(
            comment(
                authorMemberId = PIERRE,
                authorName = "Pierre",
            ),
        )

        // When
        val row = threadsOf(comments, group, now, TimeZone.UTC).single().comment

        // Then
        assertEquals(PIERRE, row.authorMemberId)
    }

    @Test
    fun `GIVEN a reply by Bruno to Ana WHEN gathering the threads THEN each author opens their own member`() {
        // Given
        val comments = listOf(
            comment(
                id = "c1",
                authorMemberId = ANA,
                authorName = "Ana",
                postedAt = now - 2 * HOUR,
            ),
            comment(
                id = "r1",
                threadId = "c1",
                postedAt = now - HOUR,
            ),
        )

        // When
        val thread = threadsOf(comments, group, now, TimeZone.UTC).single()

        // Then
        assertEquals(ANA, thread.comment.authorMemberId)
        assertEquals(BRUNO, thread.replies.single().authorMemberId)
    }

    @Test
    fun `GIVEN a comment by someone who left WHEN gathering the threads THEN its author opens nobody`() {
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
        assertNull(row.authorMemberId)
    }

    @Test
    fun `GIVEN a comment through the link by someone outside the group WHEN gathering THEN its author opens nobody`() {
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
        assertNull(row.authorMemberId)
    }

    @Test
    fun `GIVEN no member known WHEN gathering the threads THEN no author opens a profile`() {
        // Given
        val comments = listOf(
            comment(
                authorMemberId = ANA,
                authorName = "Ana",
            ),
        )

        // When
        val row = threadsOf(comments, emptyList(), now, TimeZone.UTC).single().comment

        // Then
        assertNull(row.authorMemberId)
    }

    @Test
    fun `GIVEN a deleted comment with a reply WHEN gathering the threads THEN keeps Comentario apagado over it`() {
        // Given
        val comments = listOf(
            comment(
                id = "c1",
                authorMemberId = PIERRE,
                authorName = "Pierre",
                body = "",
                postedAt = now - 2 * HOUR,
                isDeleted = true,
                likes = listOf(like(memberId = ANA)),
            ),
            comment(
                id = "r1",
                threadId = "c1",
                postedAt = now - HOUR,
            ),
        )

        // When
        val thread = threadsOf(comments, group, now, TimeZone.UTC).single()

        // Then
        val row = thread.comment
        assertEquals("c1", row.id)
        assertTrue(row.isDeleted)
        assertTrue(row.likers.isEmpty())
        assertEquals(0, row.likeCount)
        assertFalse(row.isLikedByMe)
        assertFalse(row.canInteract)
        assertFalse(row.canChange)
        assertEquals(listOf("r1"), thread.replies.map { it.id })
    }

    @Test
    fun `GIVEN a deleted comment without replies WHEN gathering the threads THEN it is gone`() {
        // Given
        val comments = listOf(
            comment(
                id = "c1",
                body = "",
                postedAt = now - 2 * HOUR,
                isDeleted = true,
            ),
            comment(
                id = "c2",
                postedAt = now - HOUR,
            ),
        )

        // When
        val threads = threadsOf(comments, group, now, TimeZone.UTC)

        // Then
        assertEquals(listOf("c2"), threads.map { it.comment.id })
    }

    @Test
    fun `GIVEN a deleted comment and its only reply deleted WHEN gathering the threads THEN the thread is gone`() {
        // Given
        val comments = listOf(
            comment(
                id = "c1",
                body = "",
                postedAt = now - 2 * HOUR,
                isDeleted = true,
            ),
            comment(
                id = "r1",
                threadId = "c1",
                body = "",
                postedAt = now - HOUR,
                isDeleted = true,
            ),
        )

        // When
        val threads = threadsOf(comments, group, now, TimeZone.UTC)

        // Then
        assertTrue(threads.isEmpty())
    }

    @Test
    fun `GIVEN a deleted reply WHEN gathering the threads THEN only the other replies show under the comment`() {
        // Given
        val comments = listOf(
            comment(
                id = "c1",
                postedAt = now - 3 * HOUR,
            ),
            comment(
                id = "r1",
                threadId = "c1",
                body = "",
                postedAt = now - 2 * HOUR,
                isDeleted = true,
            ),
            comment(
                id = "r2",
                threadId = "c1",
                postedAt = now - HOUR,
            ),
        )

        // When
        val thread = threadsOf(comments, group, now, TimeZone.UTC).single()

        // Then
        assertFalse(thread.comment.isDeleted)
        assertEquals(listOf("r2"), thread.replies.map { it.id })
    }

    @Test
    fun `GIVEN a comment edited and one never edited WHEN gathering the threads THEN only the first says editado`() {
        // Given
        val comments = listOf(
            comment(
                id = "c1",
                postedAt = now - 2 * HOUR,
                editedAt = now - HOUR,
            ),
            comment(
                id = "c2",
                postedAt = now - HOUR,
            ),
        )

        // When
        val threads = threadsOf(comments, group, now, TimeZone.UTC)

        // Then
        assertTrue(threads[0].comment.isEdited)
        assertFalse(threads[1].comment.isEdited)
    }

    @Test
    fun `GIVEN likes by Ana someone who left and someone through the link WHEN gathering THEN names each liker`() {
        // Given
        val comments = listOf(
            comment(
                likes = listOf(
                    like(
                        memberId = ANA,
                        name = "Ana",
                    ),
                    like(
                        memberId = FORMER,
                        name = "Carla",
                    ),
                    like(
                        memberId = null,
                        name = "Diego",
                    ),
                    like(
                        memberId = null,
                        name = null,
                    ),
                ),
            ),
        )

        // When
        val row = threadsOf(comments, group, now, TimeZone.UTC).single().comment

        // Then
        assertEquals(
            listOf(
                BillCommentLiker(
                    name = "Ana Paula",
                    isMe = false,
                ),
                BillCommentLiker(
                    name = "Carla",
                    isMe = false,
                ),
                BillCommentLiker(
                    name = "Diego",
                    isMe = false,
                ),
                BillCommentLiker(
                    name = null,
                    isMe = false,
                ),
            ),
            row.likers,
        )
        assertEquals(4, row.likeCount)
        assertFalse(row.isLikedByMe)
    }

    @Test
    fun `GIVEN the device user liked it on this device WHEN gathering the threads THEN the heart is filled`() {
        // Given
        val comments = listOf(
            comment(
                likes = listOf(
                    like(memberId = ANA),
                    like(
                        memberId = PIERRE,
                        name = "Pierre",
                        isMine = true,
                    ),
                ),
            ),
        )

        // When
        val row = threadsOf(comments, group, now, TimeZone.UTC).single().comment

        // Then
        assertTrue(row.isLikedByMe)
        assertEquals(listOf(false, true), row.likers.map { it.isMe })
        assertEquals(2, row.likeCount)
    }

    @Test
    fun `GIVEN the member of the device user liked it on another device WHEN gathering THEN the like is theirs`() {
        // Given
        val comments = listOf(
            comment(
                likes = listOf(
                    like(
                        memberId = PIERRE,
                        name = "Pierre",
                        isMine = false,
                    ),
                ),
            ),
        )

        // When
        val row = threadsOf(comments, group, now, TimeZone.UTC).single().comment

        // Then
        assertEquals(
            listOf(
                BillCommentLiker(
                    name = "Pierre",
                    isMe = true,
                ),
            ),
            row.likers,
        )
        assertTrue(row.isLikedByMe)
    }

    @Test
    fun `GIVEN a comment of the device user WHEN gathering the threads THEN they can change it and interact`() {
        // Given
        val comments = listOf(
            comment(
                authorMemberId = PIERRE,
                authorName = "Pierre",
            ),
        )

        // When
        val row = threadsOf(comments, group, now, TimeZone.UTC).single().comment

        // Then
        assertTrue(row.canChange)
        assertTrue(row.canInteract)
    }

    @Test
    fun `GIVEN a comment of Bruno WHEN gathering the threads THEN the device user only interacts with it`() {
        // Given
        val comments = listOf(comment())

        // When
        val row = threadsOf(comments, group, now, TimeZone.UTC).single().comment

        // Then
        assertFalse(row.canChange)
        assertTrue(row.canInteract)
    }

    @Test
    fun `GIVEN the server says the caller wrote it on the link page WHEN gathering THEN it is theirs to change`() {
        // Given
        val comments = listOf(
            comment(
                authorMemberId = null,
                authorName = "Pierre",
                isMine = true,
                viaLink = true,
            ),
        )

        // When
        val row = threadsOf(comments, emptyList(), now, TimeZone.UTC).single().comment

        // Then
        assertTrue(row.isMe)
        assertTrue(row.canChange)
    }

    @Test
    fun `GIVEN the server says the caller liked it on the link page WHEN gathering THEN the heart is filled`() {
        // Given
        val comments = listOf(
            comment(
                likes = listOf(
                    like(
                        memberId = null,
                        name = "Pierre",
                        isMine = true,
                    ),
                ),
            ),
        )

        // When
        val row = threadsOf(comments, emptyList(), now, TimeZone.UTC).single().comment

        // Then
        assertTrue(row.isLikedByMe)
        assertEquals(
            listOf(
                BillCommentLiker(
                    name = "Pierre",
                    isMe = true,
                ),
            ),
            row.likers,
        )
    }

    @Test
    fun `GIVEN a deleted comment kept over a reply WHEN replying to it THEN there is no reply target`() {
        // Given
        val threads = threadsOf(
            listOf(
                comment(
                    id = "c1",
                    body = "",
                    postedAt = now - 2 * HOUR,
                    isDeleted = true,
                ),
                comment(
                    id = "r1",
                    threadId = "c1",
                    postedAt = now - HOUR,
                ),
            ),
            group,
            now,
            TimeZone.UTC,
        )

        // When
        val target = replyTargetOf(threads, "c1")

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a reply under a deleted comment WHEN replying to it THEN the reply joins that thread`() {
        // Given
        val threads = threadsOf(
            listOf(
                comment(
                    id = "c1",
                    body = "",
                    postedAt = now - 2 * HOUR,
                    isDeleted = true,
                ),
                comment(
                    id = "r1",
                    threadId = "c1",
                    postedAt = now - HOUR,
                ),
            ),
            group,
            now,
            TimeZone.UTC,
        )

        // When
        val target = replyTargetOf(threads, "r1")

        // Then
        assertEquals("c1", target?.threadId)
        assertEquals("r1", target?.commentId)
        assertEquals("Bruno", target?.name)
    }

    @Test
    fun `GIVEN a comment that is not shown WHEN replying to it THEN there is no reply target`() {
        // Given
        val threads = threadsOf(listOf(comment(id = "c1")), group, now, TimeZone.UTC)

        // When
        val target = replyTargetOf(threads, "unknown")

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a comment and its reply WHEN finding the reply THEN returns it`() {
        // Given
        val threads = threadsOf(
            listOf(
                comment(
                    id = "c1",
                    postedAt = now - 2 * HOUR,
                ),
                comment(
                    id = "r1",
                    threadId = "c1",
                    authorMemberId = ANA,
                    authorName = "Ana",
                    postedAt = now - HOUR,
                ),
            ),
            group,
            now,
            TimeZone.UTC,
        )

        // When
        val found = threads.findComment("r1")

        // Then
        assertEquals("r1", found?.id)
        assertEquals("Ana Paula", found?.authorName)
    }

    @Test
    fun `GIVEN a deleted reply WHEN finding it THEN finds nothing`() {
        // Given
        val threads = threadsOf(
            listOf(
                comment(
                    id = "c1",
                    postedAt = now - 2 * HOUR,
                ),
                comment(
                    id = "r1",
                    threadId = "c1",
                    body = "",
                    postedAt = now - HOUR,
                    isDeleted = true,
                ),
            ),
            group,
            now,
            TimeZone.UTC,
        )

        // When
        val found = threads.findComment("r1")

        // Then
        assertNull(found)
    }

    private val now = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()

    private val group = Group(
        id = GROUP_ID,
        name = "Viagem de Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        shape = GroupShape.SQUARE,
        photo = null,
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
        editedAt: Long? = null,
        isDeleted: Boolean = false,
        isMine: Boolean = false,
        likes: List<BillCommentLike> = emptyList(),
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
        editedAt = editedAt,
        isDeleted = isDeleted,
        isMine = isMine,
        likes = likes,
    )

    private fun like(
        memberId: String?,
        name: String? = null,
        isMine: Boolean = false,
    ): BillCommentLike = BillCommentLike(
        memberId = memberId,
        name = name,
        isMine = isMine,
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
