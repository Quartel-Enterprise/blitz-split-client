package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import com.quare.blitzsplit.core.profile.photo.toTransform
import com.quare.blitzsplit.core.provider.image.AVATAR_SIZE_PX
import com.quare.blitzsplit.core.provider.image.AvatarImageEncoder
import com.quare.blitzsplit.core.utils.suspendRunCatching

class PrepareProfilePhotoUseCase(
    private val encodeAvatar: AvatarImageEncoder,
) : PrepareProfilePhoto {
    override suspend fun invoke(
        photo: CropPhoto,
        crop: PhotoCrop,
    ): Result<ProfilePhoto.Pending> = suspendRunCatching {
        ProfilePhoto.Pending(jpeg = encodeAvatar(photo.jpeg, crop.toTransform(AVATAR_SIZE_PX)))
    }
}
