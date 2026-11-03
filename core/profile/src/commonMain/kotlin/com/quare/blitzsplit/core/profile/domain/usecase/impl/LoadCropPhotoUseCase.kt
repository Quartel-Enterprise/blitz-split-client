package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.profile.domain.model.PhotoTooLargeException
import com.quare.blitzsplit.core.profile.domain.usecase.LoadCropPhoto
import com.quare.blitzsplit.core.provider.image.BillImageEncoder
import com.quare.blitzsplit.core.utils.suspendRunCatching

// Why: a bill's photo is already the upright, at most 1600 px JPEG the editor needs, and a 512 avatar crops well in it.
class LoadCropPhotoUseCase(
    private val encodeImage: BillImageEncoder,
) : LoadCropPhoto {
    override suspend fun invoke(source: ByteArray): Result<CropPhoto> = suspendRunCatching {
        if (source.size > MAX_SOURCE_BYTES) throw PhotoTooLargeException()
        val image = encodeImage(source)
        CropPhoto(
            jpeg = image.jpeg,
            width = image.width,
            height = image.height,
        )
    }

    private companion object {
        // Why: past this a phone photo is not what was picked (a video, a RAW), and decoding it costs too much memory.
        const val MAX_SOURCE_BYTES = 30 * 1024 * 1024
    }
}
