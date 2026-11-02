package com.quare.blitzsplit.core.provider.image

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataCreateMutable
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFNumberGetValue
import platform.CoreFoundation.CFNumberRef
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberDoubleType
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextSetInterpolationQuality
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGInterpolationHigh
import platform.ImageIO.CGImageDestinationAddImage
import platform.ImageIO.CGImageDestinationCreateWithData
import platform.ImageIO.CGImageDestinationFinalize
import platform.ImageIO.CGImageSourceCopyPropertiesAtIndex
import platform.ImageIO.CGImageSourceCreateThumbnailAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.CGImageSourceRef
import platform.ImageIO.kCGImageDestinationLossyCompressionQuality
import platform.ImageIO.kCGImagePropertyPixelHeight
import platform.ImageIO.kCGImagePropertyPixelWidth
import platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways
import platform.ImageIO.kCGImageSourceCreateThumbnailWithTransform
import platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize
import kotlin.math.ceil

private const val JPEG_TYPE = "public.jpeg"
private const val BITS_PER_COMPONENT = 8uL
private const val PERCENT = 100.0

internal actual fun createAvatarImageEncoder(): AvatarImageEncoder = AppleAvatarImageEncoder()

internal actual fun createBillImageEncoder(): BillImageEncoder = AppleBillImageEncoder()

/*
 * Why: ImageIO and Core Graphics exist on iOS and macOS alike, so one encoder serves both. The
 * thumbnail API decodes HEIC, applies the EXIF rotation and downsamples while decoding, so a
 * 48 MP photo never sits whole in memory.
 */
@OptIn(ExperimentalForeignApi::class)
internal class AppleAvatarImageEncoder : AvatarImageEncoder {
    override suspend fun invoke(source: ByteArray): ByteArray = withContext(Dispatchers.IO) { encode(source) }

    private fun encode(source: ByteArray): ByteArray = withImageSource(source) { imageSource, width, height ->
        // Why: the thumbnail's longest side is capped, so it is sized for the shortest one to reach 512.
        val longest = maxOf(width, height)
        val maxPixelSize = ceil(AVATAR_SIZE_PX.toDouble() * longest / minOf(width, height)).toInt()
        val upright = imageSource.createUprightThumbnail(minOf(maxPixelSize, longest))
            ?: throw UnsupportedImageException()
        val scale = AVATAR_SIZE_PX / minOf(CGImageGetWidth(upright), CGImageGetHeight(upright)).toDouble()
        val drawnWidth = CGImageGetWidth(upright).toDouble() * scale
        val drawnHeight = CGImageGetHeight(upright).toDouble() * scale
        val square = upright.createOnWhite(
            width = AVATAR_SIZE_PX,
            height = AVATAR_SIZE_PX,
            at = CGRectMake(
                x = (AVATAR_SIZE_PX - drawnWidth) / 2,
                y = (AVATAR_SIZE_PX - drawnHeight) / 2,
                width = drawnWidth,
                height = drawnHeight,
            ),
        )
        CGImageRelease(upright)
        square ?: throw UnsupportedImageException()
        square.toJpeg(AVATAR_JPEG_QUALITY).also { CGImageRelease(square) }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class AppleBillImageEncoder : BillImageEncoder {
    override suspend fun invoke(source: ByteArray): BillImage = withContext(Dispatchers.IO) { encode(source) }

    private fun encode(source: ByteArray): BillImage = withImageSource(source) { imageSource, width, height ->
        // Why: the thumbnail is the whole photo upright, downsampled while decoding to the bucket's longest side.
        val upright = imageSource.createUprightThumbnail(minOf(BILL_IMAGE_MAX_SIDE_PX, maxOf(width, height)))
            ?: throw UnsupportedImageException()
        val outputWidth = CGImageGetWidth(upright).toInt()
        val outputHeight = CGImageGetHeight(upright).toInt()
        val flat = upright.createOnWhite(
            width = outputWidth,
            height = outputHeight,
            at = CGRectMake(0.0, 0.0, outputWidth.toDouble(), outputHeight.toDouble()),
        )
        CGImageRelease(upright)
        flat ?: throw UnsupportedImageException()
        BillImage(
            jpeg = encodeUnderLimit(flat::toJpeg),
            width = outputWidth,
            height = outputHeight,
        ).also { CGImageRelease(flat) }
    }
}

// Why: the picked file's decoder and its size in pixels, released once the block is done with them.
@OptIn(ExperimentalForeignApi::class)
private inline fun <T> withImageSource(
    source: ByteArray,
    block: (imageSource: CGImageSourceRef, width: Int, height: Int) -> T,
): T {
    if (source.isEmpty()) throw UnsupportedImageException()
    val data = source.toCFData()
    val imageSource = CGImageSourceCreateWithData(data, null)
    try {
        imageSource ?: throw UnsupportedImageException()
        val properties = CGImageSourceCopyPropertiesAtIndex(imageSource, 0u, null) ?: throw UnsupportedImageException()
        val width = properties.getIntValue(kCGImagePropertyPixelWidth)
        val height = properties.getIntValue(kCGImagePropertyPixelHeight)
        CFRelease(properties)
        if (width <= 0 || height <= 0) throw UnsupportedImageException()
        return block(imageSource, width, height)
    } finally {
        CFRelease(imageSource)
        CFRelease(data)
    }
}

/*
 * Why: the thumbnail API decodes HEIC, applies the EXIF rotation and downsamples while decoding, its longest side at
 * most the size given.
 */
@OptIn(ExperimentalForeignApi::class)
private fun CGImageSourceRef.createUprightThumbnail(maxPixelSize: Int): CGImageRef? = memScoped {
    val size = alloc<IntVar>().apply { value = maxPixelSize }
    val sizeNumber = CFNumberCreate(null, kCFNumberIntType, size.ptr)
    val options = createMutableDictionary()
    CFDictionarySetValue(options, kCGImageSourceCreateThumbnailFromImageAlways, kCFBooleanTrue)
    CFDictionarySetValue(options, kCGImageSourceCreateThumbnailWithTransform, kCFBooleanTrue)
    CFDictionarySetValue(options, kCGImageSourceThumbnailMaxPixelSize, sizeNumber)
    CGImageSourceCreateThumbnailAtIndex(this@createUprightThumbnail, 0u, options).also {
        CFRelease(options)
        CFRelease(sizeNumber)
    }
}

// Why: JPEG has no alpha, so transparent pixels land on white instead of black.
@OptIn(ExperimentalForeignApi::class)
private fun CGImageRef.createOnWhite(
    width: Int,
    height: Int,
    at: CValue<CGRect>,
): CGImageRef? {
    val colorSpace = CGColorSpaceCreateDeviceRGB()
    val context = CGBitmapContextCreate(
        data = null,
        width = width.toULong(),
        height = height.toULong(),
        bitsPerComponent = BITS_PER_COMPONENT,
        bytesPerRow = 0u,
        space = colorSpace,
        bitmapInfo = CGImageAlphaInfo.kCGImageAlphaNoneSkipLast.value,
    )
    CGColorSpaceRelease(colorSpace)
    context ?: return null
    CGContextSetInterpolationQuality(context, kCGInterpolationHigh)
    CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0)
    CGContextFillRect(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
    CGContextDrawImage(context, at, this)
    return CGBitmapContextCreateImage(context).also { CGContextRelease(context) }
}

@OptIn(ExperimentalForeignApi::class)
private fun CGImageRef.toJpeg(quality: Int): ByteArray = memScoped {
    val output = CFDataCreateMutable(null, 0) ?: throw UnsupportedImageException()
    val type = CFStringCreateWithCString(null, JPEG_TYPE, kCFStringEncodingUTF8)
    val qualityValue = alloc<DoubleVar>().apply { value = quality / PERCENT }
    val qualityNumber = CFNumberCreate(null, kCFNumberDoubleType, qualityValue.ptr)
    val properties = createMutableDictionary()
    CFDictionarySetValue(properties, kCGImageDestinationLossyCompressionQuality, qualityNumber)
    try {
        val destination = CGImageDestinationCreateWithData(output, type, 1u, null)
            ?: throw UnsupportedImageException()
        CGImageDestinationAddImage(destination, this@toJpeg, properties)
        val isWritten = CGImageDestinationFinalize(destination)
        CFRelease(destination)
        if (!isWritten) throw UnsupportedImageException()
        CFDataGetBytePtr(output)?.readBytes(CFDataGetLength(output).toInt()) ?: throw UnsupportedImageException()
    } finally {
        CFRelease(properties)
        CFRelease(qualityNumber)
        CFRelease(type)
        CFRelease(output)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun createMutableDictionary(): CFMutableDictionaryRef? =
    CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)

@OptIn(ExperimentalForeignApi::class)
private fun CFDictionaryRef.getIntValue(key: CFTypeRef?): Int = memScoped {
    val number: CFNumberRef = CFDictionaryGetValue(this@getIntValue, key)?.reinterpret() ?: return 0
    val result = alloc<IntVar>()
    if (CFNumberGetValue(number, kCFNumberIntType, result.ptr)) result.value else 0
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toCFData(): CFDataRef? = usePinned { pinned ->
    CFDataCreate(null, pinned.addressOf(0).reinterpret<UByteVar>(), size.toLong())
}

@OptIn(ExperimentalForeignApi::class)
private fun CFRelease(pointer: CPointer<*>?) {
    pointer?.let { platform.CoreFoundation.CFRelease(it) }
}
