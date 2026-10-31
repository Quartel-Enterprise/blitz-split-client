package com.quare.blitzsplit.core.groups.domain.model

// Why: "Ana apagou “Viagem Floripa”", a group someone else deleted that a sync took off this device.
data class GroupDeletion(
    val groupId: String,
    val groupName: String,
    // Why: what their member was called in the group; null once that account is deleted.
    val deletedByName: String?,
)
