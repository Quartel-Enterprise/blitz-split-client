package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.domain.repository.GroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.model.GroupDeleteFailure
import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import kotlinx.coroutines.flow.first

/*
 * Why: a notice of an earlier attempt still queued goes, as this one answers anew. Once the server answered the group
 * is shown again: deleted, it already left the device; refused, it comes back. A failure may hide a deletion the
 * server made anyway (an answer lost on the way), so a sync settles it first, and only a group still on the device
 * says it wasn't deleted; that also keeps a sign out meanwhile from hearing of the previous account's group.
 */
class DeleteGroupUseCase(
    private val groupRepository: GroupRepository,
    private val deletionRepository: GroupDeletionRepository,
    private val coordinator: SyncCoordinator,
) : DeleteGroup {
    override fun invoke(
        groupId: String,
        groupName: String,
    ) {
        deletionRepository.consume(groupId)
        groupRepository.hideGroup(groupId)
        coordinator.runInBackground {
            val result = coordinator.runGroupExit { groupRepository.deleteGroup(groupId) }
            val failure = when {
                result.isFailure -> GroupDeleteFailure.FAILED
                result.getOrNull() == GroupExitOutcome.HAS_BALANCE -> GroupDeleteFailure.HAS_BALANCE
                else -> null
            }
            if (failure == GroupDeleteFailure.FAILED) coordinator.syncNow()
            groupRepository.showGroup(groupId)
            val isStillOnDevice = groupRepository.observeGroups().first().any { it.id == groupId }
            if (failure != null && isStillOnDevice) {
                deletionRepository.post(
                    GroupDeletion(
                        groupId = groupId,
                        groupName = groupName,
                        deletedByName = null,
                        failure = failure,
                    ),
                )
            }
        }
    }
}
