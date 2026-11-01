package com.quare.blitzsplit.core.activity.domain.usecase.impl

import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.activity.domain.usecase.LoadOlderActivity

class LoadOlderActivityUseCase(
    private val repository: ActivityRepository,
) : LoadOlderActivity {
    override suspend fun invoke(): Result<Unit> = repository.loadOlder()
}
