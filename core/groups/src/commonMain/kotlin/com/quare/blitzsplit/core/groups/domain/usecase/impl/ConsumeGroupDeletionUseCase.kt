package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.GroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.usecase.ConsumeGroupDeletion

class ConsumeGroupDeletionUseCase(
    private val repository: GroupDeletionRepository,
) : ConsumeGroupDeletion {
    override fun invoke(groupId: String) {
        repository.consume(groupId)
    }
}
