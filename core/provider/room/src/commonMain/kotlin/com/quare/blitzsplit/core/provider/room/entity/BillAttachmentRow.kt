package com.quare.blitzsplit.core.provider.room.entity

/*
 * Why: a bill's attachment as Conta draws it, without its JPEG: a change to any attachment runs the query again, and
 * reading up to 5 images of 1.5 MB each time would cost more than drawing them. The image loads apart, once.
 */
data class BillAttachmentRow(
    val id: String,
    val expenseId: String,
    val groupId: String,
    val kind: String,
    val width: Int?,
    val height: Int?,
    val addedByUserId: String?,
    val addedByMemberId: String?,
    val addedAt: Long,
    val updatedAt: Long,
    val pendingSync: Boolean,
    val hasPendingJpeg: Boolean,
)
