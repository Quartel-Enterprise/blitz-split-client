package com.quare.blitzsplit.feature.join.presentation.model

data class JoinWithLinkUiState(
    // Why: the link typed or pasted under "Tem o link?".
    val link: String,
    // Why: "Esse link não é um convite do Bora Rachar", after "Abrir convite" or a paste of anything else.
    val isLinkInvalid: Boolean,
    // Why: "Esse QR não é um convite do Bora Rachar" for a few seconds, the viewfinder in the error color.
    val isQrInvalid: Boolean,
    val cameraAccess: CameraAccess,
    // Why: what the platform reports; until it answers the reader assumes one, so "Open camera" never blinks in.
    val hasCamera: Boolean,
    // Why: allowed, but the camera won't open (another app holds it): only the paste is left.
    val isCameraUnavailable: Boolean,
    val hasTorch: Boolean,
    val isTorchOn: Boolean,
) {
    // Why: the flashlight button, while the camera is open and has one; never once the device is left without a camera.
    val showsTorch: Boolean
        get() = hasTorch && showsCameraPreview && !isCameraUnavailable

    // Why: the camera area: the prompt until "Open camera", then the viewfinder or why the camera isn't there.
    val cameraStage: CameraStage
        get() = when {
            !hasCamera -> CameraStage.NO_CAMERA
            cameraAccess == CameraAccess.NOT_REQUESTED -> CameraStage.PROMPT
            cameraAccess == CameraAccess.DENIED -> CameraStage.DENIED
            isCameraUnavailable -> CameraStage.FAILED
            else -> CameraStage.VIEWFINDER
        }

    // Why: the camera runs only once "Open camera" was tapped, and keeps running while the reader is open.
    val isCameraRequested: Boolean
        get() = hasCamera && cameraAccess != CameraAccess.NOT_REQUESTED

    // Why: the native camera view runs only once allowed, and never on a device left without a camera.
    val showsCameraPreview: Boolean
        get() = hasCamera && cameraAccess == CameraAccess.GRANTED

    // Why: "Abrir convite" shows once there is something in the field.
    val canOpen: Boolean
        get() = link.isNotBlank()

    // Why: "Colar" while the field is empty, "Limpar" once it has something.
    val showsPaste: Boolean
        get() = link.isEmpty()
}
