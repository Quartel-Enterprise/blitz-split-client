package com.quare.blitzsplit.feature.editprofile.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.profile.domain.model.DisplayNameRules

data class EditProfileUiState(
    val name: String,
    val avatarColor: AvatarColor,
    val photo: ProfilePhoto?,
    val isPreparingPhoto: Boolean,
    val showsPhotoError: Boolean,
    // Why: null while the name is valid, and also while it is empty, until something is typed.
    val nameError: EditProfileNameError?,
    val isNameValid: Boolean,
    // Why: Save and Cancel only matter once something differs from the saved profile.
    val hasChanges: Boolean,
    val isSaving: Boolean,
    // Why: names whose account gave the name and photo, since editing here doesn't change that account.
    val accountProvider: AccountProvider?,
) {
    val avatarInitial: String
        get() = name.trim().firstOrNull()?.uppercase() ?: "?"

    val nameLength: Int
        get() = name.length

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

    val hasPhoto: Boolean
        get() = photo != null

    val canSave: Boolean
        get() = isNameValid && hasChanges && !isSaving && !isPreparingPhoto

    val showsAccountNotice: Boolean
        get() = accountProvider != null
}
