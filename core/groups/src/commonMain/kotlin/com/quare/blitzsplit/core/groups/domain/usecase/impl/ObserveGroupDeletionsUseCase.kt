package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.repository.GroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroupDeletions
import kotlinx.coroutines.flow.Flow

class ObserveGroupDeletionsUseCase(
    private val repository: GroupDeletionRepository,
) : ObserveGroupDeletions {
    override fun invoke(): Flow<List<GroupDeletion>> = repository.observeDeletions()
}
