package com.quare.blitzsplit.core.releasenotes.domain.model

/*
 * Why: a version's name as version.properties and the beta tags write it, "3.1.0" or "3.1.0-beta_2", read into what
 * orders it: a version's betas come before it, in their own order.
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    // Why: the beta's number, null for a stable version.
    val beta: Int?,
) : Comparable<AppVersion> {
    val isBeta: Boolean
        get() = beta != null

    // Why: "3.1.0", which a beta shares with the version it leads to.
    val name: String
        get() = "$major.$minor.$patch"

    // Why: the name as it was written, "3.1.0-beta_2".
    val fullName: String
        get() = beta?.let { "$name$BETA_SEPARATOR$it" } ?: name

    override fun compareTo(other: AppVersion): Int = compareValuesBy(
        this,
        other,
        AppVersion::major,
        AppVersion::minor,
        AppVersion::patch,
        { version -> version.beta ?: Int.MAX_VALUE },
    )

    companion object {
        private const val BETA_SEPARATOR = "-beta_"
        private const val PARTS = 3
        private const val MAX_DIGITS = 9

        // Why: null for a name written any other way, which a newer site or a local build may hold.
        fun parseOrNull(text: String): AppVersion? {
            val numbers = text.substringBefore(BETA_SEPARATOR).split('.').map { it.toNumberOrNull() }
            val hasBeta = BETA_SEPARATOR in text
            val beta = text.substringAfter(BETA_SEPARATOR, missingDelimiterValue = "").toNumberOrNull()
            if (numbers.size != PARTS || numbers.any { it == null } || hasBeta != (beta != null)) return null
            return AppVersion(
                major = numbers[0] ?: return null,
                minor = numbers[1] ?: return null,
                patch = numbers[2] ?: return null,
                beta = beta,
            )
        }

        // Why: digits alone, and few enough to count: no sign, no space, nothing a number parser would let through.
        private fun String.toNumberOrNull(): Int? = takeIf {
            it.length in 1..MAX_DIGITS &&
                it.all { char -> char in '0'..'9' }
        }?.toInt()
    }
}
