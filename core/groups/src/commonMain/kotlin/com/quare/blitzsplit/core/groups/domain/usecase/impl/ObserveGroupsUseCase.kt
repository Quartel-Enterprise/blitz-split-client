package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import kotlinx.coroutines.flow.Flow

class ObserveGroupsUseCase(
    private val repository: GroupRepository,
) : ObserveGroups {
    override fun invoke(): Flow<List<Group>> = repository.observeGroups()
}
