package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.profile.domain.model.PhotoTooLargeException
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import com.quare.blitzsplit.core.provider.image.AvatarImageEncoder
import com.quare.blitzsplit.core.utils.suspendRunCatching

class PrepareProfilePhotoUseCase(
    private val encodeAvatar: AvatarImageEncoder,
) : PrepareProfilePhoto {
    override suspend fun invoke(source: ByteArray): Result<ProfilePhoto.Pending> = suspendRunCatching {
        if (source.size > MAX_SOURCE_BYTES) throw PhotoTooLargeException()
        ProfilePhoto.Pending(jpeg = encodeAvatar(source))
    }

    private companion object {
        // Why: past this a phone photo is not what was picked (a video, a RAW), and decoding it costs too much memory.
        const val MAX_SOURCE_BYTES = 30 * 1024 * 1024
    }
}
