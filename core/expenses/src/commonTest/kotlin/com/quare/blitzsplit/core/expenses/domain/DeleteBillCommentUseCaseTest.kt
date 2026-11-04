package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteBillCommentUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillCommentRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DeleteBillCommentUseCaseTest {
    private lateinit var repository: FakeBillCommentRepository
    private lateinit var useCase: DeleteBillCommentUseCase

    @Test
    fun `GIVEN a synced comment WHEN its author deletes it THEN it stays as a pending tombstone with no text`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val result = useCase(COMMENT_ID)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                original.copy(
                    body = "",
                    isDeleted = true,
                    isPending = true,
                ),
                repository.comments.value.single(),
            )
        }

    @Test
    fun `GIVEN the comment is not on the device WHEN deleting it THEN fails and nothing changes`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase("another-comment")

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(original), repository.comments.value)
    }

    @Test
    fun `GIVEN the device cannot keep the deletion WHEN deleting THEN fails`() = runTest {
        // Given
        prepareScenario(changeError = IllegalStateException("No comment"))

        // When
        val result = useCase(COMMENT_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private val original = BillComment(
        id = COMMENT_ID,
        expenseId = "bill-1",
        groupId = "group-1",
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
        likes = emptyList(),
        isPending = false,
    )

    private fun prepareScenario(changeError: Throwable? = null) {
        repository = FakeBillCommentRepository(initialComments = listOf(original)).also {
            it.changeError = changeError
        }
        useCase = DeleteBillCommentUseCase(repository)
    }

    private companion object {
        const val COMMENT_ID = "comment-1"
    }
}
