package com.quare.blitzsplit.core.activity.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import com.quare.blitzsplit.core.model.userIdOrNull
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveNotificationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// Why: the bell's badge, which leaves out the rows of Notifications whose In app toggle is off.
class ObserveActivityUnreadCountUseCase(
    private val repository: ActivityRepository,
    private val observeAccountStatus: ObserveAccountStatus,
    private val observeNotificationPreferences: ObserveNotificationPreferences,
) : ObserveActivityUnreadCount {
    override fun invoke(): Flow<Int> = combine(
        repository.observeEvents(),
        repository.observeReadState(),
        observeAccountStatus(),
        observeNotificationPreferences().map { it.inApp }.distinctUntilChanged(),
    ) { events, readState, status, inAppTypes ->
        val myUserId = status.userIdOrNull()
        events.count { readState.isUnread(it, myUserId, inAppTypes) }
    }.distinctUntilChanged()
}
