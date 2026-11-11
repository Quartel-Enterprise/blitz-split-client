package com.quare.blitzsplit.feature.editprofile.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.PhotoCropEditor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.utils.toAppLanguageUppercase

data class ProfilePhotoUiState(
    // Why: the saved profile was read; until then the viewer draws no avatar, never "?" on the default color.
    val isLoaded: Boolean,
    val displayName: String,
    val avatarColor: AvatarColor,
    // Why: the saved photo, which each shortcut replaces at once, with no Salvar.
    val photo: ProfilePhoto?,
    val isPreparingPhoto: Boolean,
    val showsPhotoError: Boolean,
    // Why: "Ajustar foto" while a picked photo is being framed, drawn over the viewer in a circle.
    val photoCrop: PhotoCropEditor?,
    // Why: the Google or Apple account's own picture, offered while it isn't the photo shown.
    val accountProvider: AccountProvider?,
    val accountPhotoUrl: String?,
) {
    val avatarInitial: String
        get() = displayName.trim().firstOrNull()?.toString()?.toAppLanguageUppercase() ?: "?"

    val photoUrl: String?
        get() = photo?.toImageUrl()

    // Why: "Remover" only once there is a photo to remove.
    val hasPhoto: Boolean
        get() = photo != null

    // Why: names whose picture is offered, so the UIs check this one value instead of the URL and the provider.
    val accountPhotoProvider: AccountProvider?
        get() = accountProvider.takeIf { isAccountPhotoOffered(accountPhotoUrl, photo) }
}
