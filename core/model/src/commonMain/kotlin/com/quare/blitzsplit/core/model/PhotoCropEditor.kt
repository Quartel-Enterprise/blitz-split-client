package com.quare.blitzsplit.core.model

/*
 * Why: "Ajustar foto", the editor a picked or taken photo opens in before it becomes the profile's, the group's or a
 * bill's: the photo and where it sits in the frame. The screen that opened it picks the frame (a profile's circle, a
 * group's rounded square, a bill attachment's rounded square).
 */
data class PhotoCropEditor(
    val photo: CropPhoto,
    val crop: PhotoCrop,
) {
    // Why: a gesture or a button moves the photo in the frame; "Pronto" and "Cancelar" are the screen's to act on.
    fun toEdited(event: PhotoCropEvent): PhotoCropEditor = copy(
        crop = when (event) {
            is PhotoCropEvent.OnDragged -> crop.toDragged(event.dx, event.dy)

            is PhotoCropEvent.OnPinched -> crop.toZoomedBy(event.zoomChange, event.focusX, event.focusY)

            is PhotoCropEvent.OnZoomChanged -> crop.toZoomed(event.zoom)

            PhotoCropEvent.OnTurnClicked -> crop.toTurned()

            PhotoCropEvent.OnMirrorHorizontallyClicked -> crop.toMirroredHorizontally()

            PhotoCropEvent.OnMirrorVerticallyClicked -> crop.toMirroredVertically()

            PhotoCropEvent.OnConfirmClicked,
            PhotoCropEvent.OnDismissed,
            -> crop
        },
    )
}
