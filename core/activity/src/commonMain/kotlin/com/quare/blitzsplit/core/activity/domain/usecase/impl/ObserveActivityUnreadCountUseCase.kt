package com.quare.blitzsplit.core.activity.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveActivityUnreadCountUseCase(
    private val repository: ActivityRepository,
    private val observeAccountStatus: ObserveAccountStatus,
) : ObserveActivityUnreadCount {
    override fun invoke(): Flow<Int> = combine(
        repository.observeEvents(),
        repository.observeReadState(),
        observeAccountStatus(),
    ) { events, readState, status ->
        val myUserId = status.userIdOrNull()
        events.count { readState.isUnread(it, myUserId) }
    }.distinctUntilChanged()
}
