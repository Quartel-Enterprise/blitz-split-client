package com.quare.blitzsplit.core.activity.domain.usecase.impl

import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveBillHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveBillHistoryUseCase(
    private val repository: ActivityRepository,
) : ObserveBillHistory {
    override fun invoke(billId: String): Flow<List<ActivityEvent>> = repository
        .observeEventsAbout(billId)
        .map { events -> events.filter { it.kind != ActivityKind.COMMENT_ADDED } }
}
