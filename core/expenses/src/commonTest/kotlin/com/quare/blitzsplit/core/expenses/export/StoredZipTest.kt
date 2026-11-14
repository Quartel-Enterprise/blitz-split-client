package com.quare.blitzsplit.core.expenses.export

import kotlin.test.Test
import kotlin.test.assertEquals

class StoredZipTest {
    @Test
    fun `GIVEN the standard check input WHEN computing its CRC-32 THEN matches the published check value`() {
        // Given
        val data = "123456789".encodeToByteArray()

        // When
        val crc = StoredZip.computeCrc32(data)

        // Then
        assertEquals(0xCBF43926L.toInt(), crc)
    }

    @Test
    fun `GIVEN no bytes WHEN computing their CRC-32 THEN is zero`() {
        // Given
        val data = ByteArray(0)

        // When
        val crc = StoredZip.computeCrc32(data)

        // Then
        assertEquals(0, crc)
    }

    @Test
    fun `GIVEN two parts WHEN zipping them THEN the file opens with a local header and ends with the end record`() {
        // Given
        val entries = listOf("a.txt" to "A".encodeToByteArray(), "b.txt" to "BB".encodeToByteArray())

        // When
        val zip = StoredZip.of(entries)

        // Then
        assertEquals(listOf(0x50, 0x4B, 0x03, 0x04), zip.take(4).map { it.toInt() and 0xFF })
        val end = zip.size - END_RECORD_SIZE
        assertEquals(listOf(0x50, 0x4B, 0x05, 0x06), zip.copyOfRange(end, end + 4).map { it.toInt() and 0xFF })
        assertEquals(2, zip[end + ENTRY_COUNT_OFFSET].toInt())
    }

    private companion object {
        const val END_RECORD_SIZE = 22
        const val ENTRY_COUNT_OFFSET = 10
    }
}
