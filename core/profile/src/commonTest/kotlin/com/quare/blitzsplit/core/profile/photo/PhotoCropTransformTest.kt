package com.quare.blitzsplit.core.profile.photo

import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.provider.image.CropTransform
import kotlin.test.Test
import kotlin.test.assertEquals

class PhotoCropTransformTest {
    @Test
    fun `GIVEN a landscape photo as picked WHEN framing it THEN its centered square fills the avatar`() {
        // Given
        val crop = PhotoCrop.of(
            photoWidth = 1200,
            photoHeight = 600,
        )

        // When
        val transform = crop.toTransform(SIDE)

        // Then
        assertMaps(
            transform,
            x = 600.0,
            y = 300.0,
            toX = SIDE / 2.0,
            toY = SIDE / 2.0,
        )
        assertMaps(
            transform,
            x = 300.0,
            y = 0.0,
            toX = 0.0,
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 900.0,
            y = 600.0,
            toX = SIDE.toDouble(),
            toY = SIDE.toDouble(),
        )
    }

    @Test
    fun `GIVEN a portrait photo as picked WHEN framing it THEN its centered square fills the avatar`() {
        // Given
        val crop = PhotoCrop.of(
            photoWidth = 600,
            photoHeight = 1200,
        )

        // When
        val transform = crop.toTransform(SIDE)

        // Then
        assertMaps(
            transform,
            x = 0.0,
            y = 300.0,
            toX = 0.0,
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 600.0,
            y = 900.0,
            toX = SIDE.toDouble(),
            toY = SIDE.toDouble(),
        )
    }

    @Test
    fun `GIVEN a quarter turn WHEN framing THEN the left edge of the photo goes to the top`() {
        // Given
        val crop = square().copy(quarterTurns = 1)

        // When
        val transform = crop.toTransform(SIDE)

        // Then
        assertMaps(
            transform,
            x = 0.0,
            y = 0.0,
            toX = SIDE.toDouble(),
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 0.0,
            y = 1000.0,
            toX = 0.0,
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 1000.0,
            y = 0.0,
            toX = SIDE.toDouble(),
            toY = SIDE.toDouble(),
        )
    }

    @Test
    fun `GIVEN a half turn WHEN framing THEN the photo is upside down`() {
        // Given
        val crop = square().copy(quarterTurns = 2)

        // When
        val transform = crop.toTransform(SIDE)

        // Then
        assertMaps(
            transform,
            x = 0.0,
            y = 0.0,
            toX = SIDE.toDouble(),
            toY = SIDE.toDouble(),
        )
        assertMaps(
            transform,
            x = 1000.0,
            y = 0.0,
            toX = 0.0,
            toY = SIDE.toDouble(),
        )
    }

    @Test
    fun `GIVEN three quarter turns WHEN framing THEN the top left corner goes to the bottom left`() {
        // Given
        val crop = square().copy(quarterTurns = 3)

        // When
        val transform = crop.toTransform(SIDE)

        // Then
        assertMaps(
            transform,
            x = 0.0,
            y = 0.0,
            toX = 0.0,
            toY = SIDE.toDouble(),
        )
        assertMaps(
            transform,
            x = 1000.0,
            y = 0.0,
            toX = 0.0,
            toY = 0.0,
        )
    }

    @Test
    fun `GIVEN a mirrored photo WHEN framing THEN the left of the photo is on the right`() {
        // Given
        val crop = square().copy(isMirrored = true)

        // When
        val transform = crop.toTransform(SIDE)

        // Then
        assertMaps(
            transform,
            x = 0.0,
            y = 0.0,
            toX = SIDE.toDouble(),
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 1000.0,
            y = 1000.0,
            toX = 0.0,
            toY = SIDE.toDouble(),
        )
    }

    @Test
    fun `GIVEN a mirrored photo turned a quarter WHEN framing THEN it is mirrored before it turns`() {
        // Given
        val crop = square().copy(
            isMirrored = true,
            quarterTurns = 1,
        )

        // When
        val transform = crop.toTransform(SIDE)

        // Then
        // Mirrored, the top left corner is at the top right, which the clockwise turn takes to the bottom right
        assertMaps(
            transform,
            x = 0.0,
            y = 0.0,
            toX = SIDE.toDouble(),
            toY = SIDE.toDouble(),
        )
        assertMaps(
            transform,
            x = 1000.0,
            y = 0.0,
            toX = SIDE.toDouble(),
            toY = 0.0,
        )
    }

    @Test
    fun `GIVEN zoom 2 WHEN framing THEN the center of the photo shows twice as big`() {
        // Given
        val crop = PhotoCrop
            .of(
                photoWidth = 1200,
                photoHeight = 600,
            ).copy(zoom = 2f)

        // When
        val transform = crop.toTransform(SIDE)

        // Then
        assertMaps(
            transform,
            x = 600.0,
            y = 300.0,
            toX = SIDE / 2.0,
            toY = SIDE / 2.0,
        )
        assertMaps(
            transform,
            x = 750.0,
            y = 300.0,
            toX = SIDE / 2.0 + SIDE / 2.0,
            toY = SIDE / 2.0,
        )
        assertMaps(
            transform,
            x = 600.0,
            y = 150.0,
            toX = SIDE / 2.0,
            toY = 0.0,
        )
    }

    @Test
    fun `GIVEN an offset of a quarter WHEN framing THEN the center of the photo moves a quarter of the side`() {
        // Given
        val crop = PhotoCrop
            .of(
                photoWidth = 1200,
                photoHeight = 600,
            ).copy(offsetX = 0.25f)

        // When
        val transform = crop.toTransform(SIDE)

        // Then
        assertMaps(
            transform,
            x = 600.0,
            y = 300.0,
            toX = 0.75 * SIDE,
            toY = SIDE / 2.0,
        )
    }

    @Test
    fun `GIVEN an offset down on a turned photo WHEN framing THEN the offset moves it down in the frame`() {
        // Given
        val crop = PhotoCrop
            .of(
                photoWidth = 1200,
                photoHeight = 600,
            ).copy(
                quarterTurns = 1,
                offsetY = 0.25f,
            )

        // When
        val transform = crop.toTransform(SIDE)

        // Then
        assertMaps(
            transform,
            x = 600.0,
            y = 300.0,
            toX = SIDE / 2.0,
            toY = 0.75 * SIDE,
        )
    }

    @Test
    fun `GIVEN another side WHEN framing THEN the square scales to that side`() {
        // Given
        val crop = PhotoCrop.of(
            photoWidth = 1200,
            photoHeight = 600,
        )

        // When
        val transform = crop.toTransform(100)

        // Then
        assertMaps(
            transform,
            x = 300.0,
            y = 0.0,
            toX = 0.0,
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 900.0,
            y = 600.0,
            toX = 100.0,
            toY = 100.0,
        )
    }

    @Test
    fun `GIVEN a photo as picked WHEN keeping it whole THEN each pixel stays where it is`() {
        // Given
        val crop = landscape()

        // When
        val transform = crop.toWholeTransform()

        // Then
        assertMaps(
            transform,
            x = 0.0,
            y = 0.0,
            toX = 0.0,
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 1200.0,
            y = 600.0,
            toX = 1200.0,
            toY = 600.0,
        )
        assertMaps(
            transform,
            x = 600.0,
            y = 300.0,
            toX = 600.0,
            toY = 300.0,
        )
    }

    @Test
    fun `GIVEN a photo zoomed in and moved WHEN keeping it whole THEN the zoom and the offset are left out`() {
        // Given
        val crop = landscape().copy(
            zoom = 2f,
            offsetX = 0.25f,
            offsetY = -0.25f,
        )

        // When
        val transform = crop.toWholeTransform()

        // Then
        assertMaps(
            transform,
            x = 0.0,
            y = 0.0,
            toX = 0.0,
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 1200.0,
            y = 600.0,
            toX = 1200.0,
            toY = 600.0,
        )
    }

    @Test
    fun `GIVEN a quarter turn WHEN keeping the photo whole THEN it fills the turned image with its left edge on top`() {
        // Given
        val crop = landscape().copy(quarterTurns = 1)

        // When
        val transform = crop.toWholeTransform()

        // Then
        // Turned, the 1200 by 600 photo is 600 by 1200
        assertMaps(
            transform,
            x = 0.0,
            y = 0.0,
            toX = 600.0,
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 0.0,
            y = 600.0,
            toX = 0.0,
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 1200.0,
            y = 600.0,
            toX = 0.0,
            toY = 1200.0,
        )
        assertMaps(
            transform,
            x = 600.0,
            y = 300.0,
            toX = 300.0,
            toY = 600.0,
        )
    }

    @Test
    fun `GIVEN a half turn WHEN keeping the photo whole THEN it is upside down in an image of its size`() {
        // Given
        val crop = landscape().copy(quarterTurns = 2)

        // When
        val transform = crop.toWholeTransform()

        // Then
        assertMaps(
            transform,
            x = 0.0,
            y = 0.0,
            toX = 1200.0,
            toY = 600.0,
        )
        assertMaps(
            transform,
            x = 1200.0,
            y = 600.0,
            toX = 0.0,
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 600.0,
            y = 300.0,
            toX = 600.0,
            toY = 300.0,
        )
    }

    @Test
    fun `GIVEN three quarter turns WHEN keeping the photo whole THEN its top left corner is at the bottom left`() {
        // Given
        val crop = landscape().copy(quarterTurns = 3)

        // When
        val transform = crop.toWholeTransform()

        // Then
        assertMaps(
            transform,
            x = 0.0,
            y = 0.0,
            toX = 0.0,
            toY = 1200.0,
        )
        assertMaps(
            transform,
            x = 1200.0,
            y = 0.0,
            toX = 0.0,
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 1200.0,
            y = 600.0,
            toX = 600.0,
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 600.0,
            y = 300.0,
            toX = 300.0,
            toY = 600.0,
        )
    }

    @Test
    fun `GIVEN a mirrored photo WHEN keeping it whole THEN its left is on the right of an image of its size`() {
        // Given
        val crop = landscape().copy(isMirrored = true)

        // When
        val transform = crop.toWholeTransform()

        // Then
        assertMaps(
            transform,
            x = 0.0,
            y = 0.0,
            toX = 1200.0,
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 1200.0,
            y = 600.0,
            toX = 0.0,
            toY = 600.0,
        )
        assertMaps(
            transform,
            x = 600.0,
            y = 300.0,
            toX = 600.0,
            toY = 300.0,
        )
    }

    @Test
    fun `GIVEN a mirrored photo turned a quarter WHEN keeping it whole THEN it is mirrored before it turns`() {
        // Given
        val crop = landscape().copy(
            isMirrored = true,
            quarterTurns = 1,
        )

        // When
        val transform = crop.toWholeTransform()

        // Then
        // Mirrored, the top left corner is at the top right, which the clockwise turn takes to the bottom right
        assertMaps(
            transform,
            x = 0.0,
            y = 0.0,
            toX = 600.0,
            toY = 1200.0,
        )
        assertMaps(
            transform,
            x = 1200.0,
            y = 0.0,
            toX = 600.0,
            toY = 0.0,
        )
        assertMaps(
            transform,
            x = 600.0,
            y = 300.0,
            toX = 300.0,
            toY = 600.0,
        )
    }

    private fun landscape(): PhotoCrop = PhotoCrop.of(
        photoWidth = 1200,
        photoHeight = 600,
    )

    private fun square(): PhotoCrop = PhotoCrop.of(
        photoWidth = 1000,
        photoHeight = 1000,
    )

    private fun assertMaps(
        transform: CropTransform,
        x: Double,
        y: Double,
        toX: Double,
        toY: Double,
    ) {
        val mappedX = transform.scaleX * x + transform.skewX * y + transform.translateX
        val mappedY = transform.skewY * x + transform.scaleY * y + transform.translateY
        assertEquals(toX, mappedX, TOLERANCE, "x of ($x, $y)")
        assertEquals(toY, mappedY, TOLERANCE, "y of ($x, $y)")
    }

    private companion object {
        const val SIDE = 512
        const val TOLERANCE = 1e-6
    }
}
