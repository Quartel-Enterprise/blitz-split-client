package com.quare.blitzsplit.feature.join.presentation.model

// Why: the camera is only asked for once the reader opens. Denied, the reader explains and offers the settings.
enum class CameraAccess {
    ASKING,
    GRANTED,
    DENIED,
}
