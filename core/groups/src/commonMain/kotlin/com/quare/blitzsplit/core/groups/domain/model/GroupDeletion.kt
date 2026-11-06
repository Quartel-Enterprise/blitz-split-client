package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.model.GroupDeleteFailure

/*
 * Why: "Ana apagou “Viagem Floripa”", a group someone else deleted that a sync took off this device, or this device's
 * own "Apagar grupo" that the server turned down, which brought the group back.
 */
data class GroupDeletion(
    val groupId: String,
    val groupName: String,
    // Why: what their member was called in the group; null once that account is deleted, and for this device's own.
    val deletedByName: String?,
    // Why: null for a group someone else deleted; why this device's own deletion didn't go through otherwise.
    val failure: GroupDeleteFailure?,
)
