package com.quare.blitzsplit.core.activity.domain.usecase.impl

import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.activity.domain.usecase.MarkActivityRead

class MarkActivityReadUseCase(
    private val repository: ActivityRepository,
) : MarkActivityRead {
    override suspend fun invoke(eventId: String): Result<Unit> = repository.saveRead(eventId)
}
