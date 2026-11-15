package com.quare.blitzsplit.core.support.domain.repository

import com.quare.blitzsplit.core.support.domain.model.ReviewPromptState

interface ReviewPromptRepository {
    suspend fun get(): ReviewPromptState

    // Why: the read and the write as one step, which answers with what was saved.
    suspend fun update(transform: (ReviewPromptState) -> ReviewPromptState): ReviewPromptState
}
