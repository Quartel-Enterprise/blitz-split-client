package com.quare.blitzsplit.core.provider.image

import kotlinx.coroutines.test.runTest
import java.awt.Color
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JvmBillImageEncoderTest {
    private val encoder = JvmBillImageEncoder()

    @Test
    fun `GIVEN a photo wider than the limit WHEN encoding THEN fits its longest side in 1600 keeping its shape`() =
        runTest {
            // Given
            val source = png(
                width = 3200,
                height = 1200,
            ) { _, _ -> Color.GRAY }

            // When
            val image = encoder(source)

            // Then
            val decoded = ImageIO.read(ByteArrayInputStream(image.jpeg))
            assertEquals(1600 to 600, image.width to image.height)
            assertEquals(1600 to 600, decoded.width to decoded.height)
            assertTrue(image.jpeg.isJpeg())
        }

    @Test
    fun `GIVEN a photo taller than the limit WHEN encoding THEN fits its height in 1600`() = runTest {
        // Given
        val source = png(
            width = 900,
            height = 2400,
        ) { _, _ -> Color.GRAY }

        // When
        val image = encoder(source)

        // Then
        assertEquals(600 to 1600, image.width to image.height)
    }

    @Test
    fun `GIVEN a photo within the limit WHEN encoding THEN keeps its size`() = runTest {
        // Given
        val source = png(
            width = 800,
            height = 600,
        ) { _, _ -> Color.GRAY }

        // When
        val image = encoder(source)

        // Then
        assertEquals(800 to 600, image.width to image.height)
        assertTrue(image.jpeg.size <= BILL_IMAGE_MAX_BYTES)
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
