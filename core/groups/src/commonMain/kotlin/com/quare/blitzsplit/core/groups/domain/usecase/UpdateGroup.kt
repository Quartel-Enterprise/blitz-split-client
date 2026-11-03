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
        // Why: null keeps the stored one, which may be none (a group made before shapes) or a newer app's.
        shape: GroupShape?,
        photo: ProfilePhoto?,
        currency: GroupCurrency,
    ): Result<Unit>
}
