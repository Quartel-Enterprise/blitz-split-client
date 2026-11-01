package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.GroupPinRepository
import com.quare.blitzsplit.core.groups.domain.usecase.ObservePinnedGroupIds
import kotlinx.coroutines.flow.Flow

class ObservePinnedGroupIdsUseCase(
    private val repository: GroupPinRepository,
) : ObservePinnedGroupIds {
    override fun invoke(): Flow<Set<String>> = repository.observePinnedGroupIds()
}
