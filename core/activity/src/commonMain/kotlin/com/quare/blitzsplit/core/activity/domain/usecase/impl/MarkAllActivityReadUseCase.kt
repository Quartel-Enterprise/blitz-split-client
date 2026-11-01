package com.quare.blitzsplit.core.activity.domain.usecase.impl

import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.activity.domain.usecase.MarkAllActivityRead
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlinx.coroutines.flow.first

class MarkAllActivityReadUseCase(
    private val repository: ActivityRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : MarkAllActivityRead {
    // Why: up to the newest event known; one arriving later is still new.
    override suspend fun invoke(): Result<Unit> {
        val newestSeq = repository
            .observeEvents()
            .first()
            .mapNotNull { it.seq }
            .maxOrNull()
            ?: return Result.success(Unit)
        return repository.saveReadThrough(
            throughSeq = newestSeq,
            now = currentTimeMillis(),
        )
    }
}
