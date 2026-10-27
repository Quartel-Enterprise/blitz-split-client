package com.quare.blitzsplit.core.provider.image

import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

internal actual fun createAvatarImageEncoder(): AvatarImageEncoder = BrowserAvatarImageEncoder()

/*
 * Why: the browser decodes every format it shows (HEIC only on Safari) and createImageBitmap
 * applies the EXIF rotation, so a canvas does the crop and the JPEG encoding.
 */
internal class BrowserAvatarImageEncoder : AvatarImageEncoder {
    override suspend fun invoke(source: ByteArray): ByteArray {
        val global: dynamic = js("globalThis")
        val blob: dynamic = js("new Blob([source])")
        val bitmap: dynamic = suspendRunCatching {
            global.createImageBitmap(blob).unsafeCast<Promise<dynamic>>().await<dynamic>()
        }.getOrElse { throw UnsupportedImageException() }
        val width = bitmap.width.unsafeCast<Int>()
        val height = bitmap.height.unsafeCast<Int>()
        val side = minOf(width, height)
        val canvas: dynamic = global.document.createElement("canvas")
        canvas.width = AVATAR_SIZE_PX
        canvas.height = AVATAR_SIZE_PX
        val context: dynamic = canvas.getContext("2d")
        context.imageSmoothingQuality = "high"
        context.fillStyle = BACKGROUND_COLOR
        context.fillRect(0, 0, AVATAR_SIZE_PX, AVATAR_SIZE_PX)
        context.drawImage(
            bitmap,
            (width - side) / 2,
            (height - side) / 2,
            side,
            side,
            0,
            0,
            AVATAR_SIZE_PX,
            AVATAR_SIZE_PX,
        )
        bitmap.close()
        val jpeg: dynamic = toJpegBlob(canvas)
        val buffer: dynamic = jpeg.arrayBuffer().unsafeCast<Promise<dynamic>>().await()
        return js("new Int8Array(buffer)").unsafeCast<ByteArray>()
    }

    private suspend fun toJpegBlob(canvas: dynamic): dynamic = suspendCoroutine { continuation ->
        canvas.toBlob(
            { blob: dynamic ->
                if (blob == null) {
                    continuation.resumeWithException(UnsupportedImageException())
                } else {
                    continuation.resume(blob)
                }
            },
            "image/jpeg",
            AVATAR_JPEG_QUALITY / PERCENT,
        )
    }

    private companion object {
        const val PERCENT = 100.0
        const val BACKGROUND_COLOR = "#FFFFFF"
    }
}
