package com.quare.blitzsplit.feature.billdetail.presentation.viewmodel

import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.SHARED_BILL_SOURCE

// Why: "Enviar" on a bill's link page; an edit saves only its text, whatever reply was picked meanwhile.
internal fun TrackEvent.commentSendClicked(
    isEdit: Boolean,
    hasReplyTarget: Boolean,
) {
    this(
        "comment_send_clicked",
        mapOf(
            "is_reply" to (!isEdit && hasReplyTarget),
            "is_edit" to isEdit,
            "source" to SHARED_BILL_SOURCE,
        ),
    )
}
