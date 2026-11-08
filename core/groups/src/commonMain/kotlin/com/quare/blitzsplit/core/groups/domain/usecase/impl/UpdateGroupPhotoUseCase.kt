package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.UpdateGroupPhoto
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

class UpdateGroupPhotoUseCase(
    private val repository: GroupRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : UpdateGroupPhoto {
    override suspend fun invoke(
        groupId: String,
        photo: ProfilePhoto?,
    ): Result<Unit> = repository.updateGroupPhoto(
        groupId = groupId,
        photo = photo,
        updatedAt = currentTimeMillis(),
    )
}
