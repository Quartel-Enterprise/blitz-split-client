@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.feature.billdetail.presentation.model.BillAttachmentTile

/*
 * Why: one of "Anexos": localImage (an Int8Array, the same one on every state, so the page makes one object URL per
 * id) for a photo added here, else imageUrl, the signed URL, null offline or until it is signed.
 */
class JsBillAttachment(
    val id: String,
    val isReceipt: Boolean,
    val localImage: ByteArray?,
    val imageUrl: String?,
    // Why: online, its URL is still being signed: the tile shimmers, as it does while the image downloads.
    val isImageLoading: Boolean,
    val width: Int?,
    val height: Int?,
    // Why: a photo kept whole, drawn so its tile shows the square framed; null fills the tile with its center.
    val thumbnailPlacement: JsPhotoFramePlacement?,
    // Why: "Foto 2"; null for the receipt.
    val photoNumber: Int?,
    // Why: null for someone no longer in the group.
    val adderName: String?,
    val isAddedByMe: Boolean,
    val addedAt: Double,
    // Why: "today", "yesterday" or "other", when the page writes the time.
    val addedDay: String,
    val isPending: Boolean,
    val canRemove: Boolean,
)

// Why: the image in a tile whose side is 1: its size and its top left corner, which may sit outside the tile.
class JsPhotoFramePlacement(
    val width: Double,
    val height: Double,
    val x: Double,
    val y: Double,
)

internal fun toJsBillAttachment(tile: BillAttachmentTile): JsBillAttachment = JsBillAttachment(
    id = tile.id,
    isReceipt = tile.isReceipt,
    localImage = tile.localImage,
    imageUrl = tile.imageUrl,
    isImageLoading = tile.isImageLoading,
    width = tile.width,
    height = tile.height,
    thumbnailPlacement = tile.thumbnailPlacement?.let { placement ->
        JsPhotoFramePlacement(
            width = placement.width.toDouble(),
            height = placement.height.toDouble(),
            x = placement.x.toDouble(),
            y = placement.y.toDouble(),
        )
    },
    photoNumber = tile.photoNumber,
    adderName = tile.adderName,
    isAddedByMe = tile.isAddedByMe,
    addedAt = tile.addedAt.toDouble(),
    addedDay = toJsRelativeDay(tile.addedDay),
    isPending = tile.isPending,
    canRemove = tile.canRemove,
)
