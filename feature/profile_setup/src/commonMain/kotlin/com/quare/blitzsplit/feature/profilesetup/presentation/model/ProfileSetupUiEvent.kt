package com.quare.blitzsplit.feature.profilesetup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface ProfileSetupUiEvent : UiEvent {
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

    // Why: the picker returned an item the UI couldn't read, such as a cloud-only photo while offline.
    data object OnPhotoUnreadable : ProfileSetupUiEvent

    data object OnContinueClicked : ProfileSetupUiEvent

    data object OnBackClicked : ProfileSetupUiEvent
}
