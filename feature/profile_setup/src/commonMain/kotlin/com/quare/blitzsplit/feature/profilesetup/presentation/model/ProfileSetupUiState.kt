package com.quare.blitzsplit.feature.profilesetup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.profile.domain.model.DisplayNameRules

data class ProfileSetupUiState(
    val name: String,
    val avatarColor: AvatarColor,
    val photo: ProfilePhoto?,
    val isPreparingPhoto: Boolean,
    val showsPhotoError: Boolean,
    // Why: null while the name is valid, and also while it is still empty, before anything was typed.
    val nameError: NameError?,
    val canContinue: Boolean,
    val isSaving: Boolean,
) {
    val avatarInitial: String
        get() = name.trim().firstOrNull()?.uppercase() ?: "?"

    // Why: the UIs put the limits in their copy, so the rule and the messages can't drift apart.
    val nameMinLength: Int
        get() = DisplayNameRules.MIN_LENGTH

    val nameMaxLength: Int
        get() = DisplayNameRules.MAX_LENGTH

    val avatarColors: List<AvatarColor>
        get() = AvatarColor.entries

    // Why: a picked photo is a ~80 KB data URI, which ProfilePhoto.Pending encodes once for every state copy.
    val photoUrl: String?
        get() = photo?.toImageUrl()
}
