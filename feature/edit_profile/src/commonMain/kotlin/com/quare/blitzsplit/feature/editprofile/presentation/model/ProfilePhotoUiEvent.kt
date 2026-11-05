package com.quare.blitzsplit.feature.editprofile.presentation.model

import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.toEventAnalytics

// Why: the screen these events come from, as their source param says, for its UiEvent and its ViewModel alike.
internal const val PROFILE_PHOTO_SOURCE = "profile_photo"

sealed interface ProfilePhotoUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: an item the picker handed back that the UI couldn't read, or a close, which nobody tapped to act.
            OnPhotoUnreadable,
            OnDismissed,
            -> EventAnalytics.NotTracked

            // Why: a photo handed back by the camera or the gallery, which the viewer's shortcuts open straight away.
            is OnPhotoPicked -> EventAnalytics.Automatic("photo_picked", mapOf("source" to PROFILE_PHOTO_SOURCE))

            is OnPhotoCrop -> event.toEventAnalytics(PROFILE_PHOTO_SOURCE)

            // Why: only a photo the viewer takes: the account's while it has one, and never while another is saving.
            OnPhotoRemoved -> EventAnalytics.Manual("photo_removed")

            OnAccountPhotoSelected -> EventAnalytics.Manual("account_photo_selected")
        }

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
