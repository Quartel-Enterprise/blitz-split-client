package com.quare.blitzsplit.core.inappupdate.data

// Why: "2.10.0" is newer than "2.9.1", which comparing the text would get wrong; a missing or odd part counts as 0.
internal object VersionComparator : Comparator<String> {
    override fun compare(
        a: String,
        b: String,
    ): Int {
        val aParts = parse(a)
        val bParts = parse(b)
        val length = maxOf(aParts.size, bParts.size)
        for (index in 0 until length) {
            val comparison = aParts.getOrElse(index) { 0 }.compareTo(bParts.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        // Why: a beta comes before its release, so 3.0.2 out in the store is an update for 3.0.2-beta_3.
        return isRelease(a).compareTo(isRelease(b))
    }

    private fun isRelease(version: String): Boolean = '-' !in version

    // Why: a beta's suffix ("3.0.2-beta_1") is dropped, so its patch isn't read as 0 and the beta as older than it is.
    private fun parse(version: String): List<Int> = version
        .trim()
        .substringBefore('-')
        .split(".")
        .map { it.toIntOrNull() ?: 0 }
}
