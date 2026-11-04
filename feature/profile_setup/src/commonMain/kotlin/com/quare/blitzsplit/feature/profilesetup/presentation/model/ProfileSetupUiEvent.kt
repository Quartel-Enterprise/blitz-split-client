package com.quare.blitzsplit.feature.profilesetup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent
import com.quare.blitzsplit.core.presentation.analytics.toEventAnalytics

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "profile_setup"

sealed interface ProfileSetupUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            is OnNameChanged -> EventAnalytics.NotTracked

            is OnAvatarColorSelected -> EventAnalytics.Automatic(
                "avatar_color_selected",
                mapOf("color" to color, "source" to SOURCE),
            )

            is OnPhotoPicked -> EventAnalytics.Automatic("photo_picked", mapOf("source" to SOURCE))

            is OnPhotoCrop -> event.toEventAnalytics(SOURCE)

            // Why: the picker handing back an item the UI couldn't read, not a tap.
            OnPhotoUnreadable -> EventAnalytics.NotTracked

            OnContinueClicked -> EventAnalytics.Automatic("profile_setup_continue_clicked")

            OnBackClicked -> EventAnalytics.Automatic("back_clicked", mapOf("source" to SOURCE))
        }

    data class OnNameChanged(
        val name: String,
    ) : ProfileSetupUiEvent

    data class OnAvatarColorSelected(
        val color: AvatarColor,
    ) : ProfileSetupUiEvent

    /*
     * Why: each UI opens its own picker straight from the tap (a browser only opens a file picker
     * inside the click), and hands over the file it returned, in whatever format.
     */
    class OnPhotoPicked(
        val source: ByteArray,
    ) : ProfileSetupUiEvent

    // Why: what was done in "Ajustar foto", the editor a picked photo opens in.
    data class OnPhotoCrop(
        val event: PhotoCropEvent,
    ) : ProfileSetupUiEvent

    // Why: the picker returned an item the UI couldn't read, such as a cloud-only photo while offline.
    data object OnPhotoUnreadable : ProfileSetupUiEvent

    data object OnContinueClicked : ProfileSetupUiEvent

    data object OnBackClicked : ProfileSetupUiEvent
}
