package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveHasGroups
import kotlinx.coroutines.flow.Flow

class ObserveHasGroupsUseCase(
    private val repository: GroupRepository,
) : ObserveHasGroups {
    override fun invoke(): Flow<Boolean> = repository.observeHasGroups()
}
