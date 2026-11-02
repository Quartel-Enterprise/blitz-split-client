package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.AddBillCommentUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillCommentRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AddBillCommentUseCaseTest {
    private lateinit var repository: FakeBillCommentRepository
    private lateinit var useCase: AddBillCommentUseCase

    @Test
    fun `GIVEN a reply WHEN adding it THEN saves it pending at the current time with no account yet`() = runTest {
        // Given
        prepareScenario(now = 7_000L)

        // When
        val result = useCase(
            expenseId = BILL_ID,
            groupId = GROUP_ID,
            body = "Já paguei",
            threadId = "thread-1",
            replyToName = "Diego",
            authorMemberId = "member-ana",
            authorName = "Ana",
        )

        // Then
        assertTrue(result.isSuccess)
        val saved = repository.comments.value.single()
        assertEquals(
            BillComment(
                id = saved.id,
                expenseId = BILL_ID,
                groupId = GROUP_ID,
                authorUserId = null,
                authorMemberId = "member-ana",
                authorName = "Ana",
                threadId = "thread-1",
                replyToName = "Diego",
                body = "Já paguei",
                viaLink = false,
                postedAt = 7_000L,
                isPending = true,
            ),
            saved,
        )
        assertTrue(saved.id.isNotBlank())
    }

    @Test
    fun `GIVEN a top comment WHEN adding it THEN saves it with no thread nor anyone answered`() = runTest {
        // Given
        prepareScenario()

        // When
        addComment(body = "Quem trouxe o gelo?")

        // Then
        val saved = repository.comments.value.single()
        assertEquals(null to null, saved.threadId to saved.replyToName)
    }

    @Test
    fun `GIVEN spaces and line breaks around the text WHEN adding the comment THEN saves the text trimmed`() = runTest {
        // Given
        prepareScenario()

        // When
        addComment(body = "  \n Valeu, Ana!  \n")

        // Then
        assertEquals(
            "Valeu, Ana!",
            repository.comments.value
                .single()
                .body,
        )
    }

    @Test
    fun `GIVEN a comment already added WHEN adding another THEN each gets its own id`() = runTest {
        // Given
        prepareScenario()
        addComment(body = "Primeiro")

        // When
        addComment(body = "Segundo")

        // Then
        val ids = repository.comments.value.map { it.id }
        assertEquals(2, ids.size)
        assertNotEquals(ids[0], ids[1])
    }

    @Test
    fun `GIVEN a blank comment WHEN adding it THEN fails and saves nothing`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = addComment(body = "   \n ")

        // Then
        assertTrue(result.isFailure)
        assertTrue(repository.comments.value.isEmpty())
    }

    @Test
    fun `GIVEN a comment over 1000 letters WHEN adding it THEN fails and saves nothing`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = addComment(body = "a".repeat(1_001))

        // Then
        assertTrue(result.isFailure)
        assertTrue(repository.comments.value.isEmpty())
    }

    @Test
    fun `GIVEN a comment of 1000 letters between spaces WHEN adding it THEN saves the 1000 letters`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = addComment(body = "  " + "a".repeat(1_000) + "  ")

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
    fun `GIVEN the bill is gone from the device WHEN adding a comment THEN fails`() = runTest {
        // Given
        prepareScenario(addError = IllegalStateException("bill gone"))

        // When
        val result = addComment(body = "Ainda vale?")

        // Then
        assertTrue(result.isFailure)
        assertTrue(repository.comments.value.isEmpty())
    }

    private suspend fun addComment(body: String): Result<Unit> = useCase(
        expenseId = BILL_ID,
        groupId = GROUP_ID,
        body = body,
        threadId = null,
        replyToName = null,
        authorMemberId = "member-ana",
        authorName = "Ana",
    )

    private fun prepareScenario(
        now: Long = 1_000L,
        addError: Throwable? = null,
    ) {
        repository = FakeBillCommentRepository().also { it.addError = addError }
        useCase = AddBillCommentUseCase(
            repository = repository,
            currentTimeMillis = { now },
        )
    }

    private companion object {
        const val BILL_ID = "bill-1"
        const val GROUP_ID = "group-1"
    }
}
