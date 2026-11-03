package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.CropPhoto

// Why: the picker returns any format at any size, and the photo editor draws it upright and small enough to move.
fun interface LoadCropPhoto {
    suspend operator fun invoke(source: ByteArray): Result<CropPhoto>
}
