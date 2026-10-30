package com.quare.blitzsplit.feature.join.presentation.model

data class JoinWithLinkUiState(
    // Why: the link typed or pasted under "Tem o link?".
    val link: String,
    // Why: "Esse link não é um convite do Bora Rachar", after "Abrir convite" or a paste of anything else.
    val isLinkInvalid: Boolean,
    // Why: "Esse QR não é um convite do Bora Rachar" for a few seconds, the viewfinder in the error color.
    val isQrInvalid: Boolean,
    val cameraAccess: CameraAccess,
    // Why: allowed, but there is no camera or it won't open (another app holds it): only the paste is left.
    val isCameraUnavailable: Boolean,
    val hasTorch: Boolean,
    val isTorchOn: Boolean,
) {
    // Why: the flashlight button, while the camera is open and has one.
    val showsTorch: Boolean
        get() = hasTorch && cameraAccess == CameraAccess.GRANTED && !isCameraUnavailable

    // Why: "Abrir convite" shows once there is something in the field.
    val canOpen: Boolean
        get() = link.isNotBlank()

    // Why: "Colar" while the field is empty, "Limpar" once it has something.
    val showsPaste: Boolean
        get() = link.isEmpty()
}
