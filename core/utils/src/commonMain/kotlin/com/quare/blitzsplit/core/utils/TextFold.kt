package com.quare.blitzsplit.core.utils

/*
 * Why: searches and the category map match "Apê 302" from "ape" and "Farmacia" with "farmácia", as people type without
 * accents. One character folds to one, so a match in the folded text is at the same place in the text shown.
 */
object TextFold {
    private const val ACCENTED = "áàâãäåéèêëíìîïóòôõöúùûüçñý"
    private const val PLAIN = "aaaaaaeeeeiiiiooooouuuucny"

    fun toFolded(text: String): String = buildString(text.length) {
        text.forEach { char ->
            val lower = char.lowercaseChar()
            val index = ACCENTED.indexOf(lower)
            append(if (index >= 0) PLAIN[index] else lower)
        }
    }
}
