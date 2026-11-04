package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSharedBillCommentUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillShareLinkRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DeleteSharedBillCommentUseCaseTest {
    private lateinit var repository: FakeBillShareLinkRepository
    private lateinit var useCase: DeleteSharedBillCommentUseCase

    @Test
    fun `GIVEN my comment on the link page WHEN deleting it THEN sends the code and the comment and is posted`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val result = useCase(
                code = CODE,
                id = COMMENT_ID,
            )

            // Then
            assertEquals(SharedBillComment.POSTED, result.getOrThrow())
            assertEquals(listOf(CODE to COMMENT_ID), repository.deletedComments)
        }

    @Test
    fun `GIVEN the link became members only WHEN deleting through it THEN answers members only`() = runTest {
        // Given
        prepareScenario(outcome = SharedBillComment.MEMBERS_ONLY)

        // When
        val result = useCase(
            code = CODE,
            id = COMMENT_ID,
        )

        // Then
        assertEquals(SharedBillComment.MEMBERS_ONLY, result.getOrThrow())
    }

    @Test
    fun `GIVEN no internet WHEN deleting through the link THEN fails`() = runTest {
        // Given
        prepareScenario(error = IllegalStateException("offline"))

        // When
        val result = useCase(
            code = CODE,
            id = COMMENT_ID,
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun prepareScenario(
        outcome: SharedBillComment = SharedBillComment.POSTED,
        error: Throwable? = null,
    ) {
        repository = FakeBillShareLinkRepository().also {
            it.changeOutcome = outcome
            it.changeError = error
        }
        useCase = DeleteSharedBillCommentUseCase(repository)
    }

    private companion object {
        const val CODE = "7Hk2pQ8x"
        const val COMMENT_ID = "0b6c1f0e-8f5a-4d7e-9c2a-3e1f5d7b9a11"
    }
}
