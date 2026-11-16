package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareBillPhoto
import com.quare.blitzsplit.core.profile.photo.toTransform
import com.quare.blitzsplit.core.provider.image.BILL_IMAGE_MAX_SIDE_PX
import com.quare.blitzsplit.core.provider.image.BillCropEncoder
import com.quare.blitzsplit.core.provider.image.BillImage
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlin.math.min
import kotlin.math.roundToInt

class PrepareBillPhotoUseCase(
    private val encodeCrop: BillCropEncoder,
) : PrepareBillPhoto {
    override suspend fun invoke(
        photo: CropPhoto,
        crop: PhotoCrop,
    ): Result<BillImage> = suspendRunCatching {
        val side = sideOf(crop)
        encodeCrop(photo.jpeg, crop.toTransform(side), side)
    }

    // Why: as many pixels as the framed square holds in the photo, so zooming in never blows a photo up.
    private fun sideOf(crop: PhotoCrop): Int =
        (min(crop.photoWidth, crop.photoHeight) / crop.zoom).roundToInt().coerceIn(1, BILL_IMAGE_MAX_SIDE_PX)
}
