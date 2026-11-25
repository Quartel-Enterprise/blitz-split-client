package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhotoFrameTest {
    @Test
    fun `GIVEN the centered square of a wide image WHEN placing it THEN the image is twice as wide and centered`() {
        // Given
        val frame = PhotoFrame(
            left = 0.25f,
            top = 0f,
            width = 0.5f,
            height = 1f,
        )

        // When
        val placement = frame.toPlacement(
            imageWidth = 1200,
            imageHeight = 600,
        )

        // Then
        assertSamePlacement(
            PhotoFramePlacement(
                width = 2f,
                height = 1f,
                x = -0.5f,
                y = 0f,
            ),
            placement,
        )
    }

    @Test
    fun `GIVEN the whole of a square image WHEN placing it in a tile THEN the image fills the tile`() {
        // Given
        val frame = PhotoFrame(
            left = 0f,
            top = 0f,
            width = 1f,
            height = 1f,
        )

        // When
        val placement = frame.toPlacement(
            imageWidth = 800,
            imageHeight = 800,
        )

        // Then
        assertSamePlacement(
            PhotoFramePlacement(
                width = 1f,
                height = 1f,
                x = 0f,
                y = 0f,
            ),
            placement,
        )
    }

    @Test
    fun `GIVEN a square at the right edge of a wide image WHEN placing it THEN both right edges meet`() {
        // Given
        val frame = PhotoFrame(
            left = 0.5f,
            top = 0f,
            width = 0.5f,
            height = 1f,
        )

        // When
        val placement = frame.toPlacement(
            imageWidth = 1200,
            imageHeight = 600,
        )

        // Then
        assertSamePlacement(
            PhotoFramePlacement(
                width = 2f,
                height = 1f,
                x = -1f,
                y = 0f,
            ),
            placement,
        )
    }

    @Test
    fun `GIVEN a frame taller than wide in pixels WHEN placing it THEN its width covers the tile around its center`() {
        // Given
        val frame = PhotoFrame(
            left = 0.1f,
            top = 0.2f,
            width = 0.3f,
            height = 0.4f,
        )

        // When
        val placement = frame.toPlacement(
            imageWidth = 2000,
            imageHeight = 1000,
        )

        // Then
        assertSamePlacement(
            PhotoFramePlacement(
                width = 5f,
                height = 2.5f,
                x = -0.75f,
                y = -0.5f,
            ),
            placement,
        )
    }

    @Test
    fun `GIVEN a frame past the top and bottom of an image WHEN placing it THEN its width covers the tile`() {
        // Given
        val frame = PhotoFrame(
            left = 0f,
            top = -0.25f,
            width = 1f,
            height = 1.5f,
        )

        // When
        val placement = frame.toPlacement(
            imageWidth = 1000,
            imageHeight = 1000,
        )

        // Then
        assertSamePlacement(
            PhotoFramePlacement(
                width = 1f,
                height = 1f,
                x = 0f,
                y = 0f,
            ),
            placement,
        )
    }

    @Test
    fun `GIVEN a frame past the sides of a square image WHEN placing it in a tile THEN its height covers the tile`() {
        // Given
        val frame = PhotoFrame(
            left = -0.5f,
            top = 0.25f,
            width = 2f,
            height = 0.5f,
        )

        // When
        val placement = frame.toPlacement(
            imageWidth = 1000,
            imageHeight = 1000,
        )

        // Then
        assertSamePlacement(
            PhotoFramePlacement(
                width = 2f,
                height = 2f,
                x = -0.5f,
                y = -0.5f,
            ),
            placement,
        )
    }

    @Test
    fun `GIVEN the frame of a zoomed photo off center WHEN placing it THEN the tile shows what the editor showed`() {
        // Given
        val crop = PhotoCrop.of(1200, 600).toZoomed(2f).toDragged(0.3f, 0.2f)
        val frame = crop.toFrameInTurnedPhoto()

        // When
        val placement = frame.toPlacement(
            imageWidth = crop.turnedWidth,
            imageHeight = crop.turnedHeight,
        )

        // Then
        assertSamePlacement(
            PhotoFramePlacement(
                width = 4f,
                height = 2f,
                x = -1.2f,
                y = -0.3f,
            ),
            placement,
        )
    }

    @Test
    fun `GIVEN all four values WHEN making a frame THEN it holds them`() {
        // Given
        val values = listOf(0.1f, 0.2f, 0.3f, 0.4f)

        // When
        val frame = PhotoFrame.ofOrNull(
            left = values[0],
            top = values[1],
            width = values[2],
            height = values[3],
        )

        // Then
        assertEquals(
            PhotoFrame(
                left = 0.1f,
                top = 0.2f,
                width = 0.3f,
                height = 0.4f,
            ),
            frame,
        )
    }

    @Test
    fun `GIVEN no values WHEN making a frame THEN there is none`() {
        // Given
        val missing: Float? = null

        // When
        val frame = PhotoFrame.ofOrNull(
            left = missing,
            top = missing,
            width = missing,
            height = missing,
        )

        // Then
        assertNull(frame)
    }

    @Test
    fun `GIVEN only the left missing WHEN making a frame THEN there is none`() {
        // Given
        val missing: Float? = null

        // When
        val frame = PhotoFrame.ofOrNull(
            left = missing,
            top = 0.2f,
            width = 0.3f,
            height = 0.4f,
        )

        // Then
        assertNull(frame)
    }

    @Test
    fun `GIVEN only the top missing WHEN making a frame THEN there is none`() {
        // Given
        val missing: Float? = null

        // When
        val frame = PhotoFrame.ofOrNull(
            left = 0.1f,
            top = missing,
            width = 0.3f,
            height = 0.4f,
        )

        // Then
        assertNull(frame)
    }

    @Test
    fun `GIVEN only the width missing WHEN making a frame THEN there is none`() {
        // Given
        val missing: Float? = null

        // When
        val frame = PhotoFrame.ofOrNull(
            left = 0.1f,
            top = 0.2f,
            width = missing,
            height = 0.4f,
        )

        // Then
        assertNull(frame)
    }

    @Test
    fun `GIVEN only the height missing WHEN making a frame THEN there is none`() {
        // Given
        val missing: Float? = null

        // When
        val frame = PhotoFrame.ofOrNull(
            left = 0.1f,
            top = 0.2f,
            width = 0.3f,
            height = missing,
        )

        // Then
        assertNull(frame)
    }

    private fun assertSamePlacement(
        expected: PhotoFramePlacement,
        actual: PhotoFramePlacement,
    ) {
        assertEquals(expected.width, actual.width, TOLERANCE)
        assertEquals(expected.height, actual.height, TOLERANCE)
        assertEquals(expected.x, actual.x, TOLERANCE)
        assertEquals(expected.y, actual.y, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1e-5f
    }
}
