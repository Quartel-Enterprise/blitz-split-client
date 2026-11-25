package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.profile.domain.model.PreparedBillPhoto

/*
 * Why: a bill's photo from "Ajustar foto", as the `bill-attachments` bucket takes it, at the pixels the editor's photo
 * (at most 1600 px) has: the whole photo mirrored and turned as framed, with the square its tile shows ("Show the whole
 * photo when opened", on by default), or only that square, as before it. Fails with UnsupportedImageException or
 * ImageTooLargeException as a pick would.
 */
fun interface PrepareBillPhoto {
    suspend operator fun invoke(
        photo: CropPhoto,
        crop: PhotoCrop,
        keepsWholePhoto: Boolean,
    ): Result<PreparedBillPhoto>
}
