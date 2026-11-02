package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RemoveBillAttachmentUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillAttachmentRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RemoveBillAttachmentUseCaseTest {
    private lateinit var repository: FakeBillAttachmentRepository
    private lateinit var useCase: RemoveBillAttachmentUseCase

    @Test
    fun `GIVEN a photo on the bill WHEN removing it THEN takes it off stamped at the current time`() = runTest {
        // Given
        prepareScenario(now = 9_000L)

        // When
        val result = useCase("photo-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("photo-1" to 9_000L), repository.removals)
        assertTrue(repository.attachments.value.isEmpty())
    }

    @Test
    fun `GIVEN the device fails to remove it WHEN removing a photo THEN fails`() = runTest {
        // Given
        prepareScenario(removeError = IllegalStateException("disk"))

        // When
        val result = useCase("photo-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(
            listOf("photo-1"),
            repository.attachments.value.map { it.id },
        )
    }

    private val photo = BillAttachment(
        id = "photo-1",
        expenseId = "bill-1",
        groupId = "group-1",
        kind = BillAttachmentKind.PHOTO,
        width = 800,
        height = 600,
        addedByUserId = "user-ana",
        addedByMemberId = "member-ana",
        addedAt = 1L,
        updatedAt = 1L,
        isPending = false,
        pendingImage = null,
    )

    private fun prepareScenario(
        now: Long = 1_000L,
        removeError: Throwable? = null,
    ) {
        repository = FakeBillAttachmentRepository(listOf(photo)).also { it.removeError = removeError }
        useCase = RemoveBillAttachmentUseCase(
            repository = repository,
            currentTimeMillis = { now },
        )
    }
}
