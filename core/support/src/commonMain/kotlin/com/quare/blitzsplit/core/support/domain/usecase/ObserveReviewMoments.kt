package com.quare.blitzsplit.core.support.domain.usecase

import com.quare.blitzsplit.core.support.domain.model.ReviewMoment
import kotlinx.coroutines.flow.Flow

fun interface ObserveReviewMoments {
    operator fun invoke(): Flow<ReviewMoment>
}
