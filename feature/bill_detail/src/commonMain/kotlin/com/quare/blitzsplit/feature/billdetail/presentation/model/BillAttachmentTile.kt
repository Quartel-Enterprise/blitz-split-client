package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay

// Why: one of "Anexos" and a page of the viewer: "Foto 2" with "Ana · ontem", or the receipt with its "Cupom" badge.
data class BillAttachmentTile(
    val id: String,
    val isReceipt: Boolean,
    // Why: the image to draw: the JPEG added here as a data URI until it is up, then a signed URL; null offline.
    val imageUrl: String?,
    val width: Int?,
    val height: Int?,
    // Why: "Foto 2"; null for the receipt.
    val photoNumber: Int?,
    // Why: the member as they are now; null for someone no longer in the group.
    val adderName: String?,
    val isAddedByMe: Boolean,
    val addedAt: Long,
    val addedDay: RelativeDay,
    // Why: "Na fila" until the photo is up.
    val isPending: Boolean,
) {
    // Why: "Remover foto" in the viewer; the receipt that filled the bill stays.
    val canRemove: Boolean
        get() = !isReceipt
}
