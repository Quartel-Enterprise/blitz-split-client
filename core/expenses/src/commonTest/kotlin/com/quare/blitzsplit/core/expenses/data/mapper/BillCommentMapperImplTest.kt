package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillCommentDto
import com.quare.blitzsplit.core.expenses.data.dto.BillCommentLikeDto
import com.quare.blitzsplit.core.expenses.data.dto.RemoteBillCommentLikeDto
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLike
import com.quare.blitzsplit.core.provider.room.entity.BillCommentEntity
import com.quare.blitzsplit.core.provider.room.entity.BillCommentLikeEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class BillCommentMapperImplTest {
    private val mapper = BillCommentMapperImpl()

    @Test
    fun `GIVEN a stored comment waiting to go up WHEN mapping to the domain THEN reads every field and pending`() {
        // Given
        val entity = entity(pendingSync = true)

        // When
        val comment = mapper.toDomain(entity, likes = emptyList())

        // Then
        assertEquals(comment(isPending = true), comment)
    }

    @Test
    fun `GIVEN a synced comment WHEN mapping to the domain THEN reads it not pending`() {
        // Given
        val entity = entity(pendingSync = false)

        // When
        val comment = mapper.toDomain(entity, likes = emptyList())

        // Then
        assertEquals(comment(isPending = false), comment)
    }

    @Test
    fun `GIVEN a comment written through the open link WHEN mapping to the domain THEN keeps it with no member`() {
        // Given
        val entity = entity(
            authorUserId = null,
            authorMemberId = null,
            viaLink = true,
        )

        // When
        val comment = mapper.toDomain(entity, likes = emptyList())

        // Then
        assertEquals(
            comment(
                authorUserId = null,
                authorMemberId = null,
                viaLink = true,
            ),
            comment,
        )
    }

    @Test
    fun `GIVEN a new comment WHEN mapping it to store THEN copies every field and stays pending`() {
        // Given
        val comment = comment(
            authorUserId = null,
            isPending = true,
        )

        // When
        val entity = mapper.toEntity(comment)

        // Then
        assertEquals(
            entity(
                authorUserId = null,
                pendingSync = true,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a comment not pending WHEN mapping it to store THEN stores it synced`() {
        // Given
        val comment = comment(isPending = false)

        // When
        val entity = mapper.toEntity(comment)

        // Then
        assertEquals(entity(pendingSync = false), entity)
    }

    @Test
    fun `GIVEN a comment from the server WHEN mapping it to store THEN copies every field and stores it synced`() {
        // Given
        val dto = dto(authorUserId = "user-ana")

        // When
        val entity = mapper.toEntity(dto)

        // Then
        assertEquals(entity(pendingSync = false), entity)
    }

    @Test
    fun `GIVEN a server comment through the open link WHEN mapping it to store THEN keeps it with no author`() {
        // Given
        val dto = dto(
            authorUserId = null,
            authorMemberId = null,
            viaLink = true,
        )

        // When
        val entity = mapper.toEntity(dto)

        // Then
        assertEquals(
            entity(
                authorUserId = null,
                authorMemberId = null,
                viaLink = true,
                pendingSync = false,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a comment written before it had an account WHEN mapping it to send THEN the caller is its author`() {
        // Given
        val entity = entity(
            authorUserId = null,
            pendingSync = true,
        )

        // When
        val dto = mapper.toDto(entity, userId = "user-caller")

        // Then
        assertEquals(dto(authorUserId = "user-caller"), dto)
    }

    @Test
    fun `GIVEN a stored comment of another account WHEN mapping it to send THEN the caller is its author`() {
        // Given
        val entity = entity(authorUserId = "user-ana")

        // When
        val dto = mapper.toDto(entity, userId = "user-caller")

        // Then
        assertEquals("user-caller", dto.authorUserId)
    }

    @Test
    fun `GIVEN a comment edited here WHEN mapping it to the domain THEN reads when it was edited`() {
        // Given
        val entity = entity(
            body = "Já paguei, Ana",
            editedAt = 9_000L,
            pendingSync = true,
        )

        // When
        val comment = mapper.toDomain(entity, likes = emptyList())

        // Then
        assertEquals(
            comment(
                body = "Já paguei, Ana",
                editedAt = 9_000L,
                isPending = true,
            ),
            comment,
        )
    }

    @Test
    fun `GIVEN a deleted comment WHEN mapping it to the domain THEN reads it deleted with no text`() {
        // Given
        val entity = entity(
            body = "",
            isDeleted = true,
        )

        // When
        val comment = mapper.toDomain(entity, likes = emptyList())

        // Then
        assertEquals(
            comment(
                body = "",
                isDeleted = true,
            ),
            comment,
        )
    }

    @Test
    fun `GIVEN my like and Bruno like WHEN mapping the comment to the domain THEN reads both with mine marked`() {
        // Given
        val entity = entity()
        val likes = listOf(
            likeEntity(
                likerKey = "user-bruno",
                memberId = "member-bruno",
                likerName = "Bruno",
            ),
            likeEntity(
                likerKey = BillCommentLikeEntity.ME,
                memberId = "member-pierre",
                likerName = "Pierre",
            ),
        )

        // When
        val comment = mapper.toDomain(entity, likes)

        // Then
        assertEquals(
            listOf(
                BillCommentLike(
                    memberId = "member-bruno",
                    name = "Bruno",
                    isMine = false,
                ),
                BillCommentLike(
                    memberId = "member-pierre",
                    name = "Pierre",
                    isMine = true,
                ),
            ),
            comment.likes,
        )
    }

    @Test
    fun `GIVEN a stored comment WHEN mapping it to the domain THEN it is not marked as mine`() {
        // Given
        val entity = entity(authorUserId = "user-caller")

        // When
        val comment = mapper.toDomain(entity, likes = emptyList())

        // Then
        assertEquals(false, comment.isMine)
    }

    @Test
    fun `GIVEN an edited comment WHEN mapping it to store THEN keeps when it was edited`() {
        // Given
        val comment = comment(
            body = "Já paguei, Ana",
            editedAt = 9_000L,
            isPending = true,
        )

        // When
        val entity = mapper.toEntity(comment)

        // Then
        assertEquals(
            entity(
                body = "Já paguei, Ana",
                editedAt = 9_000L,
                pendingSync = true,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a deleted comment WHEN mapping it to store THEN keeps it deleted`() {
        // Given
        val comment = comment(
            body = "",
            isDeleted = true,
        )

        // When
        val entity = mapper.toEntity(comment)

        // Then
        assertEquals(
            entity(
                body = "",
                isDeleted = true,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a comment edited on the server WHEN mapping it to store THEN keeps the edit synced`() {
        // Given
        val dto = dto(
            authorUserId = "user-ana",
            body = "Já paguei, Ana",
            editedAt = 9_000L,
        )

        // When
        val entity = mapper.toEntity(dto)

        // Then
        assertEquals(
            entity(
                body = "Já paguei, Ana",
                editedAt = 9_000L,
                pendingSync = false,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a comment deleted on the server WHEN mapping it to store THEN keeps it deleted and synced`() {
        // Given
        val dto = dto(
            authorUserId = "user-ana",
            body = "",
            isDeleted = true,
        )

        // When
        val entity = mapper.toEntity(dto)

        // Then
        assertEquals(
            entity(
                body = "",
                isDeleted = true,
                pendingSync = false,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN an edit waiting to go up WHEN mapping it to send THEN sends the new text and when it was edited`() {
        // Given
        val entity = entity(
            body = "Já paguei, Ana",
            editedAt = 9_000L,
            pendingSync = true,
        )

        // When
        val dto = mapper.toDto(entity, userId = "user-caller")

        // Then
        assertEquals(
            dto(
                authorUserId = "user-caller",
                body = "Já paguei, Ana",
                editedAt = 9_000L,
            ),
            dto,
        )
    }

    @Test
    fun `GIVEN a tombstone waiting to go up WHEN mapping it to send THEN sends it deleted with no text`() {
        // Given
        val entity = entity(
            body = "",
            isDeleted = true,
            pendingSync = true,
        )

        // When
        val dto = mapper.toDto(entity, userId = "user-caller")

        // Then
        assertEquals(
            dto(
                authorUserId = "user-caller",
                body = "",
                isDeleted = true,
            ),
            dto,
        )
    }

    @Test
    fun `GIVEN the users own like on the server WHEN mapping it to store THEN keeps it under ME and synced`() {
        // Given
        val dto = remoteLikeDto(userId = "user-caller")

        // When
        val entity = mapper.toEntity(dto, userId = "user-caller")

        // Then
        assertEquals(
            likeEntity(
                likerKey = BillCommentLikeEntity.ME,
                memberId = "member-bruno",
                likerName = "Bruno",
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN someone else like on the server WHEN mapping it to store THEN keeps it under their account`() {
        // Given
        val dto = remoteLikeDto(userId = "user-bruno")

        // When
        val entity = mapper.toEntity(dto, userId = "user-caller")

        // Then
        assertEquals(
            likeEntity(
                likerKey = "user-bruno",
                memberId = "member-bruno",
                likerName = "Bruno",
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a like undone on the server WHEN mapping it to store THEN keeps it unliked`() {
        // Given
        val dto = remoteLikeDto(
            userId = "user-bruno",
            isLiked = false,
        )

        // When
        val entity = mapper.toEntity(dto, userId = "user-caller")

        // Then
        assertEquals(false, entity.isLiked)
    }

    @Test
    fun `GIVEN my like waiting to go up WHEN mapping it to send THEN sends it as the caller at its version`() {
        // Given
        val like = likeEntity(
            likerKey = BillCommentLikeEntity.ME,
            memberId = "member-pierre",
            likerName = "Pierre",
            isLiked = false,
            pendingSync = true,
        )

        // When
        val dto = mapper.toDto(like, userId = "user-caller")

        // Then
        assertEquals(
            BillCommentLikeDto(
                commentId = COMMENT_ID,
                userId = "user-caller",
                groupId = "group-1",
                isLiked = false,
                updatedAt = 8_000L,
            ),
            dto,
        )
    }

    // Why: every field has a value of its own, so a field read from another would show.
    private fun comment(
        authorUserId: String? = "user-ana",
        authorMemberId: String? = "member-ana",
        viaLink: Boolean = false,
        isPending: Boolean = false,
        body: String = "Já paguei",
        editedAt: Long? = null,
        isDeleted: Boolean = false,
    ): BillComment = BillComment(
        id = COMMENT_ID,
        expenseId = "bill-1",
        groupId = "group-1",
        authorUserId = authorUserId,
        authorMemberId = authorMemberId,
        authorName = "Ana",
        threadId = "thread-1",
        replyToName = "Diego",
        body = body,
        viaLink = viaLink,
        postedAt = 7_000L,
        isPending = isPending,
        editedAt = editedAt,
        isDeleted = isDeleted,
        isMine = false,
        likes = emptyList(),
    )

    private fun entity(
        authorUserId: String? = "user-ana",
        authorMemberId: String? = "member-ana",
        viaLink: Boolean = false,
        pendingSync: Boolean = false,
        body: String = "Já paguei",
        editedAt: Long? = null,
        isDeleted: Boolean = false,
    ): BillCommentEntity = BillCommentEntity(
        id = COMMENT_ID,
        expenseId = "bill-1",
        groupId = "group-1",
        authorUserId = authorUserId,
        authorMemberId = authorMemberId,
        authorName = "Ana",
        threadId = "thread-1",
        replyToName = "Diego",
        body = body,
        viaLink = viaLink,
        postedAt = 7_000L,
        pendingSync = pendingSync,
        editedAt = editedAt,
        isDeleted = isDeleted,
    )

    private fun dto(
        authorUserId: String?,
        authorMemberId: String? = "member-ana",
        viaLink: Boolean = false,
        body: String = "Já paguei",
        editedAt: Long? = null,
        isDeleted: Boolean = false,
    ): BillCommentDto = BillCommentDto(
        id = COMMENT_ID,
        expenseId = "bill-1",
        groupId = "group-1",
        authorUserId = authorUserId,
        authorMemberId = authorMemberId,
        authorName = "Ana",
        threadId = "thread-1",
        replyToName = "Diego",
        body = body,
        viaLink = viaLink,
        postedAt = 7_000L,
        editedAt = editedAt,
        isDeleted = isDeleted,
    )

    private fun likeEntity(
        likerKey: String,
        memberId: String?,
        likerName: String?,
        isLiked: Boolean = true,
        pendingSync: Boolean = false,
    ): BillCommentLikeEntity = BillCommentLikeEntity(
        commentId = COMMENT_ID,
        likerKey = likerKey,
        groupId = "group-1",
        memberId = memberId,
        likerName = likerName,
        isLiked = isLiked,
        updatedAt = 8_000L,
        pendingSync = pendingSync,
    )

    private fun remoteLikeDto(
        userId: String,
        isLiked: Boolean = true,
    ): RemoteBillCommentLikeDto = RemoteBillCommentLikeDto(
        id = "like-1",
        commentId = COMMENT_ID,
        userId = userId,
        groupId = "group-1",
        memberId = "member-bruno",
        likerName = "Bruno",
        isLiked = isLiked,
        updatedAt = 8_000L,
    )

    private companion object {
        const val COMMENT_ID = "comment-1"
    }
}
