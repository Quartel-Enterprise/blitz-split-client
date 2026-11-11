package com.quare.blitzsplit.core.utils

import kotlin.uuid.Uuid

/*
 * Why: whoever has the link sees the group, so its code must be impossible to guess: each character comes from the
 * platform's secure random source, which Uuid.random() reads on every target (SecureRandom, crypto.getRandomValues,
 * the Apple platforms' own), and every character of [A-Za-z0-9] is as likely as the others.
 */
class SecureRandomCode(
    private val randomBytes: () -> ByteArray = ::secureRandomBytes,
) : RandomCode {
    override fun invoke(length: Int): String {
        require(length >= 0) { "A code can't have $length characters" }
        val code = StringBuilder(length)
        while (code.length < length) {
            randomBytes()
                .map { it.toInt() and BYTE_MASK }
                // Why: a byte at or over 248 would make the first characters likelier than the rest, so it's dropped.
                .filter { it < UNBIASED_LIMIT }
                .take(length - code.length)
                .forEach { code.append(ALPHABET[it % ALPHABET.length]) }
        }
        return code.toString()
    }

    private companion object {
        const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        const val BYTE_MASK = 0xFF

        // Why: the largest multiple of 62 a byte reaches.
        const val UNBIASED_LIMIT = 248
    }
}

/*
 * Why: a version 4 UUID is 122 random bits; bytes 6 and 8 carry its version and variant, so only the other 14 are
 * whole random bytes.
 */
private fun secureRandomBytes(): ByteArray = Uuid
    .random()
    .toByteArray()
    .filterIndexed { index, _ -> index != VERSION_BYTE && index != VARIANT_BYTE }
    .toByteArray()

private const val VERSION_BYTE = 6
private const val VARIANT_BYTE = 8
