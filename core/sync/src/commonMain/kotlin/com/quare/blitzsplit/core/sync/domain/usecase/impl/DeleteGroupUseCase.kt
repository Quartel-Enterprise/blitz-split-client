package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup

class DeleteGroupUseCase(
    private val groupRepository: GroupRepository,
    private val coordinator: SyncCoordinator,
) : DeleteGroup {
    override suspend fun invoke(groupId: String): Result<GroupExitOutcome> =
        coordinator.runGroupExit { groupRepository.deleteGroup(groupId) }
}
