package com.quare.blitzsplit.feature.groups.presentation.model

/*
 * Why: "Filtrar grupos" and "Buscar" find "Apê 302" from "ape", as people type a name without its accents. One
 * character folds to one, so a match in the folded text is at the same place in the text shown, which marks it.
 */
internal object GroupNameFilter {
    private const val ACCENTED = "áàâãäåéèêëíìîïóòôõöúùûüçñý"
    private const val PLAIN = "aaaaaaeeeeiiiiooooouuuucny"

    fun toFolded(text: String): String = buildString(text.length) {
        text.forEach { char ->
            val lower = char.lowercaseChar()
            val index = ACCENTED.indexOf(lower)
            append(if (index >= 0) PLAIN[index] else lower)
        }
    }

    // Why: the first place the folded query is in the text, null when it isn't there or the query is empty.
    fun findMatchOrNull(
        text: String,
        foldedQuery: String,
    ): TextMatch? {
        if (foldedQuery.isEmpty()) return null
        val start = toFolded(text).indexOf(foldedQuery)
        if (start < 0) return null
        return TextMatch(
            start = start,
            end = start + foldedQuery.length,
        )
    }
}
