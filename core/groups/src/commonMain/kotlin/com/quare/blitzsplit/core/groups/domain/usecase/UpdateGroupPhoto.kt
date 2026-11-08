package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.model.ProfilePhoto

// Why: the group's photo opened big, saved on this device and synced later; null takes the photo off.
fun interface UpdateGroupPhoto {
    suspend operator fun invoke(
        groupId: String,
        photo: ProfilePhoto?,
    ): Result<Unit>
}
