package com.quare.blitzsplit.feature.restore.presentation.model

data class RestoreWordField(
    // Why: lowercase letters only; a word completed or picked from the suggestions shows whole.
    val text: String,
    // Why: no word of the list starts with it, so the field is marked at once instead of after "Restore".
    val isDeadEnd: Boolean,
)
