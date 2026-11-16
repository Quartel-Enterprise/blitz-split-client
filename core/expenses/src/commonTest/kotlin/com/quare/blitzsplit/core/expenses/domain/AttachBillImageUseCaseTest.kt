package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.AttachBillImageUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillAttachmentRepository
import com.quare.blitzsplit.core.provider.image.BillImage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class AttachBillImageUseCaseTest {
    private lateinit var repository: FakeBillAttachmentRepository
    private lateinit var useCase: AttachBillImageUseCase

    @Test
    fun `GIVEN a framed photo WHEN attaching it THEN stores its JPEG pending at the current time with no account`() =
        runTest {
            // Given
            prepareScenario(now = 7_000L)

            // When
            val result = addAttachment(kind = BillAttachmentKind.PHOTO)

            // Then
            assertEquals(BillAttachmentAddition.ADDED, result.getOrNull())
            val saved = repository.attachments.value.single()
            assertEquals(
                BillAttachment(
                    id = saved.id,
                    expenseId = BILL_ID,
                    groupId = GROUP_ID,
                    kind = BillAttachmentKind.PHOTO,
                    width = 1_600,
                    height = 1_200,
                    addedByUserId = null,
                    addedByMemberId = "member-ana",
                    addedAt = 7_000L,
                    updatedAt = 7_000L,
                    isPending = true,
                    hasPendingImage = true,
                ),
                saved,
            )
            assertTrue(encodedJpeg.contentEquals(repository.pendingImages.getValue(saved.id)))
        }

    @Test
    fun `GIVEN a receipt image already encoded WHEN attaching it THEN stores it as it is`() = runTest {
        // Given
        prepareScenario(now = 7_000L)
        val readJpeg = byteArrayOf(5, 6, 7)

        // When
        val result = useCase(
            expenseId = BILL_ID,
            groupId = GROUP_ID,
            kind = BillAttachmentKind.RECEIPT,
            image = BillImage(
                jpeg = readJpeg,
                width = 1_200,
                height = 1_600,
            ),
            addedByMemberId = "member-ana",
        )

        // Then
        assertEquals(BillAttachmentAddition.ADDED, result.getOrNull())
        val saved = repository.attachments.value.single()
        assertEquals(BillAttachmentKind.RECEIPT, saved.kind)
        assertEquals(1_200, saved.width)
        assertEquals(1_600, saved.height)
        assertTrue(readJpeg.contentEquals(repository.pendingImages.getValue(saved.id)))
    }

    @Test
    fun `GIVEN a framed photo WHEN attaching it THEN its id is a new uuid`() = runTest {
        // Given
        prepareScenario()

        // When
        addAttachment(kind = BillAttachmentKind.PHOTO)

        // Then
        val id = repository.attachments.value
            .single()
            .id
        assertEquals(id, Uuid.parse(id).toString())
    }

    @Test
    fun `GIVEN a photo already added WHEN adding another THEN each gets its own id`() = runTest {
        // Given
        prepareScenario()
        addAttachment(kind = BillAttachmentKind.PHOTO)

        // When
        addAttachment(kind = BillAttachmentKind.PHOTO)

        // Then
        val ids = repository.attachments.value.map { it.id }
        assertEquals(2, ids.size)
        assertNotEquals(ids[0], ids[1])
    }

    @Test
    fun `GIVEN a picked receipt WHEN adding it THEN stores it as the receipt`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = addAttachment(kind = BillAttachmentKind.RECEIPT)

        // Then
        assertEquals(BillAttachmentAddition.ADDED, result.getOrNull())
        assertEquals(
            BillAttachmentKind.RECEIPT,
            repository.attachments.value
                .single()
                .kind,
        )
    }

    @Test
    fun `GIVEN no member of this device WHEN adding a photo THEN stores it with nobody as who added it`() = runTest {
        // Given
        prepareScenario()

        // When
        addAttachment(
            kind = BillAttachmentKind.PHOTO,
            addedByMemberId = null,
        )

        // Then
        val saved = repository.attachments.value.single()
        assertEquals(null to null, saved.addedByMemberId to saved.addedByUserId)
    }

    @Test
    fun `GIVEN the bill has no room left WHEN adding a photo THEN says so and stores nothing`() = runTest {
        // Given
        prepareScenario(hasRoom = false)

        // When
        val result = addAttachment(kind = BillAttachmentKind.PHOTO)

        // Then
        assertEquals(BillAttachmentAddition.NO_ROOM, result.getOrNull())
        assertTrue(repository.attachments.value.isEmpty())
    }

    @Test
    fun `GIVEN the device fails to store it WHEN adding a photo THEN fails`() = runTest {
        // Given
        prepareScenario(addError = IllegalStateException("No bill bill-1"))

        // When
        val result = addAttachment(kind = BillAttachmentKind.PHOTO)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(repository.attachments.value.isEmpty())
    }

    private val encodedJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 9)

    private suspend fun addAttachment(
        kind: BillAttachmentKind,
        addedByMemberId: String? = "member-ana",
    ): Result<BillAttachmentAddition> = useCase(
        expenseId = BILL_ID,
        groupId = GROUP_ID,
        kind = kind,
        image = BillImage(
            jpeg = encodedJpeg,
            width = 1_600,
            height = 1_200,
        ),
        addedByMemberId = addedByMemberId,
    )

    private fun prepareScenario(
        now: Long = 1_000L,
        hasRoom: Boolean = true,
        addError: Throwable? = null,
    ) {
        repository = FakeBillAttachmentRepository().also {
            it.hasRoom = hasRoom
            it.addError = addError
        }
        useCase = AttachBillImageUseCase(
            repository = repository,
            currentTimeMillis = { now },
        )
    }

    private companion object {
        const val BILL_ID = "bill-1"
        const val GROUP_ID = "group-1"
    }
}
