package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.ProfilePhoto

// Why: the picker returns any format at any size, and the profile only keeps a 512x512 JPEG.
fun interface PrepareProfilePhoto {
    suspend operator fun invoke(source: ByteArray): Result<ProfilePhoto.Pending>
}
