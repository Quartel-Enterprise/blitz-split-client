package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhotoCropTest {
    @Test
    fun `GIVEN a photo just picked WHEN framing it THEN it is upright not mirrored at zoom 1 and centered`() {
        // Given
        val width = 4000
        val height = 3000

        // When
        val crop = PhotoCrop.of(width, height)

        // Then
        assertEquals(4000, crop.photoWidth)
        assertEquals(3000, crop.photoHeight)
        assertEquals(0, crop.quarterTurns)
        assertEquals(0, crop.rotationDegrees)
        assertFalse(crop.isMirrored)
        assertEquals(1f, crop.zoom, TOLERANCE)
        assertEquals(0f, crop.offsetX, TOLERANCE)
        assertEquals(0f, crop.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a wide photo WHEN sizing it in the frame THEN its height fills the frame and its width overflows`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000)

        // When
        val size = crop.photoWidthInFrame to crop.photoHeightInFrame

        // Then
        assertEquals(2f, size.first, TOLERANCE)
        assertEquals(1f, size.second, TOLERANCE)
    }

    @Test
    fun `GIVEN a tall photo WHEN sizing it in the frame THEN its width fills the frame and its height overflows`() {
        // Given
        val crop = PhotoCrop.of(1000, 2000)

        // When
        val size = crop.photoWidthInFrame to crop.photoHeightInFrame

        // Then
        assertEquals(1f, size.first, TOLERANCE)
        assertEquals(2f, size.second, TOLERANCE)
    }

    @Test
    fun `GIVEN three quarter turns WHEN reading the rotation THEN it is 270 degrees`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).copy(quarterTurns = 3)

        // When
        val degrees = crop.rotationDegrees

        // Then
        assertEquals(270, degrees)
    }

    @Test
    fun `GIVEN any photo WHEN reading the zoom limits THEN it zooms from 1 to 3`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000)

        // When
        val limits = crop.minZoom to crop.maxZoom

        // Then
        assertEquals(1f, limits.first, TOLERANCE)
        assertEquals(3f, limits.second, TOLERANCE)
    }

    @Test
    fun `GIVEN a square photo at zoom 1 WHEN dragging it THEN it does not move`() {
        // Given
        val crop = PhotoCrop.of(1000, 1000)

        // When
        val dragged = crop.toDragged(0.3f, -0.2f)

        // Then
        assertEquals(0f, dragged.offsetX, TOLERANCE)
        assertEquals(0f, dragged.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a wide photo WHEN dragging it within its edges THEN the offset adds up`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).toDragged(0.2f, 0f)

        // When
        val dragged = crop.toDragged(0.1f, 0f)

        // Then
        assertEquals(0.3f, dragged.offsetX, TOLERANCE)
        assertEquals(0f, dragged.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a wide photo WHEN dragging it right past its edge THEN it stops with its left edge on the frame`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000)

        // When
        val dragged = crop.toDragged(5f, 0f)

        // Then
        assertEquals(0.5f, dragged.offsetX, TOLERANCE)
    }

    @Test
    fun `GIVEN a wide photo WHEN dragging it left past its edge THEN it stops with its right edge on the frame`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000)

        // When
        val dragged = crop.toDragged(-5f, 0f)

        // Then
        assertEquals(-0.5f, dragged.offsetX, TOLERANCE)
    }

    @Test
    fun `GIVEN a wide photo at zoom 1 WHEN dragging it up and down THEN it does not move vertically`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000)

        // When
        val dragged = crop.toDragged(0f, 0.4f)

        // Then
        assertEquals(0f, dragged.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a tall photo WHEN dragging it down past its edge THEN it stops with its top edge on the frame`() {
        // Given
        val crop = PhotoCrop.of(1000, 2000)

        // When
        val dragged = crop.toDragged(0.3f, 5f)

        // Then
        assertEquals(0f, dragged.offsetX, TOLERANCE)
        assertEquals(0.5f, dragged.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a tall photo WHEN dragging it up past its edge THEN it stops with its bottom edge on the frame`() {
        // Given
        val crop = PhotoCrop.of(1000, 2000)

        // When
        val dragged = crop.toDragged(0f, -5f)

        // Then
        assertEquals(-0.5f, dragged.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a square photo at zoom 2 WHEN dragging it past a corner THEN both offsets stop at the edges`() {
        // Given
        val crop = PhotoCrop.of(1000, 1000).toZoomed(2f)

        // When
        val dragged = crop.toDragged(5f, -5f)

        // Then
        assertEquals(0.5f, dragged.offsetX, TOLERANCE)
        assertEquals(-0.5f, dragged.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a wide photo turned a quarter WHEN dragging it THEN only the vertical limit lets it move`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).toTurned()

        // When
        val dragged = crop.toDragged(5f, 5f)

        // Then
        assertEquals(0f, dragged.offsetX, TOLERANCE)
        assertEquals(0.5f, dragged.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a tall photo turned three quarters WHEN dragging it THEN only the horizontal limit lets it move`() {
        // Given
        val crop = PhotoCrop
            .of(1000, 2000)
            .toTurned()
            .toTurned()
            .toTurned()

        // When
        val dragged = crop.toDragged(-5f, -5f)

        // Then
        assertEquals(-0.5f, dragged.offsetX, TOLERANCE)
        assertEquals(0f, dragged.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a zoom between the limits WHEN zooming THEN keeps that zoom`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000)

        // When
        val zoomed = crop.toZoomed(2.5f)

        // Then
        assertEquals(2.5f, zoomed.zoom, TOLERANCE)
    }

    @Test
    fun `GIVEN a zoom above 3 WHEN zooming THEN stops at 3`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000)

        // When
        val zoomed = crop.toZoomed(7f)

        // Then
        assertEquals(3f, zoomed.zoom, TOLERANCE)
    }

    @Test
    fun `GIVEN a zoom below 1 WHEN zooming THEN stops at 1`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).toZoomed(2f)

        // When
        val zoomed = crop.toZoomed(0.4f)

        // Then
        assertEquals(1f, zoomed.zoom, TOLERANCE)
    }

    @Test
    fun `GIVEN a square photo in a corner at zoom 3 WHEN zooming out to 2 THEN it comes back to the new edges`() {
        // Given
        val crop = PhotoCrop.of(1000, 1000).toZoomed(3f).toDragged(5f, -5f)

        // When
        val zoomed = crop.toZoomed(2f)

        // Then
        assertEquals(0.5f, zoomed.offsetX, TOLERANCE)
        assertEquals(-0.5f, zoomed.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a square photo dragged at zoom 3 WHEN zooming out to 1 THEN it centers again`() {
        // Given
        val crop = PhotoCrop.of(1000, 1000).toZoomed(3f).toDragged(0.6f, 0.4f)

        // When
        val zoomed = crop.toZoomed(1f)

        // Then
        assertEquals(0f, zoomed.offsetX, TOLERANCE)
        assertEquals(0f, zoomed.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a wide photo at its edge at zoom 3 WHEN zooming out to 2 THEN it comes back to the new edge`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).toZoomed(3f).toDragged(5f, 0f)

        // When
        val zoomed = crop.toZoomed(2f)

        // Then
        assertEquals(1.5f, zoomed.offsetX, TOLERANCE)
    }

    @Test
    fun `GIVEN a photo at zoom 2 WHEN pinching by 1_25 THEN the zoom multiplies to 2_5`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).toZoomed(2f)

        // When
        val zoomed = crop.toZoomedBy(1.25f)

        // Then
        assertEquals(2.5f, zoomed.zoom, TOLERANCE)
    }

    @Test
    fun `GIVEN a photo at zoom 2 WHEN pinching it twice as big THEN the zoom stops at 3`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).toZoomed(2f)

        // When
        val zoomed = crop.toZoomedBy(2f)

        // Then
        assertEquals(3f, zoomed.zoom, TOLERANCE)
    }

    @Test
    fun `GIVEN a photo dragged at zoom 3 WHEN pinching it smaller THEN the offset comes back inside`() {
        // Given
        val crop = PhotoCrop.of(1000, 1000).toZoomed(3f).toDragged(1f, 1f)

        // When
        val zoomed = crop.toZoomedBy(0.5f)

        // Then
        assertEquals(1.5f, zoomed.zoom, TOLERANCE)
        assertEquals(0.25f, zoomed.offsetX, TOLERANCE)
        assertEquals(0.25f, zoomed.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a centered photo WHEN pinching it around a point off center THEN that point stays put`() {
        // Given
        val crop = PhotoCrop.of(1000, 1000)

        // When
        val zoomed = crop.toZoomedBy(
            factor = 2f,
            focusX = 0.25f,
            focusY = 0.25f,
        )

        // Then
        assertEquals(2f, zoomed.zoom, TOLERANCE)
        assertEquals(-0.25f, zoomed.offsetX, TOLERANCE)
        assertEquals(-0.25f, zoomed.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN an upright photo WHEN turning it THEN it is one quarter turn clockwise`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000)

        // When
        val turned = crop.toTurned()

        // Then
        assertEquals(1, turned.quarterTurns)
        assertEquals(90, turned.rotationDegrees)
    }

    @Test
    fun `GIVEN a photo turned three quarters WHEN turning it THEN it is upright again`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).copy(quarterTurns = 3)

        // When
        val turned = crop.toTurned()

        // Then
        assertEquals(0, turned.quarterTurns)
    }

    @Test
    fun `GIVEN a zoomed square photo moved off center WHEN turning it THEN the offset turns clockwise with it`() {
        // Given
        val crop = PhotoCrop.of(1000, 1000).toZoomed(3f).toDragged(0.4f, 0.2f)

        // When
        val turned = crop.toTurned()

        // Then
        assertEquals(-0.2f, turned.offsetX, TOLERANCE)
        assertEquals(0.4f, turned.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a wide photo at its right edge WHEN turning it THEN that edge goes to the bottom covering the frame`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).toDragged(5f, 0f)

        // When
        val turned = crop.toTurned()

        // Then
        assertEquals(0f, turned.offsetX, TOLERANCE)
        assertEquals(0.5f, turned.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a wide photo held past its edge WHEN turning it THEN it moves back in`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).copy(
            offsetX = 0.9f,
            offsetY = 0.3f,
        )

        // When
        val turned = crop.toTurned()

        // Then
        assertEquals(0f, turned.offsetX, TOLERANCE)
        assertEquals(0.5f, turned.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a zoomed photo moved off center WHEN turning it four times THEN it is back where it was`() {
        // Given
        val crop = PhotoCrop.of(3000, 2000).toZoomed(2f).toDragged(0.7f, -0.3f)

        // When
        val turned = crop
            .toTurned()
            .toTurned()
            .toTurned()
            .toTurned()

        // Then
        assertSameCrop(crop, turned)
    }

    @Test
    fun `GIVEN a photo off center WHEN mirroring it horizontally THEN it is mirrored on the other side`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).toZoomed(2f).toDragged(0.6f, 0.3f)

        // When
        val mirrored = crop.toMirroredHorizontally()

        // Then
        assertTrue(mirrored.isMirrored)
        assertEquals(0, mirrored.quarterTurns)
        assertEquals(-0.6f, mirrored.offsetX, TOLERANCE)
        assertEquals(0.3f, mirrored.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a mirrored photo WHEN mirroring it horizontally THEN it is no longer mirrored`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).copy(isMirrored = true)

        // When
        val mirrored = crop.toMirroredHorizontally()

        // Then
        assertFalse(mirrored.isMirrored)
    }

    @Test
    fun `GIVEN a photo turned a quarter WHEN mirroring it horizontally THEN it reads as three quarter turns`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).toTurned()

        // When
        val mirrored = crop.toMirroredHorizontally()

        // Then
        assertEquals(3, mirrored.quarterTurns)
        assertTrue(mirrored.isMirrored)
    }

    @Test
    fun `GIVEN a photo turned half WHEN mirroring it horizontally THEN it stays turned half`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).toTurned().toTurned()

        // When
        val mirrored = crop.toMirroredHorizontally()

        // Then
        assertEquals(2, mirrored.quarterTurns)
    }

    @Test
    fun `GIVEN a turned photo off center WHEN mirroring it horizontally twice THEN it is back as it was`() {
        // Given
        val crop = PhotoCrop
            .of(3000, 2000)
            .toZoomed(2f)
            .toDragged(0.7f, -0.3f)
            .toTurned()

        // When
        val mirrored = crop.toMirroredHorizontally().toMirroredHorizontally()

        // Then
        assertSameCrop(crop, mirrored)
    }

    @Test
    fun `GIVEN a photo off center WHEN mirroring it vertically THEN it is mirrored turned half on the other side`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).toZoomed(2f).toDragged(0.6f, 0.3f)

        // When
        val mirrored = crop.toMirroredVertically()

        // Then
        assertTrue(mirrored.isMirrored)
        assertEquals(2, mirrored.quarterTurns)
        assertEquals(0.6f, mirrored.offsetX, TOLERANCE)
        assertEquals(-0.3f, mirrored.offsetY, TOLERANCE)
    }

    @Test
    fun `GIVEN a photo turned a quarter WHEN mirroring it vertically THEN it stays a quarter turn`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).toTurned()

        // When
        val mirrored = crop.toMirroredVertically()

        // Then
        assertEquals(1, mirrored.quarterTurns)
        assertTrue(mirrored.isMirrored)
    }

    @Test
    fun `GIVEN a photo turned three quarters WHEN mirroring it vertically THEN it stays three quarter turns`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).copy(quarterTurns = 3)

        // When
        val mirrored = crop.toMirroredVertically()

        // Then
        assertEquals(3, mirrored.quarterTurns)
    }

    @Test
    fun `GIVEN a photo turned half WHEN mirroring it vertically THEN it is upright`() {
        // Given
        val crop = PhotoCrop.of(2000, 1000).copy(quarterTurns = 2)

        // When
        val mirrored = crop.toMirroredVertically()

        // Then
        assertEquals(0, mirrored.quarterTurns)
    }

    @Test
    fun `GIVEN a turned photo off center WHEN mirroring it vertically twice THEN it is back as it was`() {
        // Given
        val crop = PhotoCrop
            .of(3000, 2000)
            .toZoomed(2f)
            .toDragged(0.7f, -0.3f)
            .toTurned()

        // When
        val mirrored = crop.toMirroredVertically().toMirroredVertically()

        // Then
        assertSameCrop(crop, mirrored)
    }

    @Test
    fun `GIVEN a photo WHEN mirroring it horizontally and then vertically THEN it is turned half and not mirrored`() {
        // Given
        val crop = PhotoCrop.of(3000, 2000).toZoomed(2f).toDragged(0.7f, -0.3f)

        // When
        val mirrored = crop.toMirroredHorizontally().toMirroredVertically()

        // Then
        assertFalse(mirrored.isMirrored)
        assertEquals(2, mirrored.quarterTurns)
        assertEquals(-0.7f, mirrored.offsetX, TOLERANCE)
        assertEquals(0.3f, mirrored.offsetY, TOLERANCE)
    }

    private fun assertSameCrop(
        expected: PhotoCrop,
        actual: PhotoCrop,
    ) {
        assertEquals(expected.photoWidth, actual.photoWidth)
        assertEquals(expected.photoHeight, actual.photoHeight)
        assertEquals(expected.quarterTurns, actual.quarterTurns)
        assertEquals(expected.isMirrored, actual.isMirrored)
        assertEquals(expected.zoom, actual.zoom, TOLERANCE)
        assertEquals(expected.offsetX, actual.offsetX, TOLERANCE)
        assertEquals(expected.offsetY, actual.offsetY, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1e-5f
    }
}
