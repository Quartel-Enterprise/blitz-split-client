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
    fun `GIVEN a wide PNG and its centered square WHEN encoding THEN returns that square as a 512 JPEG`() = runTest {
        // Given
        val source = png(
            width = 1200,
            height = 600,
        ) { x, _ -> if (x in 300 until 900) Color.BLUE else Color.RED }

        // When
        val jpeg = encoder(
            source,
            centeredSquare(
                width = 1200,
                height = 600,
            ),
        )

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
        val jpeg = encoder(
            source,
            centeredSquare(
                width = 600,
                height = 600,
            ),
        )

        // Then
        val decoded = ImageIO.read(ByteArrayInputStream(jpeg))
        assertTrue(Color(decoded.getRGB(256, 256)).red > 240, "transparent pixels are white, not black")
    }

    @Test
    fun `GIVEN bytes that are not an image WHEN encoding THEN throws UnsupportedImageException`() = runTest {
        // Given
        val source = "not an image".encodeToByteArray()

        // When
        val result = runCatching {
            encoder(
                source,
                centeredSquare(
                    width = 1,
                    height = 1,
                ),
            )
        }

        // Then
        assertFailsWith<UnsupportedImageException> { result.getOrThrow() }
    }

    // Blue on the left half and red on the right: turned a quarter clockwise, the left half goes on top.
    @Test
    fun `GIVEN a transform that turns the photo WHEN encoding THEN draws it turned`() = runTest {
        // Given
        val source = png(
            width = 600,
            height = 600,
        ) { x, _ -> if (x < 300) Color.BLUE else Color.RED }
        val scale = AVATAR_SIZE_PX / 600.0
        val turnedClockwise = CropTransform(
            scaleX = 0.0,
            skewX = -scale,
            translateX = AVATAR_SIZE_PX.toDouble(),
            skewY = scale,
            scaleY = 0.0,
            translateY = 0.0,
        )

        // When
        val jpeg = encoder(source, turnedClockwise)

        // Then
        val decoded = ImageIO.read(ByteArrayInputStream(jpeg))
        assertTrue(Color(decoded.getRGB(256, 4)).blue > 200, "the left half is on top")
        assertTrue(Color(decoded.getRGB(256, 508)).red > 200, "the right half is at the bottom")
    }

    // The photo's shortest side filling the avatar, centered, as the editor opens it.
    private fun centeredSquare(
        width: Int,
        height: Int,
    ): CropTransform {
        val scale = AVATAR_SIZE_PX.toDouble() / minOf(width, height)
        return CropTransform(
            scaleX = scale,
            skewX = 0.0,
            translateX = (AVATAR_SIZE_PX - width * scale) / 2,
            skewY = 0.0,
            scaleY = scale,
            translateY = (AVATAR_SIZE_PX - height * scale) / 2,
        )
    }
}
