package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveGroupUseCase(
    private val repository: GroupRepository,
) : ObserveGroup {
    override fun invoke(groupId: String): Flow<Group?> = repository
        .observeGroups()
        .map { groups -> groups.firstOrNull { it.id == groupId } }
        .distinctUntilChanged()
}
