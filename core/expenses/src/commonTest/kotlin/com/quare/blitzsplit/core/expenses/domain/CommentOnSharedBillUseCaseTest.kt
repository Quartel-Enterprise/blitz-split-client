package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CommentOnSharedBillUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillShareLinkRepository
import com.quare.blitzsplit.core.expenses.testing.FakeBillShareLinkRepository.PostedSharedBillComment
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommentOnSharedBillUseCaseTest {
    private lateinit var repository: FakeBillShareLinkRepository
    private lateinit var useCase: CommentOnSharedBillUseCase

    @Test
    fun `GIVEN a reply WHEN commenting through the link THEN sends it with its thread whom it answers and the name`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val result = useCase(
                code = CODE,
                id = COMMENT_ID,
                body = "Já paguei",
                threadId = "comment-1",
                replyToName = "Bruno",
                authorName = "Diego",
            )

            // Then
            assertEquals(SharedBillComment.POSTED, result.getOrThrow())
            val sent = repository.postedComments.single()
            assertEquals(
                PostedSharedBillComment(
                    code = CODE,
                    id = COMMENT_ID,
                    body = "Já paguei",
                    threadId = "comment-1",
                    replyToName = "Bruno",
                    authorName = "Diego",
                ),
                sent,
            )
        }

    @Test
    fun `GIVEN the same comment sent twice WHEN commenting THEN sends the id it was given both times`() = runTest {
        // Given
        prepareScenario()
        comment(body = "Quem trouxe o gelo?")

        // When
        comment(body = "Quem trouxe o gelo?")

        // Then
        assertEquals(listOf(COMMENT_ID, COMMENT_ID), repository.postedComments.map { it.id })
    }

    @Test
    fun `GIVEN spaces and line breaks around the text WHEN commenting THEN sends the text trimmed`() = runTest {
        // Given
        prepareScenario()

        // When
        comment(body = "  \n Valeu, Ana!  \n")

        // Then
        assertEquals("Valeu, Ana!", repository.postedComments.single().body)
    }

    @Test
    fun `GIVEN a blank comment WHEN commenting THEN fails and sends nothing`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = comment(body = "   \n ")

        // Then
        assertTrue(result.isFailure)
        assertTrue(repository.postedComments.isEmpty())
    }

    @Test
    fun `GIVEN a comment over 1000 letters WHEN commenting THEN fails and sends nothing`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = comment(body = "a".repeat(1_001))

        // Then
        assertTrue(result.isFailure)
        assertTrue(repository.postedComments.isEmpty())
    }

    @Test
    fun `GIVEN a comment of 1000 letters with spaces around WHEN commenting THEN sends it`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = comment(body = "  " + "a".repeat(1_000) + "  ")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            1_000,
            repository.postedComments
                .single()
                .body.length,
        )
    }

    @Test
    fun `GIVEN a blank name on this device WHEN commenting THEN sends no name`() = runTest {
        // Given
        prepareScenario()

        // When
        comment(
            body = "Valeu",
            authorName = "   ",
        )

        // Then
        assertNull(repository.postedComments.single().authorName)
    }

    @Test
    fun `GIVEN no name on this device WHEN commenting THEN sends no name`() = runTest {
        // Given
        prepareScenario()

        // When
        comment(
            body = "Valeu",
            authorName = null,
        )

        // Then
        assertNull(repository.postedComments.single().authorName)
    }

    @Test
    fun `GIVEN a name with spaces around WHEN commenting THEN sends the name trimmed`() = runTest {
        // Given
        prepareScenario()

        // When
        comment(
            body = "Valeu",
            authorName = "  Diego ",
        )

        // Then
        assertEquals("Diego", repository.postedComments.single().authorName)
    }

    @Test
    fun `GIVEN a top comment WHEN commenting THEN sends no thread nor anyone answered`() = runTest {
        // Given
        prepareScenario()

        // When
        comment(body = "Valeu")

        // Then
        val sent = repository.postedComments.single()
        assertEquals(null to null, sent.threadId to sent.replyToName)
    }

    @Test
    fun `GIVEN the server knows no name WHEN commenting THEN answers that it needs a name`() = runTest {
        // Given
        prepareScenario(outcome = SharedBillComment.NEEDS_NAME)

        // When
        val result = comment(body = "Valeu")

        // Then
        assertEquals(SharedBillComment.NEEDS_NAME, result.getOrThrow())
    }

    @Test
    fun `GIVEN no internet WHEN commenting THEN fails`() = runTest {
        // Given
        prepareScenario(error = IllegalStateException("offline"))

        // When
        val result = comment(body = "Valeu")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private suspend fun comment(
        body: String,
        authorName: String? = "Diego",
    ): Result<SharedBillComment> = useCase(
        code = CODE,
        id = COMMENT_ID,
        body = body,
        threadId = null,
        replyToName = null,
        authorName = authorName,
    )

    private fun prepareScenario(
        outcome: SharedBillComment = SharedBillComment.POSTED,
        error: Throwable? = null,
    ) {
        repository = FakeBillShareLinkRepository().also {
            it.commentOutcome = outcome
            it.commentError = error
        }
        useCase = CommentOnSharedBillUseCase(repository)
    }

    private companion object {
        const val CODE = "7Hk2pQ8x"
        const val COMMENT_ID = "0b6c1f0e-8f5a-4d7e-9c2a-3e1f5d7b9a11"
    }
}
