package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.Initials
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InitialsTest {
    @Test
    fun `GIVEN a name of two words WHEN taking the initials THEN gives the first letter of each`() {
        // Given
        val name = "Viagem Floripa"

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("VF", initials)
    }

    @Test
    fun `GIVEN a joining word between two words WHEN taking the initials THEN skips it`() {
        // Given
        val name = "Casa da Praia"

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("CP", initials)
    }

    @Test
    fun `GIVEN English joining words WHEN taking the initials THEN skips them too`() {
        // Given
        val name = "Pizza and the Movies"

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("PM", initials)
    }

    @Test
    fun `GIVEN a joining word in upper case WHEN taking the initials THEN still skips it`() {
        // Given
        val name = "Churrasco DE Sabado"

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("CS", initials)
    }

    @Test
    fun `GIVEN more than two meaningful words WHEN taking the initials THEN uses only the first two`() {
        // Given
        val name = "Viagem Floripa Carnaval"

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("VF", initials)
    }

    @Test
    fun `GIVEN a single word WHEN taking the initials THEN gives its first two letters`() {
        // Given
        val name = "Ana"

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("AN", initials)
    }

    @Test
    fun `GIVEN lower case words WHEN taking the initials THEN upper cases them`() {
        // Given
        val name = "viagem floripa"

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("VF", initials)
    }

    @Test
    fun `GIVEN extra spaces around and between the words WHEN taking the initials THEN ignores them`() {
        // Given
        val name = "  Viagem    Floripa  "

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("VF", initials)
    }

    @Test
    fun `GIVEN an empty name WHEN taking the initials THEN gives none`() {
        // Given
        val name = ""

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("", initials)
    }

    @Test
    fun `GIVEN only spaces WHEN taking the initials THEN gives none`() {
        // Given
        val name = "   "

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("", initials)
    }

    @Test
    fun `GIVEN a single letter WHEN taking the initials THEN gives that letter`() {
        // Given
        val name = "a"

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("A", initials)
    }

    @Test
    fun `GIVEN one meaningful word after a joining word WHEN taking the initials THEN gives its first two letters`() {
        // Given
        val name = "The Office"

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("OF", initials)
    }

    @Test
    fun `GIVEN only joining words WHEN taking the initials THEN uses them as the words`() {
        // Given
        val name = "de e"

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("DE", initials)
    }

    @Test
    fun `GIVEN an emoji before a word WHEN taking the initials THEN keeps the whole emoji as one letter`() {
        // Given
        val name = "\uD83D\uDE00 Ana"

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("\uD83D\uDE00A", initials)
    }

    @Test
    fun `GIVEN an emoji with a variation selector WHEN taking the initials THEN never splits the emoji`() {
        // Given
        val name = "\uD83C\uDFD6\uFE0F Praia"

        // When
        val initials = Initials.of(name)

        // Then
        assertTrue(initials.startsWith("\uD83C\uDFD6"))
        assertTrue(initials.endsWith("P"))
        assertFalse(hasHalfAPair(initials))
    }

    @Test
    fun `GIVEN one word starting with an emoji WHEN taking the initials THEN gives the emoji and a letter`() {
        // Given
        val name = "\uD83D\uDE00ana"

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("\uD83D\uDE00A", initials)
    }

    @Test
    fun `GIVEN a name of only one emoji WHEN taking the initials THEN gives that emoji whole`() {
        // Given
        val name = "\uD83D\uDE00"

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("\uD83D\uDE00", initials)
    }

    @Test
    fun `GIVEN a name in lower case WHEN taking its first letter THEN gives it in upper case`() {
        // Given
        val name = "ana"

        // When
        val letter = Initials.firstOf(name)

        // Then
        assertEquals("A", letter)
    }

    @Test
    fun `GIVEN a name of two words with spaces around WHEN taking its first letter THEN gives the first letter only`() {
        // Given
        val name = "  Ana Paula "

        // When
        val letter = Initials.firstOf(name)

        // Then
        assertEquals("A", letter)
    }

    @Test
    fun `GIVEN an empty name WHEN taking its first letter THEN gives none`() {
        // Given
        val name = ""

        // When
        val letter = Initials.firstOf(name)

        // Then
        assertEquals("", letter)
    }

    @Test
    fun `GIVEN only spaces WHEN taking the first letter THEN gives none`() {
        // Given
        val name = "   "

        // When
        val letter = Initials.firstOf(name)

        // Then
        assertEquals("", letter)
    }

    @Test
    fun `GIVEN a name starting with an emoji WHEN taking its first letter THEN gives the whole emoji`() {
        // Given
        val name = "\uD83D\uDE00 Pierre"

        // When
        val letter = Initials.firstOf(name)

        // Then
        assertEquals("\uD83D\uDE00", letter)
    }

    private fun hasHalfAPair(text: String): Boolean = text.indices.any { index ->
        val char = text[index]
        when {
            char.isHighSurrogate() -> index + 1 >= text.length || !text[index + 1].isLowSurrogate()
            char.isLowSurrogate() -> index == 0 || !text[index - 1].isHighSurrogate()
            else -> false
        }
    }
}
