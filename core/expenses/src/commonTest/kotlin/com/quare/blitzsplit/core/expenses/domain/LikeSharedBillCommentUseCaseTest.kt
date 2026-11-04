package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.LikeSharedBillCommentUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillShareLinkRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LikeSharedBillCommentUseCaseTest {
    private lateinit var repository: FakeBillShareLinkRepository
    private lateinit var useCase: LikeSharedBillCommentUseCase

    @Test
    fun `GIVEN a comment on the link page WHEN liking it THEN sends the like and is posted`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            code = CODE,
            id = COMMENT_ID,
            isLiked = true,
        )

        // Then
        assertEquals(SharedBillComment.POSTED, result.getOrThrow())
        assertEquals(listOf(Triple(CODE, COMMENT_ID, true)), repository.likedComments)
    }

    @Test
    fun `GIVEN a comment I liked on the link page WHEN undoing the like THEN sends it unliked`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase(
            code = CODE,
            id = COMMENT_ID,
            isLiked = false,
        )

        // Then
        assertEquals(listOf(Triple(CODE, COMMENT_ID, false)), repository.likedComments)
    }

    @Test
    fun `GIVEN the link stopped working WHEN liking through it THEN answers not found`() = runTest {
        // Given
        prepareScenario(outcome = SharedBillComment.NOT_FOUND)

        // When
        val result = useCase(
            code = CODE,
            id = COMMENT_ID,
            isLiked = true,
        )

        // Then
        assertEquals(SharedBillComment.NOT_FOUND, result.getOrThrow())
    }

    @Test
    fun `GIVEN no internet WHEN liking through the link THEN fails`() = runTest {
        // Given
        prepareScenario(error = IllegalStateException("offline"))

        // When
        val result = useCase(
            code = CODE,
            id = COMMENT_ID,
            isLiked = true,
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
        useCase = LikeSharedBillCommentUseCase(repository)
    }

    private companion object {
        const val CODE = "7Hk2pQ8x"
        const val COMMENT_ID = "0b6c1f0e-8f5a-4d7e-9c2a-3e1f5d7b9a11"
    }
}
