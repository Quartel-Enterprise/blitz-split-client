package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.LoadBillAttachmentUrlsUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillAttachmentRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LoadBillAttachmentUrlsUseCaseTest {
    private lateinit var repository: FakeBillAttachmentRepository
    private lateinit var useCase: LoadBillAttachmentUrlsUseCase

    @Test
    fun `GIVEN photos already up WHEN loading their URLs THEN answers each photo URL by its id`() = runTest {
        // Given
        prepareScenario(imageUrls = mapOf("a-1" to "https://signed/a-1", "a-2" to "https://signed/a-2"))
        val attachments = listOf(attachment("a-1"), attachment("a-2"))

        // When
        val result = useCase(attachments)

        // Then
        assertEquals(
            mapOf("a-1" to "https://signed/a-1", "a-2" to "https://signed/a-2"),
            result.getOrNull(),
        )
        assertEquals(listOf(listOf("a-1", "a-2")), repository.urlRequests)
    }

    @Test
    fun `GIVEN no internet WHEN loading the URLs THEN fails`() = runTest {
        // Given
        prepareScenario(urlsError = IllegalStateException("offline"))
        val attachments = listOf(attachment("a-1"))

        // When
        val result = useCase(attachments)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun attachment(id: String): BillAttachment = BillAttachment(
        id = id,
        expenseId = "bill-1",
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

    private fun prepareScenario(
        imageUrls: Map<String, String> = emptyMap(),
        urlsError: Throwable? = null,
    ) {
        repository = FakeBillAttachmentRepository().also {
            it.imageUrls = imageUrls
            it.urlsError = urlsError
        }
        useCase = LoadBillAttachmentUrlsUseCase(repository)
    }
}
