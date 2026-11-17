package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.DismissGroupReminder

class DismissGroupReminderUseCase(
    private val repository: GroupRepository,
) : DismissGroupReminder {
    override suspend fun invoke(
        groupId: String,
        occurrenceAt: Long,
    ): Result<Unit> = repository.dismissReminder(
        groupId = groupId,
        occurrenceAt = occurrenceAt,
    )
}
