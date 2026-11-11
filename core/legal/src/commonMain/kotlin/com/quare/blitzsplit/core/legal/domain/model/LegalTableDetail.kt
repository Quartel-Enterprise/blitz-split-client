package com.quare.blitzsplit.core.legal.domain.model

// Why: a cell of a table's row under its column's name, which a header shorter than the row leaves out.
data class LegalTableDetail(
    val label: LegalText?,
    val text: LegalText,
)
