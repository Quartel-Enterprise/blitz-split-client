package com.quare.blitzsplit.core.model

// Why: what the person did in "Ajustar foto", which each screen with a photo hands to its ProfilePhotoPicker (core:profile).
sealed interface PhotoCropEvent {
    // Why: a drag, as fractions of the frame's side, so each UI sends it in its own units.
    data class OnDragged(
        val dx: Float,
        val dy: Float,
    ) : PhotoCropEvent

    /*
     * Why: a pinch (or the trackpad's), as the factor it scaled by since the last one, around the point between the
     * fingers (from the frame's center, in fractions of its side), which stays under them.
     */
    data class OnPinched(
        val zoomChange: Float,
        val focusX: Float,
        val focusY: Float,
    ) : PhotoCropEvent

    // Why: the slider, from PhotoCrop.minZoom to maxZoom.
    data class OnZoomChanged(
        val zoom: Float,
    ) : PhotoCropEvent

    // Why: "Girar", a quarter turn clockwise.
    data object OnTurnClicked : PhotoCropEvent

    data object OnMirrorHorizontallyClicked : PhotoCropEvent

    data object OnMirrorVerticallyClicked : PhotoCropEvent

    // Why: "Pronto".
    data object OnConfirmClicked : PhotoCropEvent

    // Why: "Cancelar", ✕, a swipe or back, which leave the photo shown as it was.
    data object OnDismissed : PhotoCropEvent
}
