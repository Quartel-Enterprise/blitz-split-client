package com.quare.blitzsplit.core.support.data.repository

import com.quare.blitzsplit.core.provider.room.entity.ReviewPromptEntity
import com.quare.blitzsplit.core.support.data.datasource.ReviewPromptLocalDataSource
import com.quare.blitzsplit.core.support.domain.model.ReviewPromptState
import com.quare.blitzsplit.core.support.domain.repository.ReviewPromptRepository

class ReviewPromptRepositoryImpl(
    private val local: ReviewPromptLocalDataSource,
) : ReviewPromptRepository {
    private val nothingYet = ReviewPromptState(
        billsCreated = 0,
        firstEligibleAt = null,
        lastPromptedAt = null,
        lastPromptedVersion = null,
    )

    override suspend fun get(): ReviewPromptState = local.get()?.toState() ?: nothingYet

    override suspend fun update(transform: (ReviewPromptState) -> ReviewPromptState): ReviewPromptState = local
        .update { entity ->
            val state = transform(entity.toState())
            entity.copy(
                billsCreated = state.billsCreated,
                firstEligibleAt = state.firstEligibleAt,
                lastPromptedAt = state.lastPromptedAt,
                lastPromptedVersion = state.lastPromptedVersion,
            )
        }.toState()

    private fun ReviewPromptEntity.toState(): ReviewPromptState = ReviewPromptState(
        billsCreated = billsCreated,
        firstEligibleAt = firstEligibleAt,
        lastPromptedAt = lastPromptedAt,
        lastPromptedVersion = lastPromptedVersion,
    )
}
