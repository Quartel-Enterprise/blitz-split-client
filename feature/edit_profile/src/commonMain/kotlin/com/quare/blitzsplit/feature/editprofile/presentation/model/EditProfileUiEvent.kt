package com.quare.blitzsplit.feature.editprofile.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent
import com.quare.blitzsplit.core.presentation.analytics.toEventAnalytics

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "edit_profile"

sealed interface EditProfileUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            /*
             * Why: a value typed letter by letter (what was typed shows in the profile saved), an item the picker
             * handed back that the UI couldn't read, or a back, which each platform also does its own way.
             */
            is OnNameChanged,
            OnPhotoUnreadable,
            OnBackClicked,
            -> EventAnalytics.NotTracked

            is OnAvatarColorSelected -> EventAnalytics.Automatic(
                "avatar_color_selected",
                mapOf("color" to color, "source" to SOURCE),
            )

            // Why: a photo handed back by the camera or the picker, which each UI opens straight from its tap.
            is OnPhotoPicked -> EventAnalytics.Automatic("photo_picked", mapOf("source" to SOURCE))

            is OnPhotoCrop -> event.toEventAnalytics(SOURCE)

            OnAvatarClicked -> EventAnalytics.Automatic("profile_photo_clicked", mapOf("source" to SOURCE))

            // Why: each counts only when the form takes it (not saving, an account photo there, a saved profile).
            OnAccountPhotoSelected -> EventAnalytics.Manual("account_photo_selected")

            OnPhotoRemoved -> EventAnalytics.Manual("photo_removed")

            OnDiscardClicked -> EventAnalytics.Manual("edit_profile_discard_clicked")

            // Why: the tap, then the profile once it is saved.
            OnSaveClicked -> EventAnalytics.Manual(setOf("edit_profile_save_clicked", "profile_saved"))
        }

    data class OnNameChanged(
        val name: String,
    ) : EditProfileUiEvent

    data class OnAvatarColorSelected(
        val color: AvatarColor,
    ) : EditProfileUiEvent

    // Why: each UI opens its own picker or camera straight from the tap and hands over the file it returned.
    class OnPhotoPicked(
        val source: ByteArray,
    ) : EditProfileUiEvent

    // Why: what was done in "Ajustar foto", the editor a picked photo opens in.
    data class OnPhotoCrop(
        val event: PhotoCropEvent,
    ) : EditProfileUiEvent

    // Why: the picker returned an item the UI couldn't read, such as a cloud-only photo while offline.
    data object OnPhotoUnreadable : EditProfileUiEvent

    // Why: the Google or Apple account's own picture, kept as the photo without being uploaded.
    data object OnAccountPhotoSelected : EditProfileUiEvent

    // Why: without a photo the avatar shows the initial on its color.
    data object OnPhotoRemoved : EditProfileUiEvent

    // Why: the web and the Mac draw the form inside the profile, where the avatar opens the photo big.
    data object OnAvatarClicked : EditProfileUiEvent

    data object OnSaveClicked : EditProfileUiEvent

    // Why: the web and the Mac edit inside the profile, where Cancel puts back what was saved.
    data object OnDiscardClicked : EditProfileUiEvent

    data object OnBackClicked : EditProfileUiEvent
}
