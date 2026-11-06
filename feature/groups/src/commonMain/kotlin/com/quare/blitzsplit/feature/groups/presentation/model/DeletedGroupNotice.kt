package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.GroupDeleteFailure

/*
 * Why: "Carla apagou o grupo Aniversário da Ana.", for a group someone else deleted while it wasn't open here, or why
 * this device's own "Apagar grupo" didn't go through and the group is back.
 */
data class DeletedGroupNotice(
    // Why: two groups of the same name deleted by the same person are still two notices.
    val groupId: String,
    val groupName: String,
    // Why: null once that account is deleted, so the notice names nobody, and for this device's own deletion.
    val deletedByName: String?,
    // Why: null for a group someone else deleted; each platform words why its own deletion was turned down.
    val failure: GroupDeleteFailure?,
)
