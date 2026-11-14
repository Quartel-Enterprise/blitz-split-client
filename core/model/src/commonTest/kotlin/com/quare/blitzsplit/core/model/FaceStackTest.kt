package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FaceStackTest {
    @Test
    fun `GIVEN nobody WHEN stacking the faces THEN shows no face and no more bubble`() {
        // Given
        val peopleCount = 0

        // When
        val faces = FaceStack.faceCountOf(peopleCount)
        val extra = FaceStack.extraCountOf(peopleCount)

        // Then
        assertEquals(0, faces)
        assertEquals(0, extra)
    }

    @Test
    fun `GIVEN one person WHEN stacking the faces THEN shows their face and no more bubble`() {
        // Given
        val peopleCount = 1

        // When
        val faces = FaceStack.faceCountOf(peopleCount)
        val extra = FaceStack.extraCountOf(peopleCount)

        // Then
        assertEquals(1, faces)
        assertEquals(0, extra)
    }

    @Test
    fun `GIVEN two people WHEN stacking the faces THEN shows both faces and no more bubble`() {
        // Given
        val peopleCount = 2

        // When
        val faces = FaceStack.faceCountOf(peopleCount)
        val extra = FaceStack.extraCountOf(peopleCount)

        // Then
        assertEquals(2, faces)
        assertEquals(0, extra)
    }

    @Test
    fun `GIVEN three people WHEN stacking the faces THEN shows the first face and two more`() {
        // Given
        val peopleCount = 3

        // When
        val faces = FaceStack.faceCountOf(peopleCount)
        val extra = FaceStack.extraCountOf(peopleCount)

        // Then
        assertEquals(1, faces)
        assertEquals(2, extra)
    }

    @Test
    fun `GIVEN ten people WHEN stacking the faces THEN shows the first face and nine more`() {
        // Given
        val peopleCount = 10

        // When
        val faces = FaceStack.faceCountOf(peopleCount)
        val extra = FaceStack.extraCountOf(peopleCount)

        // Then
        assertEquals(1, faces)
        assertEquals(9, extra)
    }

    @Test
    fun `GIVEN any count of people WHEN stacking the faces THEN shows at most two bubbles`() {
        // Given
        val counts = 0..50

        // When
        val bubbles = counts.map { count ->
            FaceStack.faceCountOf(count) + if (FaceStack.extraCountOf(count) > 0) 1 else 0
        }

        // Then
        assertEquals(emptyList(), bubbles.filter { it > FaceStack.MAX_BUBBLES })
    }

    @Test
    fun `GIVEN three or more people WHEN stacking the faces THEN the face and the more bubble add up to everyone`() {
        // Given
        val counts = 3..50

        // When
        val totals = counts.map { FaceStack.faceCountOf(it) + FaceStack.extraCountOf(it) }

        // Then
        assertEquals(counts.toList(), totals)
    }

    @Test
    fun `GIVEN a negative count WHEN stacking the faces THEN shows no face and no more bubble`() {
        // Given
        val peopleCount = -3

        // When
        val faces = FaceStack.faceCountOf(peopleCount)
        val extra = FaceStack.extraCountOf(peopleCount)

        // Then
        assertEquals(0, faces)
        assertEquals(0, extra)
    }
}
