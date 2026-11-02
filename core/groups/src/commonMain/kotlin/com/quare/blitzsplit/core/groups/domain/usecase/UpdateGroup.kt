package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.ProfilePhoto

// Why: "Editar grupo", saved on this device and synced later.
fun interface UpdateGroup {
    suspend operator fun invoke(
        groupId: String,
        name: String,
        type: GroupType,
        color: AvatarColor,
        shape: GroupShape,
        photo: ProfilePhoto?,
        currency: GroupCurrency,
    ): Result<Unit>
}
