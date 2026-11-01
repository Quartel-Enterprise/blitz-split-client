package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.utils.TextFolding

// Why: "Filtrar grupos" and "Buscar" find "Apê 302" from "ape" (TextFolding), and mark where.
internal object GroupNameFilter {
    fun toFolded(text: String): String = TextFolding.toFolded(text)

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
