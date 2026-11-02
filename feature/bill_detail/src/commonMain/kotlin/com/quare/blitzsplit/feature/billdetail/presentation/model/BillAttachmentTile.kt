package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay

// Why: one of "Anexos" and a page of the viewer: "Foto 2" with "Ana · ontem", or the receipt with its "Cupom" badge.
@Suppress("ArrayInDataClass")
data class BillAttachmentTile(
    val id: String,
    val isReceipt: Boolean,
    /*
     * Why: the JPEG added on this device, the same instance on every state, so each UI decodes it once by `id`; it
     * wins over `imageUrl`. Bytes rather than a data URI, which Swift would copy as a string on every read.
     */
    val localImage: ByteArray?,
    // Why: the signed URL of an image in the bucket; null offline or until it is signed.
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
