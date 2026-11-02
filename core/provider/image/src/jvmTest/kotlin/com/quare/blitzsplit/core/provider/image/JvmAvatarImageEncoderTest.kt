package com.quare.blitzsplit.core.provider.image

import kotlinx.coroutines.test.runTest
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JvmAvatarImageEncoderTest {
    private val encoder = JvmAvatarImageEncoder()

    // Red on the sides and blue in the centered square, so the crop shows in the left edge's color.
    @Test
    fun `GIVEN a wide PNG WHEN encoding THEN returns its centered square as a 512 JPEG`() = runTest {
        // Given
        val source = png(
            width = 1200,
            height = 600,
        ) { x, _ -> if (x in 300 until 900) Color.BLUE else Color.RED }

        // When
        val jpeg = encoder(source)

        // Then
        val decoded = ImageIO.read(ByteArrayInputStream(jpeg))
        assertEquals(AVATAR_SIZE_PX, decoded.width)
        assertEquals(AVATAR_SIZE_PX, decoded.height)
        assertTrue(jpeg.isJpeg())
        assertTrue(Color(decoded.getRGB(4, 256)).blue > 200, "the left edge is the square's, not the side's")
    }

    @Test
    fun `GIVEN a transparent PNG WHEN encoding THEN puts it on white`() = runTest {
        // Given
        val source = png(
            width = 600,
            height = 600,
            type = BufferedImage.TYPE_INT_ARGB,
        ) { _, _ -> Color(0, 0, 0, 0) }

        // When
        val jpeg = encoder(source)

        // Then
        val decoded = ImageIO.read(ByteArrayInputStream(jpeg))
        assertTrue(Color(decoded.getRGB(256, 256)).red > 240, "transparent pixels are white, not black")
    }

    @Test
    fun `GIVEN bytes that are not an image WHEN encoding THEN throws UnsupportedImageException`() = runTest {
        // Given
        val source = "not an image".encodeToByteArray()

        // When
        val result = runCatching { encoder(source) }

        // Then
        assertFailsWith<UnsupportedImageException> { result.getOrThrow() }
    }
}
