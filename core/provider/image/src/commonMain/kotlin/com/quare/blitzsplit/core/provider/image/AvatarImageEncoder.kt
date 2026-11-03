package com.quare.blitzsplit.core.provider.image

/*
 * Why: the photo editor's photo (upright, at most 1600 px, `BillImageEncoder`) becomes the one format the `avatars`
 * and `group-photos` buckets accept: the square the editor framed, drawn through its transform at 512x512, as a JPEG.
 * JPEG has no alpha, so transparent pixels and any corner the transform leaves uncovered land on white.
 */
fun interface AvatarImageEncoder {
    suspend operator fun invoke(
        source: ByteArray,
        transform: CropTransform,
    ): ByteArray
}

const val AVATAR_SIZE_PX = 512
const val AVATAR_JPEG_QUALITY = 80

class UnsupportedImageException : Exception("The picked file is not an image this platform decodes")

internal expect fun createAvatarImageEncoder(): AvatarImageEncoder
