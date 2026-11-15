package com.quare.blitzsplit.core.support.domain.usecase

import com.quare.blitzsplit.core.support.domain.model.ReviewMoment

// Why: true when the platform should ask for a review now, which is then remembered as asked.
fun interface RequestReviewIfNeeded {
    suspend operator fun invoke(moment: ReviewMoment): Boolean
}
