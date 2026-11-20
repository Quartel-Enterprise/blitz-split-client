package com.quare.blitzsplit.core.watch.domain

import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCodeFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatchPairingCodeFormatTest {
    @Test
    fun `GIVEN a code typed in lower case WHEN normalizing THEN returns it in upper case`() {
        // Given
        val typed = "k7p4qx"

        // When
        val code = WatchPairingCodeFormat.normalize(typed)

        // Then
        assertEquals("K7P4QX", code)
    }

    @Test
    fun `GIVEN a code typed as shown with a space WHEN normalizing THEN drops the space`() {
        // Given
        val typed = "K7P 4QX"

        // When
        val code = WatchPairingCodeFormat.normalize(typed)

        // Then
        assertEquals("K7P4QX", code)
    }

    @Test
    fun `GIVEN a code typed with dashes and spaces around WHEN normalizing THEN keeps only its letters and digits`() {
        // Given
        val typed = " k7p-4qx "

        // When
        val code = WatchPairingCodeFormat.normalize(typed)

        // Then
        assertEquals("K7P4QX", code)
    }

    @Test
    fun `GIVEN more than six letters and digits WHEN normalizing THEN keeps the first six`() {
        // Given
        val typed = "K7P4QXZ9"

        // When
        val code = WatchPairingCodeFormat.normalize(typed)

        // Then
        assertEquals("K7P4QX", code)
    }

    @Test
    fun `GIVEN letters and digits of another script WHEN normalizing THEN keeps only A to Z and 0 to 9`() {
        // Given
        val typed = "K٧PÉ4QX9"

        // When
        val code = WatchPairingCodeFormat.normalize(typed)

        // Then
        assertEquals("KP4QX9", code)
    }

    @Test
    fun `GIVEN nothing typed WHEN normalizing THEN returns an empty code`() {
        // Given
        val typed = ""

        // When
        val code = WatchPairingCodeFormat.normalize(typed)

        // Then
        assertEquals("", code)
    }

    @Test
    fun `GIVEN six letters and digits with a space WHEN checking the code THEN is complete`() {
        // Given
        val typed = "k7p 4qx"

        // When
        val isComplete = WatchPairingCodeFormat.isComplete(typed)

        // Then
        assertTrue(isComplete)
    }

    @Test
    fun `GIVEN only five letters and digits WHEN checking the code THEN is not complete`() {
        // Given
        val typed = "K7P 4Q"

        // When
        val isComplete = WatchPairingCodeFormat.isComplete(typed)

        // Then
        assertFalse(isComplete)
    }

    @Test
    fun `GIVEN five letters and digits padded with dashes WHEN checking the code THEN is not complete`() {
        // Given
        val typed = "K7P-4Q-"

        // When
        val isComplete = WatchPairingCodeFormat.isComplete(typed)

        // Then
        assertFalse(isComplete)
    }
}
