package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.EditSharedBillCommentUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillShareLinkRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EditSharedBillCommentUseCaseTest {
    private lateinit var repository: FakeBillShareLinkRepository
    private lateinit var useCase: EditSharedBillCommentUseCase

    @Test
    fun `GIVEN my comment on the link page WHEN editing it THEN sends the code the comment and the text`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            code = CODE,
            id = COMMENT_ID,
            body = "Já paguei, Ana",
        )

        // Then
        assertEquals(SharedBillComment.POSTED, result.getOrThrow())
        assertEquals(listOf(Triple(CODE, COMMENT_ID, "Já paguei, Ana")), repository.editedComments)
    }

    @Test
    fun `GIVEN spaces and line breaks around the new text WHEN editing through the link THEN sends it trimmed`() =
        runTest {
            // Given
            prepareScenario()

            // When
            useCase(
                code = CODE,
                id = COMMENT_ID,
                body = "  \n Valeu, Ana!  \n",
            )

            // Then
            assertEquals("Valeu, Ana!", repository.editedComments.single().third)
        }

    @Test
    fun `GIVEN a blank text WHEN editing through the link THEN fails and sends nothing`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            code = CODE,
            id = COMMENT_ID,
            body = "   \n ",
        )

        // Then
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertTrue(repository.editedComments.isEmpty())
    }

    @Test
    fun `GIVEN a text over 1000 letters WHEN editing through the link THEN fails and sends nothing`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            code = CODE,
            id = COMMENT_ID,
            body = "a".repeat(1_001),
        )

        // Then
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertTrue(repository.editedComments.isEmpty())
    }

    @Test
    fun `GIVEN a text of 1000 letters between spaces WHEN editing through the link THEN sends the 1000 letters`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val result = useCase(
                code = CODE,
                id = COMMENT_ID,
                body = "  " + "a".repeat(1_000) + "  ",
            )

            // Then
            assertTrue(result.isSuccess)
            assertEquals("a".repeat(1_000), repository.editedComments.single().third)
        }

    @Test
    fun `GIVEN the link stopped working WHEN editing through it THEN answers not found`() = runTest {
        // Given
        prepareScenario(outcome = SharedBillComment.NOT_FOUND)

        // When
        val result = useCase(
            code = CODE,
            id = COMMENT_ID,
            body = "Já paguei",
        )

        // Then
        assertEquals(SharedBillComment.NOT_FOUND, result.getOrThrow())
    }

    @Test
    fun `GIVEN no internet WHEN editing through the link THEN fails`() = runTest {
        // Given
        prepareScenario(error = IllegalStateException("offline"))

        // When
        val result = useCase(
            code = CODE,
            id = COMMENT_ID,
            body = "Já paguei",
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
        useCase = EditSharedBillCommentUseCase(repository)
    }

    private companion object {
        const val CODE = "7Hk2pQ8x"
        const val COMMENT_ID = "0b6c1f0e-8f5a-4d7e-9c2a-3e1f5d7b9a11"
    }
}
