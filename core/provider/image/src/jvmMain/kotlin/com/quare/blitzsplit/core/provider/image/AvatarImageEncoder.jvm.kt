package com.quare.blitzsplit.core.provider.image

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

// Why: the jvm target only runs the unit tests, so ImageIO is enough (no EXIF, no HEIC).
internal actual fun createAvatarImageEncoder(): AvatarImageEncoder = JvmAvatarImageEncoder()

internal class JvmAvatarImageEncoder : AvatarImageEncoder {
    override suspend fun invoke(source: ByteArray): ByteArray {
        val decoded = ByteArrayInputStream(source).use(ImageIO::read) ?: throw UnsupportedImageException()
        val side = minOf(decoded.width, decoded.height)
        val square = decoded.getSubimage((decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
        val output = BufferedImage(AVATAR_SIZE_PX, AVATAR_SIZE_PX, BufferedImage.TYPE_INT_RGB)
        output.createGraphics().run {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            drawImage(square, 0, 0, AVATAR_SIZE_PX, AVATAR_SIZE_PX, Color.WHITE, null)
            dispose()
        }
        return output.toJpeg()
    }

    private fun BufferedImage.toJpeg(): ByteArray = ByteArrayOutputStream().use { stream ->
        val writer = ImageIO.getImageWritersByFormatName("jpg").next()
        ImageIO.createImageOutputStream(stream).use { output ->
            writer.output = output
            val params = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = AVATAR_JPEG_QUALITY / PERCENT
            }
            writer.write(null, IIOImage(this, null, null), params)
            writer.dispose()
        }
        stream.toByteArray()
    }

    private companion object {
        const val PERCENT = 100f
    }
}
