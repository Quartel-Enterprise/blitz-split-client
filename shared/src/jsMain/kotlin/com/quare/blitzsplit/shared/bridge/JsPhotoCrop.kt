@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.PhotoCropEditor
import com.quare.blitzsplit.core.model.PhotoCropEvent

/*
 * Why: "Ajustar foto" as the web draws it: the upright photo sized in fractions of the frame's side, then mirrored,
 * turned, zoomed and moved, in that order (a CSS transform lists them the other way round).
 */
class JsPhotoCropState(
    // Why: a data URI of the editor's photo, encoded once per pick.
    val photoUrl: String,
    val photoWidthInFrame: Float,
    val photoHeightInFrame: Float,
    val zoom: Float,
    val minZoom: Float,
    val maxZoom: Float,
    val rotationDegrees: Int,
    val isMirrored: Boolean,
    val offsetX: Float,
    val offsetY: Float,
)

internal fun PhotoCropEditor.toJs(): JsPhotoCropState = JsPhotoCropState(
    photoUrl = photo.dataUri,
    photoWidthInFrame = crop.photoWidthInFrame,
    photoHeightInFrame = crop.photoHeightInFrame,
    zoom = crop.zoom,
    minZoom = crop.minZoom,
    maxZoom = crop.maxZoom,
    rotationDegrees = crop.rotationDegrees,
    isMirrored = crop.isMirrored,
    offsetX = crop.offsetX,
    offsetY = crop.offsetY,
)

// Why: what "Ajustar foto" sends, the same from the profile setup, Editar perfil, Novo grupo and Editar grupo.
class PhotoCropPresenter internal constructor(
    private val send: (PhotoCropEvent) -> Unit,
) {
    // Why: the pointer's move as fractions of the frame's side.
    fun drag(
        dx: Float,
        dy: Float,
    ) {
        send(PhotoCropEvent.OnDragged(dx, dy))
    }

    // Why: the wheel or a trackpad's pinch, as the factor it scaled by.
    fun zoomBy(factor: Float) {
        send(PhotoCropEvent.OnPinched(factor))
    }

    fun changeZoom(zoom: Float) {
        send(PhotoCropEvent.OnZoomChanged(zoom))
    }

    fun turn() {
        send(PhotoCropEvent.OnTurnClicked)
    }

    fun mirrorHorizontally() {
        send(PhotoCropEvent.OnMirrorHorizontallyClicked)
    }

    fun mirrorVertically() {
        send(PhotoCropEvent.OnMirrorVerticallyClicked)
    }

    fun confirm() {
        send(PhotoCropEvent.OnConfirmClicked)
    }

    fun dismiss() {
        send(PhotoCropEvent.OnDismissed)
    }
}
