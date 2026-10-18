package com.quare.blitzsplit.feature.profilesetup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

data class ProfileSetupUiState(
    val name: String,
    val avatarColor: AvatarColor,
    val photoUrl: String?,
    val showsNameTooShort: Boolean,
    val canContinue: Boolean,
    val isSaving: Boolean,
) {
    val avatarInitial: String
        get() = name.trim().firstOrNull()?.uppercase() ?: "?"

    val avatarColors: List<AvatarColor>
        get() = AvatarColor.entries
}
