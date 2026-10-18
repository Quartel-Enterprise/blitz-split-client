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

    data object OnContinueClicked : ProfileSetupUiEvent

    data object OnBackClicked : ProfileSetupUiEvent
}
