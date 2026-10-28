package com.quare.blitzsplit.feature.profile.presentation.mapper

private const val MASK = "•••"

// Why: only the first letter of the name stays, so the profile tells whose account it is without showing it.
internal fun String.toMaskedEmail(): String {
    val at = indexOf('@')
    return when {
        at < 0 -> MASK
        at == 0 -> MASK + this
        else -> first() + MASK + substring(at)
    }
}
