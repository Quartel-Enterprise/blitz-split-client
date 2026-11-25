package com.quare.blitzsplit.core.provider.image

import kotlinx.coroutines.test.runTest
import java.awt.Color
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmBillCropEncoderTest {
    private val encoder = JvmBillCropEncoder()

    // Red on the sides and blue in the centered square, so the crop shows in the left edge's color.
    @Test
    fun `GIVEN a wide PNG and its centered square WHEN encoding THEN returns that square at the side asked`() =
        runTest {
            // Given
            val source = png(
                width = 1200,
                height = 600,
            ) { x, _ -> if (x in 300 until 900) Color.BLUE else Color.RED }
            val transform = CropTransform(
                scaleX = 1.0,
                skewX = 0.0,
                translateX = -300.0,
                skewY = 0.0,
                scaleY = 1.0,
                translateY = 0.0,
            )

            // When
            val image = encoder(
                source,
                transform,
                width = 600,
                height = 600,
            )

            // Then
            assertEquals(600, image.width)
            assertEquals(600, image.height)
            assertTrue(image.jpeg.isJpeg())
            assertTrue(image.jpeg.size <= BILL_IMAGE_MAX_BYTES)
            val decoded = ImageIO.read(ByteArrayInputStream(image.jpeg))
            assertEquals(600, decoded.width)
            assertEquals(600, decoded.height)
            assertTrue(Color(decoded.getRGB(4, 300)).blue > 200, "the left edge is the square's, not the side's")
        }
}
