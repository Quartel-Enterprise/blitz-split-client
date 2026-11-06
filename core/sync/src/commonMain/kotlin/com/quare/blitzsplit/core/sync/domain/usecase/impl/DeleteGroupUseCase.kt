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
 * Why: a notice of an earlier attempt still queued goes, as this one answers anew. A group the server deleted stays
 * hidden, so a local delete that failed (which the next sync redoes) never brings it back meanwhile. A request that
 * reached for the server and failed may hide a deletion it made anyway (an answer lost on the way), so a sync settles
 * it first; a sync that failed before the request already says the server wasn't reached, and one more wouldn't help.
 * Only a group still on the device then says it wasn't deleted, which also keeps a sign out meanwhile from hearing of
 * the previous account's group.
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
            var wasAsked = false
            val result = coordinator.runGroupExit {
                wasAsked = true
                groupRepository.deleteGroup(groupId)
            }
            val failure = when {
                result.isFailure -> GroupDeleteFailure.FAILED
                result.getOrNull() == GroupExitOutcome.HAS_BALANCE -> GroupDeleteFailure.HAS_BALANCE
                else -> return@runInBackground
            }
            if (failure == GroupDeleteFailure.FAILED && wasAsked) coordinator.syncNow()
            groupRepository.showGroup(groupId)
            val isStillOnDevice = groupRepository.observeGroups().first().any { it.id == groupId }
            if (isStillOnDevice) {
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
