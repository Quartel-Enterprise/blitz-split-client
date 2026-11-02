package com.quare.blitzsplit.feature.billdetail.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillAttachmentTile
import kotlinx.datetime.TimeZone

/*
 * Why: "Anexos" (7.3g) as Room orders them, the receipt first, the photos numbered in the order they were added. Each
 * draws its JPEG added here, else its signed URL, else a placeholder.
 */
internal fun tilesOf(
    attachments: List<BillAttachment>,
    imageUrls: Map<String, String>,
    localImages: Map<String, ByteArray>,
    group: Group,
    nowMillis: Long,
    timeZone: TimeZone,
): List<BillAttachmentTile> {
    var photoNumber = 0
    return attachments.map { attachment ->
        val isReceipt = attachment.kind == BillAttachmentKind.RECEIPT
        val member = attachment.addedByMemberId?.let { id -> group.members.firstOrNull { it.id == id } }
        BillAttachmentTile(
            id = attachment.id,
            isReceipt = isReceipt,
            localImage = localImages[attachment.id],
            imageUrl = imageUrls[attachment.id],
            width = attachment.width,
            height = attachment.height,
            photoNumber = if (isReceipt) null else ++photoNumber,
            adderName = member?.name,
            isAddedByMe = member?.isMe == true,
            addedAt = attachment.addedAt,
            addedDay = SpentDay.relativeDayOf(
                epochMillis = attachment.addedAt,
                nowMillis = nowMillis,
                timeZone = timeZone,
            ),
            isPending = attachment.isPending,
        )
    }
}
