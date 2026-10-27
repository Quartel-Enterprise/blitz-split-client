package com.quare.blitzsplit.core.provider.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

internal actual fun createAvatarImageEncoder(): AvatarImageEncoder = AndroidAvatarImageEncoder()

internal class AndroidAvatarImageEncoder : AvatarImageEncoder {
    override suspend fun invoke(source: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        val decoded = decode(source)
        val output = Bitmap.createBitmap(AVATAR_SIZE_PX, AVATAR_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(
            decoded,
            decoded.getCenteredSquare(),
            Rect(0, 0, AVATAR_SIZE_PX, AVATAR_SIZE_PX),
            Paint(Paint.FILTER_BITMAP_FLAG),
        )
        decoded.recycle()
        ByteArrayOutputStream().use { stream ->
            output.compress(Bitmap.CompressFormat.JPEG, AVATAR_JPEG_QUALITY, stream)
            output.recycle()
            stream.toByteArray()
        }
    }

    /*
     * Why: ImageDecoder applies the EXIF rotation and decodes HEIC; BitmapFactory is the fallback
     * of Android 8, where a camera photo may come out sideways.
     */
    private fun decode(source: ByteArray): Bitmap = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(source))) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSampleSize(calculateSampleSize(info.size.width, info.size.height))
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
            }
            BitmapFactory.decodeByteArray(source, 0, source.size, options)
        }
    } catch (exception: Exception) {
        throw UnsupportedImageException().apply { initCause(exception) }
    } ?: throw UnsupportedImageException()

    // Why: decoding a 12 MP photo whole only to shrink it to 512 would waste ~48 MB of memory.
    private fun calculateSampleSize(
        width: Int,
        height: Int,
    ): Int {
        var sampleSize = 1
        while (minOf(width, height) / (sampleSize * 2) >= AVATAR_SIZE_PX) sampleSize *= 2
        return sampleSize
    }

    private fun Bitmap.getCenteredSquare(): Rect {
        val side = minOf(width, height)
        val left = (width - side) / 2
        val top = (height - side) / 2
        return Rect(left, top, left + side, top + side)
    }
}
