package com.quare.blitzsplit.core.provider.image

import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

private const val PERCENT = 100f

// Why: the jvm target only runs the unit tests, so ImageIO is enough (no EXIF, no HEIC).
internal actual fun createAvatarImageEncoder(): AvatarImageEncoder = JvmAvatarImageEncoder()

internal actual fun createBillImageEncoder(): BillImageEncoder = JvmBillImageEncoder()

internal class JvmAvatarImageEncoder : AvatarImageEncoder {
    override suspend fun invoke(
        source: ByteArray,
        transform: CropTransform,
    ): ByteArray {
        val decoded = decode(source)
        val output = BufferedImage(AVATAR_SIZE_PX, AVATAR_SIZE_PX, BufferedImage.TYPE_INT_RGB)
        output.createGraphics().run {
            color = Color.WHITE
            fillRect(0, 0, AVATAR_SIZE_PX, AVATAR_SIZE_PX)
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            drawImage(decoded, transform.toAffineTransform(), null)
            dispose()
        }
        return output.toJpeg(AVATAR_JPEG_QUALITY)
    }

    private fun CropTransform.toAffineTransform(): AffineTransform =
        AffineTransform(scaleX, skewY, skewX, scaleY, translateX, translateY)
}

internal class JvmBillImageEncoder : BillImageEncoder {
    override suspend fun invoke(source: ByteArray): BillImage {
        val decoded = decode(source)
        val size = findBillImageSize(decoded.width, decoded.height)
        val output = decoded.createOnWhite(size.width, size.height)
        return BillImage(
            jpeg = encodeUnderLimit(output::toJpeg),
            width = size.width,
            height = size.height,
        )
    }
}

private fun decode(source: ByteArray): BufferedImage =
    ByteArrayInputStream(source).use(ImageIO::read) ?: throw UnsupportedImageException()

private fun BufferedImage.createOnWhite(
    width: Int,
    height: Int,
): BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).also { output ->
    output.createGraphics().run {
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        drawImage(this@createOnWhite, 0, 0, width, height, Color.WHITE, null)
        dispose()
    }
}

private fun BufferedImage.toJpeg(quality: Int): ByteArray = ByteArrayOutputStream().use { stream ->
    val writer = ImageIO.getImageWritersByFormatName("jpg").next()
    ImageIO.createImageOutputStream(stream).use { output ->
        writer.output = output
        val params = writer.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = quality / PERCENT
        }
        writer.write(null, IIOImage(this, null, null), params)
        writer.dispose()
    }
    stream.toByteArray()
}
