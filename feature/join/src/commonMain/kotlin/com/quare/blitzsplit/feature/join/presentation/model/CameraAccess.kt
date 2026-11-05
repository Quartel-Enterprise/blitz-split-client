package com.quare.blitzsplit.feature.join.presentation.model

/*
 * Why: the reader never opens the camera on its own. Until "Open camera" it is NOT_REQUESTED and nothing is asked;
 * then the platform asks (ASKING) and answers. Denied, the reader explains and offers the settings.
 */
enum class CameraAccess {
    NOT_REQUESTED,
    ASKING,
    GRANTED,
    DENIED,
}
