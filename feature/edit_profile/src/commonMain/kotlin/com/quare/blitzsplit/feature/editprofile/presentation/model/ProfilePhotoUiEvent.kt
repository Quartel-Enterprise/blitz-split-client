package com.quare.blitzsplit.feature.editprofile.presentation.model

import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface ProfilePhotoUiEvent : UiEvent {
    // Why: "Tirar foto" and "Galeria" open each platform's own camera or picker, which hands over the file it returned.
    class OnPhotoPicked(
        val source: ByteArray,
    ) : ProfilePhotoUiEvent

    // Why: what was done in "Ajustar foto", which a picked photo opens in over the viewer.
    data class OnPhotoCrop(
        val event: PhotoCropEvent,
    ) : ProfilePhotoUiEvent

    data object OnPhotoUnreadable : ProfilePhotoUiEvent

    data object OnAccountPhotoSelected : ProfilePhotoUiEvent

    data object OnPhotoRemoved : ProfilePhotoUiEvent

    // Why: ✕, a tap outside the photo, Esc and back.
    data object OnDismissed : ProfilePhotoUiEvent
}
