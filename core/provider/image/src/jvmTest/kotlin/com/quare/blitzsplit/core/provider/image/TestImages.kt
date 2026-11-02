package com.quare.blitzsplit.core.provider.image

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

internal fun png(
    width: Int,
    height: Int,
    type: Int = BufferedImage.TYPE_INT_RGB,
    color: (Int, Int) -> Color,
): ByteArray {
    val image = BufferedImage(width, height, type)
    for (x in 0 until width) for (y in 0 until height) image.setRGB(x, y, color(x, y).rgb)
    return ByteArrayOutputStream().use { stream ->
        ImageIO.write(image, "png", stream)
        stream.toByteArray()
    }
}

internal fun ByteArray.isJpeg(): Boolean = this[0] == 0xFF.toByte() && this[1] == 0xD8.toByte()
