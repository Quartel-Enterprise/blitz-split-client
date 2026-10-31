package com.quare.blitzsplit.feature.groups.presentation.model

// Why: "Filtrar grupos" finds "Apê 302" from "ape", as people type a name without its accents.
internal object GroupNameFilter {
    private const val ACCENTED = "áàâãäåéèêëíìîïóòôõöúùûüçñý"
    private const val PLAIN = "aaaaaaeeeeiiiiooooouuuucny"

    fun toFolded(text: String): String = buildString(text.length) {
        text.lowercase().forEach { char ->
            val index = ACCENTED.indexOf(char)
            append(if (index >= 0) PLAIN[index] else char)
        }
    }
}
