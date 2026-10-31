package com.quare.blitzsplit.core.groups.domain.repository

import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import kotlinx.coroutines.flow.Flow

// Why: the groups someone else deleted that this device still owes "Grupo apagado" for, while the app runs.
interface GroupDeletionRepository {
    // Why: the oldest first.
    fun observeDeletions(): Flow<List<GroupDeletion>>

    fun post(deletion: GroupDeletion)

    // Why: shown once, by whichever screen was on top.
    fun consume(groupId: String)
}
