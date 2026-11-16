package com.quare.blitzsplit.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecoveryWordsTest {
    private val validCode = "legal winner thank year wave sausage worth useful legal winner thank yellow"

    @Test
    fun `GIVEN the word list WHEN reading it THEN holds 2048 distinct words with distinct first four letters`() {
        // Given
        val list = BIP39_ENGLISH

        // When
        val prefixes = list.map { it.take(4) }.toSet()

        // Then
        assertEquals(2048, list.size)
        assertEquals(2048, list.toSet().size)
        assertEquals(2048, prefixes.size)
        assertEquals("abandon", list.first())
        assertEquals("zoo", list.last())
    }

    @Test
    fun `GIVEN a numbered list with capitals WHEN splitting THEN returns the lowercase words without the numbers`() {
        // Given
        val pasted = "1. Legal 2. winner"

        // When
        val words = RecoveryWords.split(pasted)

        // Then
        assertEquals(listOf("legal", "winner"), words)
    }

    @Test
    fun `GIVEN commas new lines and spaces WHEN splitting THEN each separates words`() {
        // Given
        val pasted = "legal,winner\nthank  year,\n wave"

        // When
        val words = RecoveryWords.split(pasted)

        // Then
        assertEquals(listOf("legal", "winner", "thank", "year", "wave"), words)
    }

    @Test
    fun `GIVEN the words of a QR link WHEN splitting THEN the plus and the encoded space separate words`() {
        // Given
        val link = "legal+winner%20thank"

        // When
        val words = RecoveryWords.split(link)

        // Then
        assertEquals(listOf("legal", "winner", "thank"), words)
    }

    @Test
    fun `GIVEN only digits and symbols WHEN splitting THEN returns no words`() {
        // Given
        val typed = " 12, 34. - "

        // When
        val words = RecoveryWords.split(typed)

        // Then
        assertTrue(words.isEmpty())
    }

    @Test
    fun `GIVEN a mistyped ending in capitals WHEN reading a word THEN returns the word of its first four letters`() {
        // Given
        val typed = "Abandom"

        // When
        val word = RecoveryWords.wordOf(typed)

        // Then
        assertEquals("abandon", word)
    }

    @Test
    fun `GIVEN extra letters after the word WHEN reading a word THEN returns the word of its first four letters`() {
        // Given
        val typed = "abandonxyz"

        // When
        val word = RecoveryWords.wordOf(typed)

        // Then
        assertEquals("abandon", word)
    }

    @Test
    fun `GIVEN only the first four letters WHEN reading a word THEN returns the full word`() {
        // Given
        val typed = "aban"

        // When
        val word = RecoveryWords.wordOf(typed)

        // Then
        assertEquals("abandon", word)
    }

    @Test
    fun `GIVEN a whole three letter word WHEN reading a word THEN returns that word`() {
        // Given
        val typed = "act"

        // When
        val word = RecoveryWords.wordOf(typed)

        // Then
        assertEquals("act", word)
    }

    @Test
    fun `GIVEN the last word of the list WHEN reading a word THEN returns it`() {
        // Given
        val typed = "zoo"

        // When
        val word = RecoveryWords.wordOf(typed)

        // Then
        assertEquals("zoo", word)
    }

    @Test
    fun `GIVEN a word padded with spaces and a number WHEN reading a word THEN returns the word`() {
        // Given
        val typed = " 3. Thank "

        // When
        val word = RecoveryWords.wordOf(typed)

        // Then
        assertEquals("thank", word)
    }

    @Test
    fun `GIVEN two letters WHEN reading a word THEN returns no word`() {
        // Given
        val typed = "ab"

        // When
        val word = RecoveryWords.wordOf(typed)

        // Then
        assertNull(word)
    }

    @Test
    fun `GIVEN three letters that are not a whole word WHEN reading a word THEN returns no word`() {
        // Given
        val typed = "aba"

        // When
        val word = RecoveryWords.wordOf(typed)

        // Then
        assertNull(word)
    }

    @Test
    fun `GIVEN four letters no word starts with WHEN reading a word THEN returns no word`() {
        // Given
        val typed = "qqqq"

        // When
        val word = RecoveryWords.wordOf(typed)

        // Then
        assertNull(word)
    }

    @Test
    fun `GIVEN two words WHEN reading a word THEN returns no word`() {
        // Given
        val typed = "legal winner"

        // When
        val word = RecoveryWords.wordOf(typed)

        // Then
        assertNull(word)
    }

    @Test
    fun `GIVEN a blank field WHEN reading a word THEN returns no word`() {
        // Given
        val typed = "   "

        // When
        val word = RecoveryWords.wordOf(typed)

        // Then
        assertNull(word)
    }

    @Test
    fun `GIVEN a blank field WHEN checking for a dead end THEN it is not one`() {
        // Given
        val typed = " "

        // When
        val deadEnd = RecoveryWords.isDeadEnd(typed)

        // Then
        assertFalse(deadEnd)
    }

    @Test
    fun `GIVEN the start of a word WHEN checking for a dead end THEN it is not one`() {
        // Given
        val typed = "ab"

        // When
        val deadEnd = RecoveryWords.isDeadEnd(typed)

        // Then
        assertFalse(deadEnd)
    }

    @Test
    fun `GIVEN a mistyped ending after four good letters WHEN checking for a dead end THEN it is not one`() {
        // Given
        val typed = "abandom"

        // When
        val deadEnd = RecoveryWords.isDeadEnd(typed)

        // Then
        assertFalse(deadEnd)
    }

    @Test
    fun `GIVEN letters no word starts with WHEN checking for a dead end THEN it is one`() {
        // Given
        val typed = "abx"

        // When
        val deadEnd = RecoveryWords.isDeadEnd(typed)

        // Then
        assertTrue(deadEnd)
    }

    @Test
    fun `GIVEN letters of no word at all WHEN checking for a dead end THEN it is one`() {
        // Given
        val typed = "qqq"

        // When
        val deadEnd = RecoveryWords.isDeadEnd(typed)

        // Then
        assertTrue(deadEnd)
    }

    @Test
    fun `GIVEN letters many words start with WHEN suggesting THEN returns the first three in list order`() {
        // Given
        val typed = "ab"

        // When
        val suggestions = RecoveryWords.suggestionsFor(typed)

        // Then
        assertEquals(listOf("abandon", "ability", "able"), suggestions)
    }

    @Test
    fun `GIVEN letters in capitals WHEN suggesting THEN matches the lowercase words`() {
        // Given
        val typed = "ZE"

        // When
        val suggestions = RecoveryWords.suggestionsFor(typed)

        // Then
        assertEquals(listOf("zebra", "zero"), suggestions)
    }

    @Test
    fun `GIVEN letters no word starts with WHEN suggesting THEN returns nothing`() {
        // Given
        val typed = "qqq"

        // When
        val suggestions = RecoveryWords.suggestionsFor(typed)

        // Then
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `GIVEN a blank field WHEN suggesting THEN returns nothing`() {
        // Given
        val typed = "  "

        // When
        val suggestions = RecoveryWords.suggestionsFor(typed)

        // Then
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `GIVEN two words WHEN suggesting THEN returns nothing`() {
        // Given
        val typed = "ab ac"

        // When
        val suggestions = RecoveryWords.suggestionsFor(typed)

        // Then
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `GIVEN a valid code WHEN normalizing THEN returns the same twelve words`() {
        // Given
        val typed = validCode

        // When
        val normalized = RecoveryWords.normalize(typed)

        // Then
        assertEquals(validCode, normalized)
    }

    @Test
    fun `GIVEN a pasted numbered list with prefixes WHEN normalizing THEN returns the full words joined by spaces`() {
        // Given
        val pasted = "1. LEGAL, 2. winn\n3. Thank 4. year 5. wave 6. sausage\n" +
            "7. wort 8. usef 9. legal 10. winner 11. thank 12. yell"

        // When
        val normalized = RecoveryWords.normalize(pasted)

        // Then
        assertEquals(validCode, normalized)
    }

    @Test
    fun `GIVEN eleven words WHEN normalizing THEN returns nothing`() {
        // Given
        val typed = validCode.substringBeforeLast(" ")

        // When
        val normalized = RecoveryWords.normalize(typed)

        // Then
        assertNull(normalized)
    }

    @Test
    fun `GIVEN thirteen words WHEN normalizing THEN returns nothing`() {
        // Given
        val typed = "$validCode zoo"

        // When
        val normalized = RecoveryWords.normalize(typed)

        // Then
        assertNull(normalized)
    }

    @Test
    fun `GIVEN twelve words with one not in the list WHEN normalizing THEN returns nothing`() {
        // Given
        val typed = validCode.replace("sausage", "qwerty")

        // When
        val normalized = RecoveryWords.normalize(typed)

        // Then
        assertNull(normalized)
    }

    @Test
    fun `GIVEN a blank code WHEN normalizing THEN returns nothing`() {
        // Given
        val typed = ""

        // When
        val normalized = RecoveryWords.normalize(typed)

        // Then
        assertNull(normalized)
    }

    @Test
    fun `GIVEN a stored code of words WHEN reading its words THEN returns the twelve words in order`() {
        // Given
        val stored = validCode

        // When
        val words = RecoveryWords.wordsOf(stored)

        // Then
        assertEquals(validCode.split(" "), words)
        assertEquals(RecoveryWords.COUNT, words?.size)
    }

    @Test
    fun `GIVEN an old base32 code WHEN reading its words THEN returns no words`() {
        // Given
        val stored = "K7QM4XPA9TRE"

        // When
        val words = RecoveryWords.wordsOf(stored)

        // Then
        assertNull(words)
    }
}
