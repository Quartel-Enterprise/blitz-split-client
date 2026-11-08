package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.PhotoCropEditor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.toImageUrl

data class GroupPhotoUiState(
    // Why: the group was read; until then the viewer draws no avatar, never "?" on the default color.
    val isLoaded: Boolean,
    val name: String,
    val initials: String,
    val color: AvatarColor,
    // Why: the saved photo, which each shortcut replaces at once, with no Salvar.
    val photo: ProfilePhoto?,
    val isPreparingPhoto: Boolean,
    val showsPhotoError: Boolean,
    // Why: "Ajustar foto" while a picked photo is being framed, drawn over the viewer in the group's square.
    val photoCrop: PhotoCropEditor?,
) {
    val photoUrl: String?
        get() = photo?.toImageUrl()

    // Why: "Remover" only once there is a photo to remove.
    val hasPhoto: Boolean
        get() = photo != null
}
