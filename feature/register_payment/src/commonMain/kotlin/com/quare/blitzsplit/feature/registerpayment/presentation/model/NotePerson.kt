package com.quare.blitzsplit.feature.registerpayment.presentation.model

// Why: someone a note names, "você" for this device's user and "Ex-membro" with no name.
data class NotePerson(
    val name: String?,
    val isMe: Boolean,
)
