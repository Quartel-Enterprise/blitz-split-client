package com.quare.blitzsplit.core.legal.domain.model

// Why: a line of text as runs, since bold and links fall inside it.
data class LegalText(
    val spans: List<LegalSpan>,
) {
    val plain: String
        get() = spans.joinToString(separator = "") { it.text }
}
