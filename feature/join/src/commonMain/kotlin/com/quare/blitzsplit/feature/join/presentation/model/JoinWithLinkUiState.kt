package com.quare.blitzsplit.feature.join.presentation.model

data class JoinWithLinkUiState(
    // Why: the link typed or pasted under "Tem o link?".
    val link: String,
    // Why: "Esse link não é um convite do Bora Rachar", after "Abrir convite" or a paste of anything else.
    val isLinkInvalid: Boolean,
    // Why: "Esse QR não é um convite do Bora Rachar" for a few seconds, the viewfinder in the error color.
    val isQrInvalid: Boolean,
    val cameraAccess: CameraAccess,
    val isTorchOn: Boolean,
) {
    // Why: "Abrir convite" shows once there is something in the field.
    val canOpen: Boolean
        get() = link.isNotBlank()
}
