package com.quare.blitzsplit.core.provider.image

import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

private const val PERCENT = 100.0
private const val BACKGROUND_COLOR = "#FFFFFF"

internal actual fun createAvatarImageEncoder(): AvatarImageEncoder = BrowserAvatarImageEncoder()

internal actual fun createBillImageEncoder(): BillImageEncoder = BrowserBillImageEncoder()

/*
 * Why: the browser decodes every format it shows (HEIC only on Safari) and createImageBitmap
 * applies the EXIF rotation, so a canvas does the crop and the JPEG encoding.
 */
internal class BrowserAvatarImageEncoder : AvatarImageEncoder {
    override suspend fun invoke(source: ByteArray): ByteArray {
        val bitmap = decode(source)
        val width = bitmap.width.unsafeCast<Int>()
        val height = bitmap.height.unsafeCast<Int>()
        val side = minOf(width, height)
        val canvas = createOnWhite(
            bitmap = bitmap,
            from = Area(
                x = (width - side) / 2,
                y = (height - side) / 2,
                width = side,
                height = side,
            ),
            width = AVATAR_SIZE_PX,
            height = AVATAR_SIZE_PX,
        )
        return toJpeg(canvas, AVATAR_JPEG_QUALITY)
    }
}

internal class BrowserBillImageEncoder : BillImageEncoder {
    override suspend fun invoke(source: ByteArray): BillImage {
        val bitmap = decode(source)
        val width = bitmap.width.unsafeCast<Int>()
        val height = bitmap.height.unsafeCast<Int>()
        val size = findBillImageSize(width, height)
        val canvas = createOnWhite(
            bitmap = bitmap,
            from = Area(
                x = 0,
                y = 0,
                width = width,
                height = height,
            ),
            width = size.width,
            height = size.height,
        )
        return BillImage(
            jpeg = encodeUnderLimit { quality -> toJpeg(canvas, quality) },
            width = size.width,
            height = size.height,
        )
    }
}

private class Area(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

private suspend fun decode(source: ByteArray): dynamic {
    val global: dynamic = js("globalThis")
    val blob: dynamic = global.Reflect.construct(global.Blob, arrayOf(arrayOf<Any>(source)))
    return suspendRunCatching {
        global.createImageBitmap(blob).unsafeCast<Promise<dynamic>>().await<dynamic>()
    }.getOrElse { throw UnsupportedImageException() }
}

// Why: JPEG has no alpha, so transparent pixels land on white instead of black. Closes the bitmap.
private fun createOnWhite(
    bitmap: dynamic,
    from: Area,
    width: Int,
    height: Int,
): dynamic {
    val global: dynamic = js("globalThis")
    val canvas: dynamic = global.document.createElement("canvas")
    canvas.width = width
    canvas.height = height
    val context: dynamic = canvas.getContext("2d")
    context.imageSmoothingQuality = "high"
    context.fillStyle = BACKGROUND_COLOR
    context.fillRect(0, 0, width, height)
    context.drawImage(bitmap, from.x, from.y, from.width, from.height, 0, 0, width, height)
    bitmap.close()
    return canvas
}

private suspend fun toJpeg(
    canvas: dynamic,
    quality: Int,
): ByteArray {
    val jpeg: dynamic = suspendCoroutine { continuation ->
        canvas.toBlob(
            { blob: dynamic ->
                if (blob == null) {
                    continuation.resumeWithException(UnsupportedImageException())
                } else {
                    continuation.resume(blob)
                }
            },
            "image/jpeg",
            quality / PERCENT,
        )
    }
    val buffer: dynamic = jpeg.arrayBuffer().unsafeCast<Promise<dynamic>>().await()
    return js("new Int8Array(buffer)").unsafeCast<ByteArray>()
}
