package com.quare.blitzsplit.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SecureRandomCodeTest {
    private lateinit var randomCode: SecureRandomCode
    private var batchesRead = 0

    @Test
    fun `GIVEN enough random bytes WHEN making a code THEN returns exactly that many characters`() {
        // Given
        prepareScenario(bytes(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13))
        val length = 8

        // When
        val code = randomCode(length)

        // Then
        assertEquals("ABCDEFGH", code)
    }

    @Test
    fun `GIVEN a length of zero WHEN making a code THEN returns an empty code`() {
        // Given
        prepareScenario(bytes(0, 1, 2))
        val length = 0

        // When
        val code = randomCode(length)

        // Then
        assertEquals("", code)
    }

    @Test
    fun `GIVEN a negative length WHEN making a code THEN rejects it`() {
        // Given
        prepareScenario(bytes(0, 1, 2))
        val length = -1

        // When
        val error = assertFailsWith<IllegalArgumentException> { randomCode(length) }

        // Then
        assertTrue(error.message.orEmpty().isNotEmpty())
    }

    @Test
    fun `GIVEN the bytes at each edge of the alphabet WHEN making a code THEN maps them in A-Z a-z 0-9 order`() {
        // Given
        prepareScenario(bytes(0, 25, 26, 51, 52, 61))
        val length = 6

        // When
        val code = randomCode(length)

        // Then
        assertEquals("AZaz09", code)
    }

    @Test
    fun `GIVEN a byte past the alphabet WHEN making a code THEN wraps around to its start`() {
        // Given
        prepareScenario(bytes(62, 63, 124, 186))
        val length = 4

        // When
        val code = randomCode(length)

        // Then
        assertEquals("ABAA", code)
    }

    @Test
    fun `GIVEN the byte 247 WHEN making a code THEN keeps it as the last character of the alphabet`() {
        // Given
        prepareScenario(bytes(247))
        val length = 1

        // When
        val code = randomCode(length)

        // Then
        assertEquals("9", code)
    }

    @Test
    fun `GIVEN bytes from 248 to 255 WHEN making a code THEN drops them so no character is likelier`() {
        // Given
        prepareScenario(bytes(248, 249, 250, 251, 252, 253, 254, 255, 1, 2))
        val length = 2

        // When
        val code = randomCode(length)

        // Then
        assertEquals("BC", code)
    }

    @Test
    fun `GIVEN negative Kotlin bytes WHEN making a code THEN reads them as unsigned`() {
        // Given
        prepareScenario(byteArrayOf((-1).toByte(), (-9).toByte(), (-128).toByte()))
        val length = 2

        // When
        val code = randomCode(length)

        // Then
        assertEquals("9E", code)
    }

    @Test
    fun `GIVEN a batch of only dropped bytes WHEN making a code THEN asks the source for more`() {
        // Given
        prepareScenario(bytes(255, 254, 248), bytes(3, 4))
        val length = 2

        // When
        val code = randomCode(length)

        // Then
        assertEquals("DE", code)
        assertEquals(2, batchesRead)
    }

    @Test
    fun `GIVEN an empty batch first WHEN making a code THEN asks the source again`() {
        // Given
        prepareScenario(ByteArray(0), bytes(5, 6, 7))
        val length = 3

        // When
        val code = randomCode(length)

        // Then
        assertEquals("FGH", code)
        assertEquals(2, batchesRead)
    }

    @Test
    fun `GIVEN a code longer than a batch WHEN making it THEN fills it from several batches`() {
        // Given
        prepareScenario(bytes(0, 1, 2), bytes(255, 3, 4, 5), bytes(6, 7, 8, 9, 10))
        val length = 9

        // When
        val code = randomCode(length)

        // Then
        assertEquals("ABCDEFGHI", code)
        assertEquals(3, batchesRead)
    }

    @Test
    fun `GIVEN more bytes than the code needs WHEN making it THEN uses only as many characters as asked`() {
        // Given
        prepareScenario(bytes(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13), bytes(14, 15))
        val length = 3

        // When
        val code = randomCode(length)

        // Then
        assertEquals("ABC", code)
        assertEquals(1, batchesRead)
    }

    @Test
    fun `GIVEN the secure random source WHEN making a code THEN returns 8 letters and digits`() {
        // Given
        val secureRandomCode = SecureRandomCode()
        val length = 8

        // When
        val code = secureRandomCode(length)

        // Then
        assertTrue(Regex("^[A-Za-z0-9]{8}$").matches(code), "Unexpected code: $code")
    }

    @Test
    fun `GIVEN the secure random source WHEN making a long code THEN fills it with letters and digits`() {
        // Given
        val secureRandomCode = SecureRandomCode()
        val length = 500

        // When
        val code = secureRandomCode(length)

        // Then
        assertTrue(Regex("^[A-Za-z0-9]{500}$").matches(code), "Unexpected code: $code")
    }

    @Test
    fun `GIVEN the secure random source WHEN making two codes THEN they differ`() {
        // Given
        val secureRandomCode = SecureRandomCode()
        val first = secureRandomCode(8)

        // When
        val second = secureRandomCode(8)

        // Then
        assertNotEquals(first, second)
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

    private fun prepareScenario(vararg batches: ByteArray) {
        val remaining = ArrayDeque(batches.toList())
        batchesRead = 0
        randomCode = SecureRandomCode(
            randomBytes = {
                batchesRead++
                remaining.removeFirstOrNull() ?: error("The source ran out of batches")
            },
        )
    }
}
