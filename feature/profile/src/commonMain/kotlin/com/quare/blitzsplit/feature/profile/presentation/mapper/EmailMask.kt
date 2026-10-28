package com.quare.blitzsplit.feature.profile.presentation.mapper

private const val MASK = "•••"

// Why: only the first letter of the name stays, so the profile tells whose account it is without showing it.
internal fun String.toMaskedEmail(): String {
    val at = indexOf('@')
    if (at <= 0) return take(1) + MASK
    return first() + MASK + substring(at)
}
