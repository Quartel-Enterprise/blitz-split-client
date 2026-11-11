package com.quare.blitzsplit.core.legal.domain.model

// Why: the day the text last changed, which each UI writes in the reader's format ("Updated November 5, 2026").
data class LegalDate(
    val year: Int,
    val month: Int,
    val day: Int,
)
