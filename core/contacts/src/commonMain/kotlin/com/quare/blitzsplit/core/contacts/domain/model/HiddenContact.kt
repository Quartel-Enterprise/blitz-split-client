package com.quare.blitzsplit.core.contacts.domain.model

// Why: "Ocultar de Contatos", each user's own list, synced across their devices.
data class HiddenContact(
    val personKey: String,
    // Why: false once "Mostrar" brought them back.
    val isHidden: Boolean,
    // Why: a bill shared with them saved after it brings them back by themselves.
    val hiddenAt: Long,
)
