package com.quare.blitzsplit.core.activity.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.activity.domain.model.ActivityFeed
import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityFeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveActivityFeedUseCase(
    private val repository: ActivityRepository,
    private val observeAccountStatus: ObserveAccountStatus,
) : ObserveActivityFeed {
    override fun invoke(): Flow<ActivityFeed> = combine(
        repository.observeEvents(),
        repository.observeQueued(),
        repository.observeReadState(),
        repository.observeTargets(),
        combine(
            observeAccountStatus().map { it.userIdOrNull() }.distinctUntilChanged(),
            repository.observeHasOlder(),
            ::Pair,
        ),
    ) { events, queued, readState, targets, (myUserId, hasOlder) ->
        ActivityFeed(
            events = events,
            queued = queued,
            readState = readState,
            targets = targets,
            myUserId = myUserId,
            hasOlder = hasOlder,
        )
    }
}
