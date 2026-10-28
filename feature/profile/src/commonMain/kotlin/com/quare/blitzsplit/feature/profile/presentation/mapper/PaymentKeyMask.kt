package com.quare.blitzsplit.feature.profile.presentation.mapper

import com.quare.blitzsplit.core.model.PaymentKeyFormat

private const val HIDDEN = "•••"
private const val ELLIPSIS = "…"
private const val EMAIL_SHOWN = 3
private const val PHONE_LENGTH = 14

/*
 * Why: the profile shows enough to tell keys apart, never the whole key: "•••.456.789-••", "+55 (11) 9••••-4321",
 * "pie•••@email.com", "7d9f3c2a…4d1a37", "bc1p5d7r…sxg3297". A Lightning Address is a public handle and stays whole.
 */
internal fun String.toMaskedPaymentKey(format: PaymentKeyFormat): String = when (format) {
    PaymentKeyFormat.CPF -> "$HIDDEN.${substring(3, 6)}.${substring(6, 9)}-••"

    PaymentKeyFormat.PHONE -> toMaskedPhone()

    PaymentKeyFormat.EMAIL -> toMaskedEmailKey()

    PaymentKeyFormat.RANDOM -> toShortened(
        first = 8,
        last = 6,
    )

    PaymentKeyFormat.LIGHTNING_ADDRESS -> this

    PaymentKeyFormat.LNURL -> toShortened(
        first = 10,
        last = 6,
    )

    PaymentKeyFormat.SEGWIT, PaymentKeyFormat.TAPROOT -> toShortened(
        first = 8,
        last = 7,
    )
}

// Why: "+5511912345678" keeps the country, the DDD, the leading 9 and the last four digits.
private fun String.toMaskedPhone(): String =
    if (length == PHONE_LENGTH) "+55 (${substring(3, 5)}) 9••••-${takeLast(4)}" else HIDDEN

private fun String.toMaskedEmailKey(): String {
    val at = indexOf('@')
    if (at <= 0) return HIDDEN
    val shown = if (at > EMAIL_SHOWN) EMAIL_SHOWN else 1
    return take(shown) + HIDDEN + substring(at)
}

private fun String.toShortened(
    first: Int,
    last: Int,
): String = if (length <= first + last) this else take(first) + ELLIPSIS + takeLast(last)
