package com.quare.blitzsplit.android.feature.join

import com.quare.blitzsplit.android.designsystem.layout.previewInitialsOf
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.join.presentation.model.JoinMember

// Why: the previews of the invite and of "Quem é você no grupo?" show the same group.
internal fun createPreviewMembers(): List<JoinMember> = listOf(
    JoinMember(
        "1",
        "Pierre",
        previewInitialsOf("Pierre"),
        AvatarColor.BLUE,
        hasJoined = true,
        photoUrl = null,
    ),
    JoinMember(
        "2",
        "Ana",
        previewInitialsOf("Ana"),
        AvatarColor.GREEN,
        hasJoined = true,
        photoUrl = null,
    ),
    JoinMember(
        "3",
        "Bruno",
        previewInitialsOf("Bruno"),
        AvatarColor.PEACH,
        hasJoined = false,
        photoUrl = null,
    ),
    JoinMember(
        "4",
        "Carla",
        previewInitialsOf("Carla"),
        AvatarColor.LILAC,
        hasJoined = false,
        photoUrl = null,
    ),
)
