package com.quare.blitzsplit.core.activity.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.activity.domain.usecase.MarkActivityTypeRead
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.model.userIdOrNull
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.first

class MarkActivityTypeReadUseCase(
    private val repository: ActivityRepository,
    private val observeAccountStatus: ObserveAccountStatus,
) : MarkActivityTypeRead {
    // Why: each read waits on the device, offline too, and goes up with the next sync like a tap on the item.
    override suspend fun invoke(type: NotificationType): Result<Unit> = suspendRunCatching {
        val readState = repository.observeReadState().first()
        val myUserId = observeAccountStatus().first().userIdOrNull()
        repository
            .observeEvents()
            .first()
            .filter { it.kind.notificationType == type && readState.isUnread(it, myUserId) }
            .forEach { repository.saveRead(it.id).getOrThrow() }
    }
}
