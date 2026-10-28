package com.quare.blitzsplit.core.profile.domain.model

/*
 * Why: on-chain addresses (BIP 173 and 350) and LNURLs are bech32 strings, and only their checksum tells a
 * mistyped character apart from a real address.
 */
internal object Bech32 {
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private const val BECH32_CONSTANT = 1
    private const val BECH32M_CONSTANT = 0x2bc830a3
    private const val CHECKSUM_LENGTH = 6
    private val GENERATORS = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)

    enum class Encoding {
        BECH32,
        BECH32M,
    }

    class Decoded(
        val hrp: String,
        // Why: the 5-bit groups between the separator and the checksum.
        val data: IntArray,
        val encoding: Encoding,
    )

    // Why: null for anything that isn't bech32 with a valid checksum, mixed case included.
    fun decode(input: String): Decoded? {
        if (input.lowercase() != input && input.uppercase() != input) return null
        val text = input.lowercase()
        val separator = text.lastIndexOf('1')
        if (separator < 1 || separator + CHECKSUM_LENGTH + 1 > text.length) return null
        val hrp = text.substring(0, separator)
        if (hrp.any { it.code !in 33..126 }) return null
        val values = text.substring(separator + 1).map { CHARSET.indexOf(it) }
        if (values.any { it < 0 }) return null
        val encoding = when (computePolymod(toExpandedHumanReadablePart(hrp) + values)) {
            BECH32_CONSTANT -> Encoding.BECH32
            BECH32M_CONSTANT -> Encoding.BECH32M
            else -> return null
        }
        return Decoded(
            hrp = hrp,
            data = values.dropLast(CHECKSUM_LENGTH).toIntArray(),
            encoding = encoding,
        )
    }

    // Why: regroups 5-bit values into bytes; null when the padding isn't zero, as BIP 173 requires.
    fun toBytes(data: IntArray): ByteArray? {
        var accumulator = 0
        var bits = 0
        val bytes = mutableListOf<Byte>()
        for (value in data) {
            accumulator = (accumulator shl 5) or value
            bits += 5
            if (bits >= 8) {
                bits -= 8
                bytes += ((accumulator shr bits) and 0xff).toByte()
            }
        }
        val hasNonZeroPadding = bits >= 5 || ((accumulator shl (8 - bits)) and 0xff) != 0
        return if (hasNonZeroPadding) null else bytes.toByteArray()
    }

    private fun toExpandedHumanReadablePart(hrp: String): List<Int> =
        hrp.map { it.code shr 5 } + 0 + hrp.map { it.code and 31 }

    private fun computePolymod(values: List<Int>): Int {
        var checksum = 1
        for (value in values) {
            val top = checksum ushr 25
            checksum = ((checksum and 0x1ffffff) shl 5) xor value
            GENERATORS.forEachIndexed { index, generator ->
                if ((top shr index) and 1 == 1) checksum = checksum xor generator
            }
        }
        return checksum
    }
}
