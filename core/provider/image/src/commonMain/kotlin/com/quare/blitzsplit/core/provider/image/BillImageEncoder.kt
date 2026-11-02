package com.quare.blitzsplit.core.provider.image

// Why: the qualities tried in turn, the first one under the limit kept.
private val billJpegQualities = listOf(80, 65, 50)

/*
 * Why: a bill's photo (Anexos, 7.3g) or the receipt it was read from (7.1e) as the `bill-attachments` bucket takes it:
 * upright, its longest side at most 1600 px, as a JPEG under 1.5 MB, with its size for the UIs to lay it out before
 * it loads.
 */
fun interface BillImageEncoder {
    suspend operator fun invoke(source: ByteArray): BillImage
}

class BillImage(
    val jpeg: ByteArray,
    val width: Int,
    val height: Int,
)

const val BILL_IMAGE_MAX_SIDE_PX = 1600

/*
 * Why: under the bucket's 2 MiB, and under the 2 MiB Android's Room reads a row through (CursorWindow) with room for
 * the rest of the row, since the photo waits there until it goes up. A 1600 px photo only reaches it when it is all
 * fine detail.
 */
const val BILL_IMAGE_MAX_BYTES = 1_500_000

internal class ScaledSize(
    val width: Int,
    val height: Int,
)

// Why: the size that fits the longest side in the limit, keeping the proportions; never larger than the photo.
internal fun findBillImageSize(
    width: Int,
    height: Int,
): ScaledSize {
    val longest = maxOf(width, height)
    if (longest <= BILL_IMAGE_MAX_SIDE_PX) return ScaledSize(width, height)
    val scale = BILL_IMAGE_MAX_SIDE_PX.toDouble() / longest
    return ScaledSize(
        width = maxOf(1, (width * scale).toInt()),
        height = maxOf(1, (height * scale).toInt()),
    )
}

internal inline fun encodeUnderLimit(encode: (quality: Int) -> ByteArray): ByteArray {
    billJpegQualities.forEach { quality ->
        val jpeg = encode(quality)
        if (jpeg.size <= BILL_IMAGE_MAX_BYTES) return jpeg
    }
    throw ImageTooLargeException()
}

internal expect fun createBillImageEncoder(): BillImageEncoder
