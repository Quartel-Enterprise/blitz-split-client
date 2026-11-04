package com.quare.blitzsplit.feature.editgroup.presentation.model

// Why: someone named in "O que muda" (13.3), "você" for this device's user, "Ex-membro" with no name.
data class SimplifyName(
    val name: String?,
    val isMe: Boolean,
)
