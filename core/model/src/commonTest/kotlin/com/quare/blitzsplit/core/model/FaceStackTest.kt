package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FaceStackTest {
    @Test
    fun `GIVEN nobody WHEN stacking the faces THEN shows no face and no more bubble`() {
        // Given
        val peopleCount = 0

        // When
        val faces = FaceStack.COMPACT.faceCountOf(peopleCount)
        val extra = FaceStack.COMPACT.extraCountOf(peopleCount)

        // Then
        assertEquals(0, faces)
        assertEquals(0, extra)
    }

    @Test
    fun `GIVEN one person WHEN stacking the faces in a row THEN shows their face and no more bubble`() {
        // Given
        val peopleCount = 1

        // When
        val faces = FaceStack.COMPACT.faceCountOf(peopleCount)
        val extra = FaceStack.COMPACT.extraCountOf(peopleCount)

        // Then
        assertEquals(1, faces)
        assertEquals(0, extra)
    }

    @Test
    fun `GIVEN two people WHEN stacking the faces in a row THEN shows both faces and no more bubble`() {
        // Given
        val peopleCount = 2

        // When
        val faces = FaceStack.COMPACT.faceCountOf(peopleCount)
        val extra = FaceStack.COMPACT.extraCountOf(peopleCount)

        // Then
        assertEquals(2, faces)
        assertEquals(0, extra)
    }

    @Test
    fun `GIVEN three people WHEN stacking the faces in a row THEN shows the first face and two more`() {
        // Given
        val peopleCount = 3

        // When
        val faces = FaceStack.COMPACT.faceCountOf(peopleCount)
        val extra = FaceStack.COMPACT.extraCountOf(peopleCount)

        // Then
        assertEquals(1, faces)
        assertEquals(2, extra)
    }

    @Test
    fun `GIVEN ten people WHEN stacking the faces in a row THEN shows the first face and nine more`() {
        // Given
        val peopleCount = 10

        // When
        val faces = FaceStack.COMPACT.faceCountOf(peopleCount)
        val extra = FaceStack.COMPACT.extraCountOf(peopleCount)

        // Then
        assertEquals(1, faces)
        assertEquals(9, extra)
    }

    @Test
    fun `GIVEN five people WHEN stacking the faces where there is room THEN shows the five faces and no more bubble`() {
        // Given
        val peopleCount = 5

        // When
        val faces = FaceStack.ROOMY.faceCountOf(peopleCount)
        val extra = FaceStack.ROOMY.extraCountOf(peopleCount)

        // Then
        assertEquals(5, faces)
        assertEquals(0, extra)
    }

    @Test
    fun `GIVEN six people WHEN stacking the faces where there is room THEN shows four faces and two more`() {
        // Given
        val peopleCount = 6

        // When
        val faces = FaceStack.ROOMY.faceCountOf(peopleCount)
        val extra = FaceStack.ROOMY.extraCountOf(peopleCount)

        // Then
        assertEquals(4, faces)
        assertEquals(2, extra)
    }

    @Test
    fun `GIVEN any count of people WHEN stacking the faces THEN shows at most as many bubbles as the stack holds`() {
        // Given
        val counts = 0..50

        // When
        val overflowing = FaceStack.entries.flatMap { stack ->
            counts.filter { count ->
                stack.faceCountOf(count) + (if (stack.extraCountOf(count) > 0) 1 else 0) > stack.maxBubbles
            }
        }

        // Then
        assertEquals(emptyList(), overflowing)
    }

    @Test
    fun `GIVEN any count of people WHEN stacking the faces THEN the faces and the more bubble add up to everyone`() {
        // Given
        val counts = 0..50

        // When
        val totals = FaceStack.entries.map { stack ->
            counts.map { stack.faceCountOf(it) + stack.extraCountOf(it) }
        }

        // Then
        assertEquals(FaceStack.entries.map { counts.toList() }, totals)
    }

    @Test
    fun `GIVEN any count of people WHEN stacking the faces THEN the more bubble never stands for one person`() {
        // Given
        val counts = 0..50

        // When
        val extras = FaceStack.entries.flatMap { stack -> counts.map(stack::extraCountOf) }

        // Then
        assertEquals(emptyList(), extras.filter { it == 1 })
    }

    @Test
    fun `GIVEN a negative count WHEN stacking the faces THEN shows no face and no more bubble`() {
        // Given
        val peopleCount = -3

        // When
        val faces = FaceStack.COMPACT.faceCountOf(peopleCount)
        val extra = FaceStack.COMPACT.extraCountOf(peopleCount)

        // Then
        assertEquals(0, faces)
        assertEquals(0, extra)
    }
}
