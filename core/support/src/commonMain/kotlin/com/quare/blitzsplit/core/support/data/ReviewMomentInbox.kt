package com.quare.blitzsplit.core.support.data

import com.quare.blitzsplit.core.support.domain.model.ReviewMoment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/*
 * Why: one for the whole app. The screen that notes a moment leaves right after (a saved bill closes its form), so
 * the app's own scope is what listens; a moment nobody listens to is dropped, never kept for later.
 */
class ReviewMomentInbox {
    private val moments = MutableSharedFlow<ReviewMoment>(extraBufferCapacity = BUFFERED_MOMENTS)

    fun note(moment: ReviewMoment) {
        moments.tryEmit(moment)
    }

    fun observe(): Flow<ReviewMoment> = moments

    private companion object {
        const val BUFFERED_MOMENTS = 8
    }
}
