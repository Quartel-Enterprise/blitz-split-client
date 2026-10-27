package com.quare.blitzsplit.feature.profilesetup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.feature.profilesetup.presentation.mapper.toImageUrl

data class ProfileSetupUiState(
    val name: String,
    val avatarColor: AvatarColor,
    val photo: ProfilePhoto?,
    val isPreparingPhoto: Boolean,
    val showsPhotoError: Boolean,
    val showsNameTooShort: Boolean,
    val canContinue: Boolean,
    val isSaving: Boolean,
) {
    val avatarInitial: String
        get() = name.trim().firstOrNull()?.uppercase() ?: "?"

    val avatarColors: List<AvatarColor>
        get() = AvatarColor.entries

    // Why: a picked photo is a ~80 KB data URI, which ProfilePhoto.Pending encodes once for every state copy.
    val photoUrl: String?
        get() = photo?.toImageUrl()
}
