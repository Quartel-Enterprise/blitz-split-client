package com.quare.blitzsplit.core.presentation.analytics

import com.quare.blitzsplit.core.model.PhotoCropEvent

/*
 * Why: "Ajustar foto" is the same editor in every screen with a photo, so it tracks the same events wherever it opens,
 * source naming the screen. A drag, a pinch and the zoom slider are gestures, not taps.
 */
fun PhotoCropEvent.toEventAnalytics(source: String): EventAnalytics = when (this) {
    is PhotoCropEvent.OnDragged,
    is PhotoCropEvent.OnPinched,
    is PhotoCropEvent.OnZoomChanged,
    -> EventAnalytics.NotTracked

    PhotoCropEvent.OnTurnClicked -> EventAnalytics.Automatic("photo_crop_turned", mapOf("source" to source))

    PhotoCropEvent.OnMirrorHorizontallyClicked -> EventAnalytics.Automatic(
        "photo_crop_mirrored",
        mapOf("source" to source, "axis" to "horizontal"),
    )

    PhotoCropEvent.OnMirrorVerticallyClicked -> EventAnalytics.Automatic(
        "photo_crop_mirrored",
        mapOf("source" to source, "axis" to "vertical"),
    )

    PhotoCropEvent.OnConfirmClicked -> EventAnalytics.Automatic("photo_crop_confirmed", mapOf("source" to source))

    PhotoCropEvent.OnDismissed -> EventAnalytics.Automatic("photo_crop_dismissed", mapOf("source" to source))
}
