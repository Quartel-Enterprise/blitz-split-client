package com.quare.blitzsplit.core.groups.data.repository

import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.repository.GroupDeletionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Why: nothing to keep across launches: the group is already gone, and the notice only matters while the app runs.
class InMemoryGroupDeletionRepository : GroupDeletionRepository {
    private val deletions = MutableStateFlow(emptyList<GroupDeletion>())

    override fun observeDeletions(): Flow<List<GroupDeletion>> = deletions

    override fun post(deletion: GroupDeletion) {
        deletions.update { list -> list.filterNot { it.groupId == deletion.groupId } + deletion }
    }

    override fun consume(groupId: String) {
        deletions.update { list -> list.filterNot { it.groupId == groupId } }
    }
}
