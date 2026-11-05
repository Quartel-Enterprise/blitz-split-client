package com.quare.blitzsplit.feature.join.presentation.model

sealed interface JoinWithLinkUiAction {
    // Why: "Open camera" tapped: the platform asks for the camera if it must, then answers with OnCameraAccessChanged.
    data object RequestCameraAccess : JoinWithLinkUiAction

    // Why: the app's page in the device's settings, where the camera can be allowed.
    data object OpenCameraSettings : JoinWithLinkUiAction
}
