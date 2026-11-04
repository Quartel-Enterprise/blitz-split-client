package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLike
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.LikeBillCommentUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillCommentRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LikeBillCommentUseCaseTest {
    private lateinit var repository: FakeBillCommentRepository
    private lateinit var useCase: LikeBillCommentUseCase

    @Test
    fun `GIVEN a comment Bruno liked WHEN liking it THEN keeps my like pending beside his`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = like(isLiked = true)

        // Then
        assertTrue(result.isSuccess)
        val comment = repository.comments.value.single()
        assertEquals(
            listOf(
                brunoLike,
                BillCommentLike(
                    memberId = "member-pierre",
                    name = "Pierre",
                    isMine = true,
                ),
            ),
            comment.likes,
        )
        assertTrue(comment.isPending)
    }

    @Test
    fun `GIVEN a comment WHEN liking it THEN stamps the like with this device clock`() = runTest {
        // Given
        prepareScenario(clock = { 9_000L })

        // When
        like(isLiked = true)

        // Then
        assertEquals(listOf(9_000L), repository.likedAts)
    }

    @Test
    fun `GIVEN a comment I liked WHEN undoing the like THEN only Bruno like stays and the change is pending`() =
        runTest {
            // Given
            prepareScenario()
            like(isLiked = true)

            // When
            val result = like(isLiked = false)

            // Then
            assertTrue(result.isSuccess)
            val comment = repository.comments.value.single()
            assertEquals(listOf(brunoLike), comment.likes)
            assertTrue(comment.isPending)
        }

    @Test
    fun `GIVEN a like already stamped WHEN undoing it later THEN stamps the undoing with the later time`() = runTest {
        // Given
        var now = 9_000L
        prepareScenario(clock = { now })
        like(isLiked = true)
        now = 9_500L

        // When
        like(isLiked = false)

        // Then
        assertEquals(listOf(9_000L, 9_500L), repository.likedAts)
    }

    @Test
    fun `GIVEN the comment is not on the device WHEN liking it THEN fails and nothing changes`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            commentId = "another-comment",
            groupId = GROUP_ID,
            isLiked = true,
            memberId = "member-pierre",
            likerName = "Pierre",
        )

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(original), repository.comments.value)
    }

    @Test
    fun `GIVEN the device cannot keep the like WHEN liking THEN fails`() = runTest {
        // Given
        prepareScenario(changeError = IllegalStateException("No comment"))

        // When
        val result = like(isLiked = true)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private suspend fun like(isLiked: Boolean): Result<Unit> = useCase(
        commentId = COMMENT_ID,
        groupId = GROUP_ID,
        isLiked = isLiked,
        memberId = "member-pierre",
        likerName = "Pierre",
    )

    private val brunoLike = BillCommentLike(
        memberId = "member-bruno",
        name = "Bruno",
        isMine = false,
    )

    private val original = BillComment(
        id = COMMENT_ID,
        expenseId = "bill-1",
        groupId = GROUP_ID,
        authorUserId = "user-ana",
        authorMemberId = "member-ana",
        authorName = "Ana",
        threadId = null,
        replyToName = null,
        body = "Quem trouxe o gelo?",
        viaLink = false,
        postedAt = 1_000L,
        editedAt = null,
        isDeleted = false,
        isMine = false,
        likes = listOf(brunoLike),
        isPending = false,
    )

    private fun prepareScenario(
        clock: () -> Long = { 5_000L },
        changeError: Throwable? = null,
    ) {
        repository = FakeBillCommentRepository(initialComments = listOf(original)).also {
            it.changeError = changeError
        }
        useCase = LikeBillCommentUseCase(
            repository = repository,
            currentTimeMillis = { clock() },
        )
    }

    private companion object {
        const val COMMENT_ID = "comment-1"
        const val GROUP_ID = "group-1"
    }
}
