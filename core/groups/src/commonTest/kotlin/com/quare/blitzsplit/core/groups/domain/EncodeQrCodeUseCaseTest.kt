package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.groups.domain.usecase.impl.EncodeQrCodeUseCase
import com.quare.blitzsplit.core.model.QrCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class EncodeQrCodeUseCaseTest {
    private val useCase = EncodeQrCodeUseCase()

    @Test
    fun `GIVEN an invite link WHEN encoding it THEN has one module for each row and column of its side`() {
        // Given
        val text = InviteLink.of("aB3dE5gH")

        // When
        val code = useCase(text)

        // Then
        assertEquals(code.size * code.size, code.modules.size)
    }

    @Test
    fun `GIVEN an invite link WHEN encoding it THEN has the finder squares at the three corners`() {
        // Given
        val text = InviteLink.of("aB3dE5gH")

        // When
        val code = useCase(text)

        // Then
        val corners = listOf(0 to 0, 0 to code.size - 7, code.size - 7 to 0)
        corners.forEach { (top, left) ->
            for (row in 0 until 7) {
                for (column in 0 until 7) {
                    val border = row == 0 || row == 6 || column == 0 || column == 6
                    val core = row in 2..4 && column in 2..4
                    assertEquals(
                        border || core,
                        code.isDark(top + row, left + column),
                        "finder at ($top, $left), module ($row, $column)",
                    )
                }
            }
        }
    }

    @Test
    fun `GIVEN an invite link WHEN encoding it THEN the always dark module sits by the bottom left finder`() {
        // Given
        val text = InviteLink.of("aB3dE5gH")

        // When
        val code = useCase(text)

        // Then
        assertTrue(code.isDark(code.size - 8, 8))
    }

    @Test
    fun `GIVEN an invite link WHEN encoding it THEN both copies of the format read medium error correction`() {
        // Given
        val text = InviteLink.of("aB3dE5gH")

        // When
        val code = useCase(text)

        // Then
        val first = firstFormatCopy(code)
        val second = secondFormatCopy(code)
        assertEquals(first, second)
        assertTrue(isValidFormat(first), "format bits ${first.toString(2)}")
        assertEquals(MEDIUM_ERROR_CORRECTION, (first xor FORMAT_MASK) shr 13)
    }

    @Test
    fun `GIVEN the same link twice WHEN encoding it THEN gives the same code`() {
        // Given
        val text = InviteLink.of("aB3dE5gH")

        // When
        val first = useCase(text)
        val second = useCase(text)

        // Then
        assertEquals(first, second)
    }

    @Test
    fun `GIVEN the links of two groups WHEN encoding them THEN gives different codes`() {
        // Given
        val trip = InviteLink.of("aB3dE5gH")
        val home = InviteLink.of("Zx9Yw8Vu")

        // When
        val tripCode = useCase(trip)
        val homeCode = useCase(home)

        // Then
        assertNotEquals(tripCode.modules, homeCode.modules)
    }

    @Test
    fun `GIVEN an invite link of 33 characters WHEN encoding it THEN fits a small code of version 2 or 3`() {
        // Given
        val text = InviteLink.of("aB3dE5gH")

        // When
        val code = useCase(text)

        // Then
        assertEquals(33, text.length)
        assertTrue(code.size in setOf(25, 29), "size ${code.size}")
    }

    // Why: ISO/IEC 18004 places the 15 format bits twice, around the top left finder and split by the other two.
    private fun firstFormatCopy(code: QrCode): Int {
        val positions = (0..5).map { it to 8 } +
            listOf(7 to 8, 8 to 8, 8 to 7) +
            (9..14).map { 8 to 14 - it }
        return bits(code, positions)
    }

    private fun secondFormatCopy(code: QrCode): Int {
        val positions = (0..7).map { 8 to code.size - 1 - it } + (8..14).map { code.size - 15 + it to 8 }
        return bits(code, positions)
    }

    private fun bits(
        code: QrCode,
        positions: List<Pair<Int, Int>>,
    ): Int = positions.foldIndexed(0) { bit, value, (row, column) ->
        if (code.isDark(row, column)) value or (1 shl bit) else value
    }

    private fun isValidFormat(bits: Int): Boolean {
        val unmasked = bits xor FORMAT_MASK
        val data = unmasked shr 10
        var remainder = data
        repeat(10) { remainder = (remainder shl 1) xor ((remainder ushr 9) * FORMAT_GENERATOR) }
        return (data shl 10 or remainder) == unmasked
    }

    private companion object {
        const val FORMAT_MASK = 0x5412
        const val FORMAT_GENERATOR = 0x537
        const val MEDIUM_ERROR_CORRECTION = 0b00
    }
}
