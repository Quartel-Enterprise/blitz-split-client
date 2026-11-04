package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveBillCommentsUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillCommentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveBillCommentsUseCaseTest {
    private lateinit var repository: FakeBillCommentRepository
    private lateinit var useCase: ObserveBillCommentsUseCase

    @Test
    fun `GIVEN comments on two bills WHEN observing one bill THEN shows only its comments`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                comment(
                    id = "c-1",
                    expenseId = BILL_ID,
                    postedAt = 1L,
                ),
                comment(
                    id = "c-2",
                    expenseId = "other-bill",
                    postedAt = 2L,
                ),
                comment(
                    id = "c-3",
                    expenseId = BILL_ID,
                    postedAt = 3L,
                ),
            ),
        )

        // When
        val comments = useCase(BILL_ID).first()

        // Then
        assertEquals(listOf("c-1", "c-3"), comments.map { it.id })
    }

    @Test
    fun `GIVEN a bill nobody commented WHEN observing it THEN shows no comment`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                comment(
                    id = "c-1",
                    expenseId = "other-bill",
                    postedAt = 1L,
                ),
            ),
        )

        // When
        val comments = useCase(BILL_ID).first()

        // Then
        assertTrue(comments.isEmpty())
    }

    private fun comment(
        id: String,
        expenseId: String,
        postedAt: Long,
    ): BillComment = BillComment(
        id = id,
        expenseId = expenseId,
        groupId = "group-1",
        authorUserId = "user-1",
        authorMemberId = "member-1",
        authorName = "Ana",
        threadId = null,
        replyToName = null,
        body = "Paguei no Pix",
        viaLink = false,
        postedAt = postedAt,
        isPending = false,
        editedAt = null,
        isDeleted = false,
        isMine = false,
        likes = emptyList(),
    )

    private fun prepareScenario(stored: List<BillComment>) {
        repository = FakeBillCommentRepository(stored)
        useCase = ObserveBillCommentsUseCase(repository)
    }

    private companion object {
        const val BILL_ID = "bill-1"
    }
}
