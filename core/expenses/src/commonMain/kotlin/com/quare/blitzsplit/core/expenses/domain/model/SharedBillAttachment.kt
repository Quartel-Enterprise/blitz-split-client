package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.PhotoFrame

// Why: a photo or the receipt of a shared bill, read through a signed URL anyone with an open link may get.
data class SharedBillAttachment(
    val id: String,
    val kind: BillAttachmentKind,
    val width: Int?,
    val height: Int?,
    // Why: null once they left the group.
    val addedByName: String?,
    val addedAt: Long,
    // Why: null when the bucket couldn't sign it.
    val imageUrl: String?,
    // Why: the square its tile shows when the image is the whole photo.
    val frame: PhotoFrame?,
)
