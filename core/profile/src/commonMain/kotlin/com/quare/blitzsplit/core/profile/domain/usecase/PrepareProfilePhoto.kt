package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.model.ProfilePhoto

// Why: a profile and a group only keep a 512x512 JPEG, of the square the photo editor framed.
fun interface PrepareProfilePhoto {
    suspend operator fun invoke(
        photo: CropPhoto,
        crop: PhotoCrop,
    ): Result<ProfilePhoto.Pending>
}
