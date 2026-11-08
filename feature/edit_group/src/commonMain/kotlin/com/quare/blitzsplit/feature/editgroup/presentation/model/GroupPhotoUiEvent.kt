package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.toEventAnalytics

// Why: the screen these events come from, as their source param says, for its UiEvent and its ViewModel alike.
internal const val GROUP_PHOTO_SOURCE = "group_photo"

sealed interface GroupPhotoUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: an item the picker handed back that the UI couldn't read, or a close, which nobody tapped to act.
            OnPhotoUnreadable,
            OnDismissed,
            -> EventAnalytics.NotTracked

            // Why: a photo handed back by the camera or the gallery, which the viewer's shortcuts open straight away.
            is OnPhotoPicked -> EventAnalytics.Automatic("photo_picked", mapOf("source" to GROUP_PHOTO_SOURCE))

            is OnPhotoCrop -> event.toEventAnalytics(GROUP_PHOTO_SOURCE)

            // Why: only while there is a photo, which a late second tap no longer finds.
            OnPhotoRemoved -> EventAnalytics.Manual("photo_removed")
        }

    // Why: "Tirar foto" and "Galeria" open each platform's own camera or picker, which hands over the file it returned.
    class OnPhotoPicked(
        val source: ByteArray,
    ) : GroupPhotoUiEvent

    // Why: what was done in "Ajustar foto", which a picked photo opens in over the viewer.
    data class OnPhotoCrop(
        val event: PhotoCropEvent,
    ) : GroupPhotoUiEvent

    data object OnPhotoUnreadable : GroupPhotoUiEvent

    data object OnPhotoRemoved : GroupPhotoUiEvent

    // Why: ✕, a tap outside the photo, Esc and back.
    data object OnDismissed : GroupPhotoUiEvent
}
