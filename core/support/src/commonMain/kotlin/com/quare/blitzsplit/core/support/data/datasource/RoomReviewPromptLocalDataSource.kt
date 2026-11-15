package com.quare.blitzsplit.core.support.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.ReviewPromptDao
import com.quare.blitzsplit.core.provider.room.entity.ReviewPromptEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID

class RoomReviewPromptLocalDataSource(
    private val dao: ReviewPromptDao,
) : ReviewPromptLocalDataSource {
    private val empty = ReviewPromptEntity(
        id = SINGLE_ROW_ID,
        billsCreated = 0,
        firstEligibleAt = null,
        lastPromptedAt = null,
        lastPromptedVersion = null,
    )

    override suspend fun get(): ReviewPromptEntity? = dao.get()

    override suspend fun update(transform: (ReviewPromptEntity) -> ReviewPromptEntity): ReviewPromptEntity =
        dao.update(empty, transform)
}
