package com.quare.blitzsplit.feature.join.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

data class JoinMember(
    val id: String,
    val name: String,
    // Why: "A" for Ana, "AP" for Ana Paula.
    val initials: String,
    val avatarColor: AvatarColor,
    // Why: "Já entrou": someone opened the link and picked this member, who can't be picked again.
    val hasJoined: Boolean,
    // Why: the face of whoever joined as this member, drawn over the initials; null for none.
    val photoUrl: String?,
)
