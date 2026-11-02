package com.quare.blitzsplit.core.profile.domain.model

import com.quare.blitzsplit.core.utils.TextFolding

/*
 * Why: "Pix copia e cola" and its QR code in "Pagar Ana" (4.5): a static BR Code (the Banco Central's EMV QR) with
 * the receiver's key and the amount already in it, so the payer's bank fills both in. The bank shows the name the key
 * belongs to, so the name and city here only fill the fields the code must carry.
 */
object PixBrCode {
    private const val GUI = "br.gov.bcb.pix"
    private const val NAME_MAX_LENGTH = 25
    private const val CITY = "BRASIL"
    private const val NAME_FALLBACK = "PIX"
    private const val CENTS_PER_REAL = 100
    private const val CRC_POLYNOMIAL = 0x1021
    private const val CRC_INITIAL = 0xFFFF
    private const val CRC_MASK = 0xFFFF
    private const val CRC_HIGH_BIT = 0x8000
    private const val BYTE_BITS = 8
    private const val CRC_LENGTH = 4
    private const val HEX_RADIX = 16

    // Why: letters past TextFolding's Portuguese and Spanish ones, which would otherwise be dropped ("Šárka" → "Sarka").
    private val MORE_FOLDS = mapOf(
        'ÿ' to 'y',
        'š' to 's',
        'ž' to 'z',
        'č' to 'c',
        'ć' to 'c',
        'ř' to 'r',
        'ď' to 'd',
        'ť' to 't',
        'ň' to 'n',
        'ľ' to 'l',
        'ĺ' to 'l',
        'ŕ' to 'r',
        'ś' to 's',
        'ź' to 'z',
        'ż' to 'z',
        'ł' to 'l',
        'ő' to 'o',
        'ű' to 'u',
        'ă' to 'a',
        'ș' to 's',
        'ţ' to 't',
        'ț' to 't',
        'ę' to 'e',
        'ą' to 'a',
        'ė' to 'e',
        'ī' to 'i',
        'ū' to 'u',
        'ē' to 'e',
        'ā' to 'a',
        'ğ' to 'g',
        'ş' to 's',
        'ı' to 'i',
        'ø' to 'o',
    )

    /*
     * Why: key is the stored value (11 CPF digits, "+5521987654321", a lowercase e-mail or random key), which is how
     * the Pix directory writes it; amountMinor in centavos, none for a code the payer types the amount into.
     */
    fun of(
        key: String,
        amountMinor: Long?,
        receiverName: String,
    ): String {
        val payload = buildString {
            append(encodeField("00", "01"))
            append(encodeField("26", encodeField("00", GUI) + encodeField("01", key)))
            append(encodeField("52", "0000"))
            append(encodeField("53", "986"))
            amountMinor?.takeIf { it > 0 }?.let { append(encodeField("54", amountOf(it))) }
            append(encodeField("58", "BR"))
            append(encodeField("59", nameOf(receiverName)))
            append(encodeField("60", CITY))
            append(encodeField("62", encodeField("05", "***")))
            append("6304")
        }
        return payload + crcOf(payload)
    }

    private fun encodeField(
        id: String,
        value: String,
    ): String = id + value.length.toString().padStart(2, '0') + value

    private fun amountOf(amountMinor: Long): String =
        "${amountMinor / CENTS_PER_REAL}.${(amountMinor % CENTS_PER_REAL).toString().padStart(2, '0')}"

    // Why: the field takes only plain letters and digits, up to 25: "José" goes as "Jose".
    private fun nameOf(name: String): String = name
        .map { char ->
            val lower = TextFolding.toFolded(char.toString()).single()
            val folded = MORE_FOLDS[lower] ?: lower
            if (char.isUpperCase()) folded.uppercaseChar() else folded
        }.filter { it.isAsciiLetterOrDigit() || it == ' ' }
        .joinToString("")
        .trim()
        .take(NAME_MAX_LENGTH)
        .trim()
        .ifEmpty { NAME_FALLBACK }

    private fun Char.isAsciiLetterOrDigit(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'

    // Why: CRC-16/CCITT-FALSE over the whole payload, its own "6304" included, as the BR Code manual asks.
    private fun crcOf(payload: String): String {
        var crc = CRC_INITIAL
        payload.encodeToByteArray().forEach { byte ->
            crc = crc xor ((byte.toInt() and 0xFF) shl BYTE_BITS)
            repeat(BYTE_BITS) {
                crc = if (crc and CRC_HIGH_BIT != 0) (crc shl 1) xor CRC_POLYNOMIAL else crc shl 1
                crc = crc and CRC_MASK
            }
        }
        return crc.toString(HEX_RADIX).uppercase().padStart(CRC_LENGTH, '0')
    }
}
