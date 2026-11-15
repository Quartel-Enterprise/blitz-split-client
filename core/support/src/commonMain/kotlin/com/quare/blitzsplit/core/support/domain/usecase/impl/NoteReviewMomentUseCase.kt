package com.quare.blitzsplit.core.support.domain.usecase.impl

import com.quare.blitzsplit.core.support.data.ReviewMomentInbox
import com.quare.blitzsplit.core.support.domain.model.ReviewMoment
import com.quare.blitzsplit.core.support.domain.usecase.NoteReviewMoment

class NoteReviewMomentUseCase(
    private val inbox: ReviewMomentInbox,
) : NoteReviewMoment {
    override fun invoke(moment: ReviewMoment) {
        inbox.note(moment)
    }
}
