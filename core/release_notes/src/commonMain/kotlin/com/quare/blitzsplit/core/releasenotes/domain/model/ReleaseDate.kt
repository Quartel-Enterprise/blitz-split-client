package com.quare.blitzsplit.core.releasenotes.domain.model

// Why: the day a version came out, which each UI writes in the reader's format ("November 14, 2026").
data class ReleaseDate(
    val year: Int,
    val month: Int,
    val day: Int,
)
