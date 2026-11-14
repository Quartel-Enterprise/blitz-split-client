package com.quare.blitzsplit.core.expenses.export

private const val BITS_PER_BYTE = 8

/*
 * Why: the zip an .xlsx is, written without a library: every part stored as is (method 0), which Excel, Numbers,
 * LibreOffice and Google Sheets all open. A spreadsheet of a group's bills is small, so compressing it buys little.
 */
internal object StoredZip {
    private const val LOCAL_HEADER_SIGNATURE = 0x04034b50
    private const val CENTRAL_HEADER_SIGNATURE = 0x02014b50
    private const val END_SIGNATURE = 0x06054b50
    private const val ZIP_VERSION = 20

    // Why: bit 11, the names are UTF-8.
    private const val UTF8_NAMES = 0x0800

    // Why: 1980-01-01 00:00 in MS-DOS time, the earliest a zip can say; the parts of a file carry no date of their own.
    private const val DOS_DATE = 0x0021
    private const val CRC_POLYNOMIAL = 0xEDB88320.toInt()
    private const val BYTE_MASK = 0xFF
    private const val BYTE_VALUES = 256

    // Why: the CRC of each byte worked out once, so a sheet's bytes take one lookup each rather than eight shifts.
    private val crcTable = IntArray(BYTE_VALUES) { byte ->
        var crc = byte
        repeat(BITS_PER_BYTE) {
            crc = if (crc and 1 != 0) (crc ushr 1) xor CRC_POLYNOMIAL else crc ushr 1
        }
        crc
    }

    fun of(entries: List<Pair<String, ByteArray>>): ByteArray {
        val out = ZipBytes()
        val central = ZipBytes()
        entries.forEach { (name, data) ->
            val nameBytes = name.encodeToByteArray()
            val crc = computeCrc32(data)
            val offset = out.size
            out.int(LOCAL_HEADER_SIGNATURE)
            out.short(ZIP_VERSION)
            out.short(UTF8_NAMES)
            out.short(0)
            out.short(0)
            out.short(DOS_DATE)
            out.int(crc)
            out.int(data.size)
            out.int(data.size)
            out.short(nameBytes.size)
            out.short(0)
            out.bytes(nameBytes)
            out.bytes(data)

            central.int(CENTRAL_HEADER_SIGNATURE)
            central.short(ZIP_VERSION)
            central.short(ZIP_VERSION)
            central.short(UTF8_NAMES)
            central.short(0)
            central.short(0)
            central.short(DOS_DATE)
            central.int(crc)
            central.int(data.size)
            central.int(data.size)
            central.short(nameBytes.size)
            central.short(0)
            central.short(0)
            central.short(0)
            central.short(0)
            central.int(0)
            central.int(offset)
            central.bytes(nameBytes)
        }
        val centralOffset = out.size
        val centralBytes = central.toByteArray()
        out.bytes(centralBytes)
        out.int(END_SIGNATURE)
        out.short(0)
        out.short(0)
        out.short(entries.size)
        out.short(entries.size)
        out.int(centralBytes.size)
        out.int(centralOffset)
        out.short(0)
        return out.toByteArray()
    }

    fun computeCrc32(data: ByteArray): Int {
        var crc = -1
        data.forEach { byte ->
            crc = crcTable[(crc xor byte.toInt()) and BYTE_MASK] xor (crc ushr BITS_PER_BYTE)
        }
        return crc.inv()
    }
}

// Why: a growing buffer of little-endian numbers, the order a zip writes them in.
private class ZipBytes {
    private var buffer = ByteArray(1024)
    var size = 0
        private set

    fun short(value: Int) {
        byte(value)
        byte(value ushr BITS_PER_BYTE)
    }

    fun int(value: Int) {
        short(value)
        short(value ushr (BITS_PER_BYTE * 2))
    }

    fun bytes(data: ByteArray) {
        ensure(data.size)
        data.copyInto(buffer, size)
        size += data.size
    }

    fun toByteArray(): ByteArray = buffer.copyOf(size)

    private fun byte(value: Int) {
        ensure(1)
        buffer[size++] = value.toByte()
    }

    private fun ensure(extra: Int) {
        if (size + extra <= buffer.size) return
        buffer = buffer.copyOf(maxOf(buffer.size * 2, size + extra))
    }
}
