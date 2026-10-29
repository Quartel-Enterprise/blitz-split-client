package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

data class GroupRow(
    val id: String,
    val name: String,
    // Why: "VF" for "Viagem Floripa", what the group's shape shows.
    val initials: String,
    val color: AvatarColor,
    // Why: up to three faces; the rest is "+N" (extraMemberCount).
    val faces: List<MemberFace>,
    val extraMemberCount: Int,
    // Why: "Só você" for one, "N membros" otherwise.
    val memberCount: Int,
    // Why: the orange dot and "na fila": created or changed here and not on the server yet.
    val isPending: Boolean,
)
