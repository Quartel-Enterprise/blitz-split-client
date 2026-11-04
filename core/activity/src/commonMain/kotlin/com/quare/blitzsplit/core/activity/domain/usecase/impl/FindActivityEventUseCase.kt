package com.quare.blitzsplit.core.activity.domain.usecase.impl

import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.activity.domain.usecase.FindActivityEvent

class FindActivityEventUseCase(
    private val repository: ActivityRepository,
) : FindActivityEvent {
    override suspend fun invoke(eventId: String): ActivityEvent? = repository.findEvent(eventId)
}
