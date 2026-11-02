package com.quare.blitzsplit.feature.groups.presentation.model

/*
 * Why: "Sincronizando… 3 de 7 grupos" (5.1d) while a new device's first download brings the groups down one by one;
 * groupCount is null until the server said how many are coming.
 */
data class DownloadProgress(
    val arrivedGroupCount: Int,
    val groupCount: Int?,
) {
    // Why: the bar's share; null draws a bar that runs, until the count is known.
    val fraction: Float?
        get() = groupCount?.takeIf { it > 0 }?.let { arrivedGroupCount.toFloat() / it }

    // Why: grey rows hold the place of the groups still to come, a few at most, three while their count is unknown.
    val placeholderCount: Int
        get() = groupCount?.let { (it - arrivedGroupCount).coerceIn(0, MAX_PLACEHOLDERS) } ?: MAX_PLACEHOLDERS

    private companion object {
        const val MAX_PLACEHOLDERS = 3
    }
}
