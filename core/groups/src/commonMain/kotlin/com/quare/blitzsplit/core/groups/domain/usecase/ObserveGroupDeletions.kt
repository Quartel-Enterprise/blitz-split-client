package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import kotlinx.coroutines.flow.Flow

fun interface ObserveGroupDeletions {
    // Why: the groups someone else deleted whose "Grupo apagado" no screen showed yet, the oldest first.
    operator fun invoke(): Flow<List<GroupDeletion>>
}
