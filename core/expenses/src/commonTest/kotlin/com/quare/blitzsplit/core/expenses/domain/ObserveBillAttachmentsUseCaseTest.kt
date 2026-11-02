package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveBillAttachmentsUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillAttachmentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveBillAttachmentsUseCaseTest {
    private lateinit var repository: FakeBillAttachmentRepository
    private lateinit var useCase: ObserveBillAttachmentsUseCase

    @Test
    fun `GIVEN photos on two bills WHEN observing one bill THEN shows only its photos`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                attachment(
                    id = "a-1",
                    expenseId = BILL_ID,
                ),
                attachment(
                    id = "a-2",
                    expenseId = "other-bill",
                ),
                attachment(
                    id = "a-3",
                    expenseId = BILL_ID,
                ),
            ),
        )

        // When
        val attachments = useCase(BILL_ID).first()

        // Then
        assertEquals(listOf("a-1", "a-3"), attachments.map { it.id })
    }

    @Test
    fun `GIVEN a bill without photos WHEN observing it THEN shows none`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                attachment(
                    id = "a-1",
                    expenseId = "other-bill",
                ),
            ),
        )

        // When
        val attachments = useCase(BILL_ID).first()

        // Then
        assertTrue(attachments.isEmpty())
    }

    private fun attachment(
        id: String,
        expenseId: String,
    ): BillAttachment = BillAttachment(
        id = id,
        expenseId = expenseId,
        groupId = "group-1",
        kind = BillAttachmentKind.PHOTO,
        width = 800,
        height = 600,
        addedByUserId = "user-1",
        addedByMemberId = "member-1",
        addedAt = 1L,
        updatedAt = 1L,
        isPending = false,
        pendingImage = null,
    )

    private fun prepareScenario(stored: List<BillAttachment>) {
        repository = FakeBillAttachmentRepository(stored)
        useCase = ObserveBillAttachmentsUseCase(repository)
    }

    private companion object {
        const val BILL_ID = "bill-1"
    }
}
