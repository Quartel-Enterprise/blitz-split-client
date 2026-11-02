package com.quare.blitzsplit.feature.categorymap.presentation.model

// Why: what "Termo" says under the word (9.2d): "Use pelo menos 2 letras.", at most 30, or only letters, digits and spaces.
enum class TermError {
    TOO_SHORT,
    TOO_LONG,
    INVALID_CHARACTERS,
}
