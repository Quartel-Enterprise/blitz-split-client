package com.quare.blitzsplit.feature.join.presentation.model

sealed interface JoinWithLinkUiAction {
    // Why: the app's page in the device's settings, where the camera can be allowed.
    data object OpenCameraSettings : JoinWithLinkUiAction
}
