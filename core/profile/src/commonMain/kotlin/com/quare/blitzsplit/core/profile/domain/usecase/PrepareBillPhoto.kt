package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.provider.image.BillImage

/*
 * Why: a bill's photo keeps the square framed in "Ajustar foto" (Anexos' tile), cut from the editor's photo (at most 1600
 * px) at the pixels it has there, as the `bill-attachments` bucket takes it. Fails with UnsupportedImageException or ImageTooLargeException as a pick would.
 */
fun interface PrepareBillPhoto {
    suspend operator fun invoke(
        photo: CropPhoto,
        crop: PhotoCrop,
    ): Result<BillImage>
}
