package com.quare.blitzsplit.core.support.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.ReviewPromptEntity

interface ReviewPromptLocalDataSource {
    // Why: null until the device has something to remember.
    suspend fun get(): ReviewPromptEntity?

    suspend fun update(transform: (ReviewPromptEntity) -> ReviewPromptEntity): ReviewPromptEntity
}
