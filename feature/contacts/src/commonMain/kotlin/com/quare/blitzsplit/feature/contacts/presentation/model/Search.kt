package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.utils.TextFolding

// Why: Contatos' search and the agenda's find "Júlia" from "julia", ignoring case and accents, and mark where.
internal object Search {
    fun toFolded(text: String): String = TextFolding.toFolded(text)

    fun matchOrNull(
        text: String,
        foldedQuery: String,
    ): NameMatch? {
        if (foldedQuery.isEmpty()) return null
        val start = toFolded(text).indexOf(foldedQuery)
        if (start < 0) return null
        return NameMatch(
            start = start,
            end = start + foldedQuery.length,
        )
    }

    // Why: "Caio Nunes" → "CN", as the agenda's rows draw it.
    fun initialsOf(name: String): String = Initials.of(name)
}
