package com.quare.blitzsplit.feature.groups.presentation.model

// Why: the part of a name or a description the search found, which each UI marks ("Chur" in "Churrasco"); end excluded.
data class TextMatch(
    val start: Int,
    val end: Int,
)
