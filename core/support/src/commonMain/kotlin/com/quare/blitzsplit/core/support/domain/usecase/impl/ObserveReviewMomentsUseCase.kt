package com.quare.blitzsplit.core.support.domain.usecase.impl

import com.quare.blitzsplit.core.support.data.ReviewMomentInbox
import com.quare.blitzsplit.core.support.domain.model.ReviewMoment
import com.quare.blitzsplit.core.support.domain.usecase.ObserveReviewMoments
import kotlinx.coroutines.flow.Flow

class ObserveReviewMomentsUseCase(
    private val inbox: ReviewMomentInbox,
) : ObserveReviewMoments {
    override fun invoke(): Flow<ReviewMoment> = inbox.observe()
}
