package com.quare.blitzsplit.feature.groupdetail.presentation.model

// Why: "Grupo apagado": "Ana apagou “Viagem Floripa” depois que todo mundo acertou.", with "Voltar para Grupos".
data class GroupDeletedNotice(
    val groupName: String,
    // Why: null once that account is deleted, so the notice names nobody.
    val deletedByName: String?,
)
