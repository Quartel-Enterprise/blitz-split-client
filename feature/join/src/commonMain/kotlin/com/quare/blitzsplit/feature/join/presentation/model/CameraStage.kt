package com.quare.blitzsplit.feature.join.presentation.model

// Why: what the reader's camera area shows, so every platform draws the same one.
enum class CameraStage {
    // Why: "Scan the invite's QR code" with "Open camera": the camera is off and nothing was asked yet.
    PROMPT,

    // Why: the device has no camera: no button, only the link below.
    NO_CAMERA,

    // Why: the viewfinder, over the camera once it is allowed (over black while the system asks).
    VIEWFINDER,

    // Why: "No access to the camera" with the way to the settings.
    DENIED,

    // Why: allowed, but the camera won't open (another app holds it).
    FAILED,
}
