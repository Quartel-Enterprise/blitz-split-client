package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.PendingJpeg
import com.quare.blitzsplit.core.expenses.domain.repository.BillAttachmentRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.AddBillAttachment
import com.quare.blitzsplit.core.provider.image.BillImageEncoder
import com.quare.blitzsplit.core.provider.image.UnsupportedImageException
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlin.uuid.Uuid

// Why: on the device right away, offline included, as a JPEG the bucket takes; the others see it once synced (7.3g).
class AddBillAttachmentUseCase(
    private val repository: BillAttachmentRepository,
    private val encodeImage: BillImageEncoder,
    private val currentTimeMillis: CurrentTimeMillis,
) : AddBillAttachment {
    override suspend fun invoke(
        expenseId: String,
        groupId: String,
        kind: BillAttachmentKind,
        source: ByteArray,
        addedByMemberId: String?,
    ): Result<BillAttachmentAddition> {
        val image = suspendRunCatching { encodeImage(source) }.getOrElse { error ->
            return if (error is UnsupportedImageException) {
                Result.success(BillAttachmentAddition.UNREADABLE)
            } else {
                Result.failure(error)
            }
        }
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
            pendingImage = PendingJpeg(image.jpeg),
        )
        return repository.addAttachment(attachment).map { added ->
            if (added) BillAttachmentAddition.ADDED else BillAttachmentAddition.NO_ROOM
        }
    }
}
