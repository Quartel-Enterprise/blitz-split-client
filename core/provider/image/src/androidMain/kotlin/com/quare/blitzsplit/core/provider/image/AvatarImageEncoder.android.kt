package com.quare.blitzsplit.core.provider.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.hypot

internal actual fun createAvatarImageEncoder(): AvatarImageEncoder = AndroidAvatarImageEncoder()

internal actual fun createBillImageEncoder(): BillImageEncoder = AndroidBillImageEncoder()

internal class AndroidAvatarImageEncoder : AvatarImageEncoder {
    override suspend fun invoke(
        source: ByteArray,
        transform: CropTransform,
    ): ByteArray = withContext(Dispatchers.Default) {
        // Why: decoded whole, since the transform is in the photo's own pixels and the editor's is at most 1600 px.
        var photo = decode(source) { _, _ -> 1 }
        val matrix = transform.toMatrix()
        /*
         * Why: bilinear filtering only blends neighbors, so shrinking 1600 px to 512 in one pass aliases hair and
         * text. Halved first until the last pass shrinks by half at most, as Apple's and the web's smoothing do.
         */
        while (matrix.findScale() < HALF && photo.width > 1 && photo.height > 1) {
            val halved = Bitmap.createScaledBitmap(photo, photo.width / 2, photo.height / 2, true)
            matrix.preScale(photo.width.toFloat() / halved.width, photo.height.toFloat() / halved.height)
            photo.recycle()
            photo = halved
        }
        val output = Bitmap.createBitmap(AVATAR_SIZE_PX, AVATAR_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(photo, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        photo.recycle()
        output.toJpeg(AVATAR_JPEG_QUALITY).also { output.recycle() }
    }

    // Why: how much the matrix scales a length, the same whatever quarter turn or mirror it holds.
    private fun Matrix.findScale(): Float {
        val values = FloatArray(MATRIX_VALUES).also(::getValues)
        return hypot(values[Matrix.MSCALE_X], values[Matrix.MSKEW_Y])
    }

    private companion object {
        const val HALF = 0.5f
        const val MATRIX_VALUES = 9
    }

    private fun CropTransform.toMatrix(): Matrix = Matrix().apply {
        setValues(
            floatArrayOf(
                scaleX.toFloat(),
                skewX.toFloat(),
                translateX.toFloat(),
                skewY.toFloat(),
                scaleY.toFloat(),
                translateY.toFloat(),
                0f,
                0f,
                1f,
            ),
        )
    }
}

internal class AndroidBillImageEncoder : BillImageEncoder {
    override suspend fun invoke(source: ByteArray): BillImage = withContext(Dispatchers.Default) {
        val decoded = decode(source) { width, height ->
            sampleSizeFor(maxOf(width, height), BILL_IMAGE_MAX_SIDE_PX)
        }
        val size = findBillImageSize(decoded.width, decoded.height)
        val output = decoded.createOnWhite(Rect(0, 0, decoded.width, decoded.height), size.width, size.height)
        // Why: recycled also when no quality fits the limit, which throws.
        try {
            BillImage(
                jpeg = encodeUnderLimit(output::toJpeg),
                width = size.width,
                height = size.height,
            )
        } finally {
            output.recycle()
        }
    }
}

/*
 * Why: ImageDecoder applies the EXIF rotation and decodes HEIC; BitmapFactory is the fallback
 * of Android 8, where a camera photo may come out sideways.
 */
private fun decode(
    source: ByteArray,
    sampleSizeOf: (width: Int, height: Int) -> Int,
): Bitmap = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(source))) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(sampleSizeOf(info.size.width, info.size.height))
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeOf(bounds.outWidth, bounds.outHeight)
        }
        BitmapFactory.decodeByteArray(source, 0, source.size, options)
    }
} catch (exception: Exception) {
    throw UnsupportedImageException().apply { initCause(exception) }
} ?: throw UnsupportedImageException()

// Why: the largest power of two that still leaves the side at the target or above, decoded at that fraction.
private fun sampleSizeFor(
    side: Int,
    target: Int,
): Int {
    var sampleSize = 1
    while (side / (sampleSize * 2) >= target) sampleSize *= 2
    return sampleSize
}

// Why: JPEG has no alpha, so transparent pixels land on white instead of black. Recycles the decoded bitmap.
private fun Bitmap.createOnWhite(
    from: Rect,
    width: Int,
    height: Int,
): Bitmap {
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(this, from, Rect(0, 0, width, height), Paint(Paint.FILTER_BITMAP_FLAG))
    recycle()
    return output
}

private fun Bitmap.toJpeg(quality: Int): ByteArray = ByteArrayOutputStream().use { stream ->
    compress(Bitmap.CompressFormat.JPEG, quality, stream)
    stream.toByteArray()
}
