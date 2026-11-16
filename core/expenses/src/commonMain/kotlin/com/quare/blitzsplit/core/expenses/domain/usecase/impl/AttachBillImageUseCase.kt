package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.repository.BillAttachmentRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.AttachBillImage
import com.quare.blitzsplit.core.provider.image.BillImage
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlin.uuid.Uuid

// Why: on the device right away, offline included, as a JPEG the bucket takes; the others see it once synced (7.3g).
class AttachBillImageUseCase(
    private val repository: BillAttachmentRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : AttachBillImage {
    override suspend fun invoke(
        expenseId: String,
        groupId: String,
        kind: BillAttachmentKind,
        image: BillImage,
        addedByMemberId: String?,
    ): Result<BillAttachmentAddition> {
        val now = currentTimeMillis()
        val attachment = BillAttachment(
            id = Uuid.random().toString(),
            expenseId = expenseId,
            groupId = groupId,
            kind = kind,
            width = image.width,
            height = image.height,
            addedByUserId = null,
            addedByMemberId = addedByMemberId,
            addedAt = now,
            updatedAt = now,
            isPending = true,
            hasPendingImage = true,
        )
        return repository.addAttachment(attachment, image.jpeg).map { added ->
            if (added) BillAttachmentAddition.ADDED else BillAttachmentAddition.NO_ROOM
        }
    }
}
