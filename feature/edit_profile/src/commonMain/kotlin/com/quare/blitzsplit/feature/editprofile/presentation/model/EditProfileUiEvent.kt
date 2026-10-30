package com.quare.blitzsplit.feature.editprofile.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface EditProfileUiEvent : UiEvent {
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

    // Why: the picker returned an item the UI couldn't read, such as a cloud-only photo while offline.
    data object OnPhotoUnreadable : EditProfileUiEvent

    // Why: the Google or Apple account's own picture, kept as the photo without being uploaded.
    data object OnAccountPhotoSelected : EditProfileUiEvent

    // Why: without a photo the avatar shows the initial on its color.
    data object OnPhotoRemoved : EditProfileUiEvent

    data object OnSaveClicked : EditProfileUiEvent

    // Why: the web and the Mac edit inside the profile, where Cancel puts back what was saved.
    data object OnDiscardClicked : EditProfileUiEvent

    data object OnBackClicked : EditProfileUiEvent
}
