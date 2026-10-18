package com.quare.blitzsplit.core.utils

// Why: mirrors _shared/recovery_code.ts of the API, so a mistyped code is caught before any request.
object RecoveryCodeFormat {
    const val LENGTH: Int = 12
    private const val GROUP_SIZE = 4
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    fun normalize(input: String): String? {
        val normalized = input
            .uppercase()
            .filter { it.isLetterOrDigit() }
            .replace('O', '0')
            .replace('I', '1')
            .replace('L', '1')
        return normalized.takeIf { it.length == LENGTH && it.all { char -> char in ALPHABET } }
    }

    fun format(code: String): String = code.chunked(GROUP_SIZE).joinToString("-")
}
