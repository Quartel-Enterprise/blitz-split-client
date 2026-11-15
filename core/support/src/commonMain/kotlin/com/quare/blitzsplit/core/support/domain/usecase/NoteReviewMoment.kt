package com.quare.blitzsplit.core.support.domain.usecase

import com.quare.blitzsplit.core.support.domain.model.ReviewMoment

// Why: a screen says what just went well and leaves; whether and when to ask for a review is decided past it.
fun interface NoteReviewMoment {
    operator fun invoke(moment: ReviewMoment)
}
