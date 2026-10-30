package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.UpdateGroup
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

class UpdateGroupUseCase(
    private val repository: GroupRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : UpdateGroup {
    override suspend fun invoke(
        groupId: String,
        name: String,
        type: GroupType,
        color: AvatarColor,
        currency: GroupCurrency,
    ): Result<Unit> = repository.updateGroup(
        groupId = groupId,
        name = GroupNameRules.toCapped(name.trim()),
        type = type,
        color = color,
        currency = currency,
        updatedAt = currentTimeMillis(),
    )
}
