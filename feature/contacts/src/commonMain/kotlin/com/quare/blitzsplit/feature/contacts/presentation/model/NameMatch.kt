package com.quare.blitzsplit.feature.contacts.presentation.model

// Why: where the typed text is in the name, so each UI marks it.
data class NameMatch(
    val start: Int,
    val end: Int,
)
