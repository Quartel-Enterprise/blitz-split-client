package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.utils.TextFolding

// Why: "Filtrar grupos" finds "Apê 302" from "ape" (TextFolding); "Buscar" also forgives typos (TypoTolerantQuery).
internal object GroupNameFilter {
    fun toFolded(text: String): String = TextFolding.toFolded(text)
}
