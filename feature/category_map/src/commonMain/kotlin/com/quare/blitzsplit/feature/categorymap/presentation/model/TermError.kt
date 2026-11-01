package com.quare.blitzsplit.feature.categorymap.presentation.model

// Why: what "Termo" says under the word (9.2d): "Use pelo menos 2 letras." or only letters, digits and spaces.
enum class TermError {
    TOO_SHORT,
    INVALID_CHARACTERS,
}
