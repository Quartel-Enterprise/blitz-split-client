package com.quare.blitzsplit.core.utils

/*
 * Why: the home's "Buscar" and Contatos find "Apê 302" from "ape", as people type a name without its accents, and
 * sort "Álvaro" with the A's; the categories match "Farmacia" with "farmácia" the same way. One character folds to
 * one, so a match in the folded text is at the same place in the text shown, which marks it.
 */
object TextFolding {
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
