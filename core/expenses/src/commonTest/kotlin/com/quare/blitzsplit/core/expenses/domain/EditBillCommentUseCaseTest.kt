package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.EditBillCommentUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillCommentRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EditBillCommentUseCaseTest {
    private lateinit var repository: FakeBillCommentRepository
    private lateinit var useCase: EditBillCommentUseCase

    @Test
    fun `GIVEN a synced comment WHEN its author edits it THEN keeps the new text pending at this device clock`() =
        runTest {
            // Given
            prepareScenario(now = 9_000L)

            // When
            val result = useCase(
                id = COMMENT_ID,
                body = "Já paguei, Ana",
            )

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                original.copy(
                    body = "Já paguei, Ana",
                    editedAt = 9_000L,
                    isPending = true,
                ),
                repository.comments.value.single(),
            )
        }

    @Test
    fun `GIVEN spaces and line breaks around the new text WHEN editing THEN keeps the text trimmed`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase(
            id = COMMENT_ID,
            body = "  \n Valeu, Ana!  \n",
        )

        // Then
        assertEquals(
            "Valeu, Ana!",
            repository.comments.value
                .single()
                .body,
        )
    }

    @Test
    fun `GIVEN a blank text WHEN editing THEN fails and the comment stays as it was`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            id = COMMENT_ID,
            body = "   \n ",
        )

        // Then
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertEquals(listOf(original), repository.comments.value)
    }

    @Test
    fun `GIVEN a text over 1000 letters WHEN editing THEN fails and the comment stays as it was`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            id = COMMENT_ID,
            body = "a".repeat(1_001),
        )

        // Then
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertEquals(listOf(original), repository.comments.value)
    }

    @Test
    fun `GIVEN a text of 1000 letters between spaces WHEN editing THEN keeps the 1000 letters`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            id = COMMENT_ID,
            body = "  " + "a".repeat(1_000) + "  ",
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            "a".repeat(1_000),
            repository.comments.value
                .single()
                .body,
        )
    }

    @Test
    fun `GIVEN the comment is not on the device WHEN editing it THEN fails`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            id = "another-comment",
            body = "Já paguei",
        )

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(original), repository.comments.value)
    }

    @Test
    fun `GIVEN the device cannot keep the edit WHEN editing THEN fails`() = runTest {
        // Given
        prepareScenario(changeError = IllegalStateException("No comment to edit"))

        // When
        val result = useCase(
            id = COMMENT_ID,
            body = "Já paguei",
        )

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

    private fun prepareScenario(
        now: Long = 5_000L,
        changeError: Throwable? = null,
    ) {
        repository = FakeBillCommentRepository(initialComments = listOf(original)).also {
            it.changeError = changeError
        }
        useCase = EditBillCommentUseCase(
            repository = repository,
            currentTimeMillis = { now },
        )
    }

    private companion object {
        const val COMMENT_ID = "comment-1"
    }
}
