package com.quare.blitzsplit.core.expenses.export

import java.io.ByteArrayInputStream
import java.util.zip.CRC32
import java.util.zip.ZipInputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

// Why: reads the zip back with java.util.zip, the reader commonTest doesn't have.
class StoredZipReadBackTest {
    @Test
    fun `GIVEN parts with UTF-8 names WHEN zipping them THEN java util zip reads back the same names and bytes`() {
        // Given
        val entries = listOf(
            "ação/conta.txt" to "Açaí, R$ 12,50".encodeToByteArray(),
            "empty.txt" to ByteArray(0),
            "random.bin" to Random(42).nextBytes(5_000),
        )

        // When
        val zip = StoredZip.of(entries)

        // Then
        val read = ZipInputStream(ByteArrayInputStream(zip)).use { stream ->
            generateSequence { stream.nextEntry }.map { it.name to stream.readBytes() }.toList()
        }
        assertEquals(entries.map { it.first }, read.map { it.first })
        entries.zip(read).forEach { (written, readBack) -> assertContentEquals(written.second, readBack.second) }
    }

    @Test
    fun `GIVEN random bytes WHEN computing their CRC-32 THEN matches the JDK CRC-32`() {
        // Given
        val data = Random(7).nextBytes(10_000)

        // When
        val crc = StoredZip.computeCrc32(data)

        // Then
        assertEquals(CRC32().apply { update(data) }.value, crc.toLong() and 0xFFFFFFFFL)
    }
}
