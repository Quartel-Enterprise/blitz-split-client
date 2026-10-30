package com.quare.blitzsplit.feature.join.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface JoinWithLinkUiEvent : UiEvent {
    // Why: the platform's answer to the camera permission, asked when the reader opens and again on coming back.
    data class OnCameraAccessChanged(
        val isGranted: Boolean,
    ) : JoinWithLinkUiEvent

    // Why: the platform's camera opened (again, after another app let it go), with or without a flashlight.
    data class OnCameraOpened(
        val hasTorch: Boolean,
    ) : JoinWithLinkUiEvent

    // Why: no camera, or one that won't open; only the paste is left until it opens.
    data object OnCameraUnavailable : JoinWithLinkUiEvent

    // Why: "Abrir configurações" ("Abrir Ajustes" on the iPhone), with the camera denied.
    data object OnOpenSettingsClicked : JoinWithLinkUiEvent

    data object OnTorchClicked : JoinWithLinkUiEvent

    data class OnQrScanned(
        val content: String,
    ) : JoinWithLinkUiEvent

    data class OnLinkChanged(
        val link: String,
    ) : JoinWithLinkUiEvent

    // Why: "Colar", with what the clipboard had: an invite opens straight away.
    data class OnPasted(
        val text: String,
    ) : JoinWithLinkUiEvent

    data object OnClearClicked : JoinWithLinkUiEvent

    // Why: "Abrir convite", for a link typed by hand.
    data object OnOpenClicked : JoinWithLinkUiEvent

    data object OnCloseClicked : JoinWithLinkUiEvent
}
