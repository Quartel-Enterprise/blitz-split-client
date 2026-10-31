package com.quare.blitzsplit.feature.groups.presentation.model

// Why: "Carla apagou o grupo Aniversário da Ana.", for a group someone else deleted while it wasn't open here.
data class DeletedGroupNotice(
    // Why: two groups of the same name deleted by the same person are still two notices.
    val groupId: String,
    val groupName: String,
    // Why: null once that account is deleted, so the notice names nobody.
    val deletedByName: String?,
)
