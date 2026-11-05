package com.quare.blitzsplit.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TypoTolerantQueryTest {
    @Test
    fun `GIVEN an empty query WHEN searching a name THEN the query is empty and finds nothing`() {
        // Given
        val query = TypoTolerantQuery.of("")

        // When
        val found = query.findIn("Larissa")

        // Then
        assertTrue(query.isEmpty)
        assertNull(found)
    }

    @Test
    fun `GIVEN a query of only spaces WHEN searching a name THEN the query is empty and finds nothing`() {
        // Given
        val query = TypoTolerantQuery.of("   ")

        // When
        val found = query.findIn("Larissa Silva")

        // Then
        assertTrue(query.isEmpty)
        assertNull(found)
    }

    @Test
    fun `GIVEN a typed name WHEN reading whether the query is empty THEN it is not`() {
        // Given
        val query = TypoTolerantQuery.of("larisa")

        // When
        val isEmpty = query.isEmpty

        // Then
        assertFalse(isEmpty)
    }

    @Test
    fun `GIVEN a query of only punctuation WHEN searching a hyphenated name THEN finds nothing`() {
        // Given
        val query = TypoTolerantQuery.of("--")

        // When
        val found = query.findIn("Ana-Clara")

        // Then
        assertNull(found)
    }

    @Test
    fun `GIVEN a query without its accent WHEN searching an accented group THEN finds it exactly at its start`() {
        // Given
        val query = TypoTolerantQuery.of("ape")

        // When
        val found = query.findIn("Apê 302")

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 0,
                end = 3,
                typos = 0,
            ),
            found,
        )
        assertTrue(found?.isExact == true)
    }

    @Test
    fun `GIVEN an upper case query with an accent WHEN searching a plain group THEN finds it exactly`() {
        // Given
        val query = TypoTolerantQuery.of("CHÚR")

        // When
        val found = query.findIn("Churrasco")

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 0,
                end = 4,
                typos = 0,
            ),
            found,
        )
    }

    @Test
    fun `GIVEN a query with spaces around it WHEN searching the name THEN finds it as if trimmed`() {
        // Given
        val query = TypoTolerantQuery.of("  larissa  ")

        // When
        val found = query.findIn("Larissa")

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 0,
                end = 7,
                typos = 0,
            ),
            found,
        )
    }

    @Test
    fun `GIVEN a query inside a word WHEN searching the name THEN finds exactly the letters typed`() {
        // Given
        val query = TypoTolerantQuery.of("rissa")

        // When
        val found = query.findIn("Larissa")

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 2,
                end = 7,
                typos = 0,
            ),
            found,
        )
    }

    @Test
    fun `GIVEN the words of a name in another order WHEN searching it THEN finds the whole words with no typo`() {
        // Given
        val query = TypoTolerantQuery.of("silva lari")

        // When
        val found = query.findIn("Larissa Silva")

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 0,
                end = 13,
                typos = 0,
            ),
            found,
        )
        assertTrue(found?.isExact == true)
    }

    @Test
    fun `GIVEN a name missing a letter WHEN searching the full name THEN finds the whole word with one typo`() {
        // Given
        val query = TypoTolerantQuery.of("larisa")

        // When
        val found = query.findIn("Larissa")

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 0,
                end = 7,
                typos = 1,
            ),
            found,
        )
        assertFalse(found?.isExact ?: true)
    }

    @Test
    fun `GIVEN two adjacent letters swapped in a name being typed WHEN searching THEN finds the name with one typo`() {
        // Given
        val query = TypoTolerantQuery.of("larsi")

        // When
        val found = query.findIn("Larissa")

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 0,
                end = 7,
                typos = 1,
            ),
            found,
        )
    }

    @Test
    fun `GIVEN a 3 letter name one letter off WHEN searching THEN finds nothing`() {
        // Given
        val query = TypoTolerantQuery.of("ana")

        // When
        val found = query.findIn("Ane")

        // Then
        assertNull(found)
    }

    @Test
    fun `GIVEN a 4 letter query one letter off WHEN searching THEN finds the name with one typo`() {
        // Given
        val query = TypoTolerantQuery.of("mara")

        // When
        val found = query.findIn("Maria")

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 0,
                end = 5,
                typos = 1,
            ),
            found,
        )
    }

    @Test
    fun `GIVEN a 6 letter name two letters off WHEN searching THEN finds nothing`() {
        // Given
        val query = TypoTolerantQuery.of("lerisa")

        // When
        val found = query.findIn("Larissa")

        // Then
        assertNull(found)
    }

    @Test
    fun `GIVEN an 8 letter query two letters off WHEN searching THEN finds the group with two typos`() {
        // Given
        val query = TypoTolerantQuery.of("churasko")

        // When
        val found = query.findIn("Churrasco")

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 0,
                end = 9,
                typos = 2,
            ),
            found,
        )
    }

    @Test
    fun `GIVEN a long word missing two letters WHEN searching THEN finds the group with two typos`() {
        // Given
        val query = TypoTolerantQuery.of("churasqeira")

        // When
        val found = query.findIn("Churrasqueira")

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 0,
                end = 13,
                typos = 2,
            ),
            found,
        )
    }

    @Test
    fun `GIVEN two words each with a typo WHEN searching the name THEN adds up the typos of both words`() {
        // Given
        val query = TypoTolerantQuery.of("slva larisa")

        // When
        val found = query.findIn("Larissa Silva")

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 0,
                end = 13,
                typos = 2,
            ),
            found,
        )
    }

    @Test
    fun `GIVEN two words one of them too far WHEN searching the name THEN finds nothing`() {
        // Given
        val query = TypoTolerantQuery.of("silva lerisa")

        // When
        val found = query.findIn("Larissa Silva")

        // Then
        assertNull(found)
    }

    @Test
    fun `GIVEN an accented name after a title WHEN searching it plain THEN marks the name in the original text`() {
        // Given
        val query = TypoTolerantQuery.of("alvaro")
        val text = "Dr. Álvaro"

        // When
        val found = query.findIn(text)

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 4,
                end = 10,
                typos = 0,
            ),
            found,
        )
        assertEquals("Álvaro", found?.let { text.substring(it.start, it.end) })
    }

    @Test
    fun `GIVEN an accented name typed plain with a typo WHEN searching THEN marks the name in the original text`() {
        // Given
        val query = TypoTolerantQuery.of("alvro")
        val text = "Dr. Álvaro"

        // When
        val found = query.findIn(text)

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 4,
                end = 10,
                typos = 1,
            ),
            found,
        )
        assertEquals("Álvaro", found?.let { text.substring(it.start, it.end) })
    }

    @Test
    fun `GIVEN a text with no words WHEN searching a name THEN finds nothing`() {
        // Given
        val query = TypoTolerantQuery.of("larisa")

        // When
        val found = query.findIn("--- !!!")

        // Then
        assertNull(found)
    }

    @Test
    fun `GIVEN two words that both qualify WHEN searching THEN picks the one with fewer typos`() {
        // Given
        val query = TypoTolerantQuery.of("churascaria")

        // When
        val found = query.findIn("Churrasceria Churrascaria")

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 13,
                end = 25,
                typos = 1,
            ),
            found,
        )
    }

    @Test
    fun `GIVEN a query without typos WHEN searching a name one letter off THEN finds nothing`() {
        // Given
        val query = TypoTolerantQuery.of("larisa").withoutTypos()

        // When
        val found = query.findIn("Larissa")

        // Then
        assertNull(found)
    }

    @Test
    fun `GIVEN a query without typos WHEN searching the words of a name in another order THEN still finds them`() {
        // Given
        val query = TypoTolerantQuery.of("silva lari").withoutTypos()

        // When
        val found = query.findIn("Larissa Silva")

        // Then
        assertEquals(
            TypoTolerantQuery.Found(
                start = 0,
                end = 13,
                typos = 0,
            ),
            found,
        )
    }
}
