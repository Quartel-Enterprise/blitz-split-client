package com.quare.blitzsplit.core.provider.image

/*
 * Why: whatever the picker returns (HEIC, PNG, a 12 MP JPEG with EXIF rotation) becomes the one
 * format the `avatars` bucket accepts: the centered square, upright, at 512x512, as a JPEG.
 * JPEG has no alpha, so transparent pixels land on white instead of black.
 */
fun interface AvatarImageEncoder {
    suspend operator fun invoke(source: ByteArray): ByteArray
}

const val AVATAR_SIZE_PX = 512
const val AVATAR_JPEG_QUALITY = 80

class UnsupportedImageException : Exception("The picked file is not an image this platform decodes")

internal expect fun createAvatarImageEncoder(): AvatarImageEncoder
