package com.quare.blitzsplit.core.expenses.domain.model

// Why: a photo or the receipt of a bill, as added on a device or as the server stamped it.
data class BillAttachment(
    val id: String,
    val expenseId: String,
    val groupId: String,
    val kind: BillAttachmentKind,
    val width: Int?,
    val height: Int?,
    val addedByUserId: String?,
    val addedByMemberId: String?,
    val addedAt: Long,
    val updatedAt: Long,
    // Why: added or taken off here and not up yet ("Na fila").
    val isPending: Boolean,
    // Why: the image added here, until it is in the bucket; the others are read through signed URLs.
    val pendingImage: PendingJpeg?,
) {
    // Why: where the image lives in the `bill-attachments` bucket.
    val storagePath: String
        get() = createStoragePath(groupId, expenseId, id)

    companion object {
        fun createStoragePath(
            groupId: String,
            expenseId: String,
            id: String,
        ): String = "$groupId/$expenseId/$id.jpg"
    }
}
