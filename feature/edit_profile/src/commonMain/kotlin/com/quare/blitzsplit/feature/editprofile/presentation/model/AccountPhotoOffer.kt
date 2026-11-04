package com.quare.blitzsplit.feature.editprofile.presentation.model

import com.quare.blitzsplit.core.model.ProfilePhoto

// Why: "Editar perfil" and the photo viewer offer the account's picture once it was read, while it isn't the photo.
internal fun isAccountPhotoOffered(
    accountPhotoUrl: String?,
    photo: ProfilePhoto?,
): Boolean = accountPhotoUrl != null && photo != ProfilePhoto.Remote(accountPhotoUrl)
