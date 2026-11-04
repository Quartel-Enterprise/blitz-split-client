package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

/*
 * Why: what the comments' taps send, the same in Conta and on the shared page, each sent by its ViewModel only once
 * the screen takes the tap, with the screen as its source.
 */
internal val commentAnalytics: EventAnalytics = EventAnalytics.Manual(
    setOf(
        "comment_send_clicked",
        "comment_reply_clicked",
        "comment_edit_clicked",
        "comment_delete_clicked",
        "comment_delete_confirmed",
        "comment_delete_cancelled",
        "comment_like_toggled",
        "comment_likes_clicked",
        "comment_share_clicked",
    ),
)
