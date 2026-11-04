package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillCommentDto
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.provider.room.entity.BillCommentEntity
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

    // Why: every field has a value of its own, so a field read from another would show.
    private fun comment(
        authorUserId: String? = "user-ana",
        authorMemberId: String? = "member-ana",
        viaLink: Boolean = false,
        isPending: Boolean = false,
    ): BillComment = BillComment(
        id = COMMENT_ID,
        expenseId = "bill-1",
        groupId = "group-1",
        authorUserId = authorUserId,
        authorMemberId = authorMemberId,
        authorName = "Ana",
        threadId = "thread-1",
        replyToName = "Diego",
        body = "Já paguei",
        viaLink = viaLink,
        postedAt = 7_000L,
        isPending = isPending,
        editedAt = null,
        isDeleted = false,
        isMine = false,
        likes = emptyList(),
    )

    private fun entity(
        authorUserId: String? = "user-ana",
        authorMemberId: String? = "member-ana",
        viaLink: Boolean = false,
        pendingSync: Boolean = false,
    ): BillCommentEntity = BillCommentEntity(
        id = COMMENT_ID,
        expenseId = "bill-1",
        groupId = "group-1",
        authorUserId = authorUserId,
        authorMemberId = authorMemberId,
        authorName = "Ana",
        threadId = "thread-1",
        replyToName = "Diego",
        body = "Já paguei",
        viaLink = viaLink,
        postedAt = 7_000L,
        pendingSync = pendingSync,
        editedAt = null,
        isDeleted = false,
    )

    private fun dto(
        authorUserId: String?,
        authorMemberId: String? = "member-ana",
        viaLink: Boolean = false,
    ): BillCommentDto = BillCommentDto(
        id = COMMENT_ID,
        expenseId = "bill-1",
        groupId = "group-1",
        authorUserId = authorUserId,
        authorMemberId = authorMemberId,
        authorName = "Ana",
        threadId = "thread-1",
        replyToName = "Diego",
        body = "Já paguei",
        viaLink = viaLink,
        postedAt = 7_000L,
        editedAt = null,
        isDeleted = false,
    )

    private companion object {
        const val COMMENT_ID = "comment-1"
    }
}
