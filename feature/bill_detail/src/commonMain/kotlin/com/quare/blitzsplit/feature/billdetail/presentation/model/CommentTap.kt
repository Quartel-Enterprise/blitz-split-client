package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

/*
 * Why: the taps of "Comentários", the same in Conta and on the shared page, whose events mirror each other: each tap's
 * event, which its ViewModel sends only once the screen takes the tap, with the screen as its source.
 */
internal enum class CommentTap(
    val analytics: EventAnalytics,
) {
    SEND(EventAnalytics.Manual("comment_send_clicked")),
    REPLY(EventAnalytics.Manual("comment_reply_clicked")),
    REPLY_CANCEL(EventAnalytics.Manual("comment_reply_cancelled")),
    EDIT(EventAnalytics.Manual("comment_edit_clicked")),
    EDIT_CANCEL(EventAnalytics.Manual("comment_edit_cancelled")),
    DELETE(EventAnalytics.Manual("comment_delete_clicked")),
    DELETE_CONFIRM(EventAnalytics.Manual("comment_delete_confirmed")),
    DELETE_CANCEL(EventAnalytics.Manual("comment_delete_cancelled")),
    LIKE(EventAnalytics.Manual("comment_like_toggled")),
    LIKES(EventAnalytics.Manual("comment_likes_clicked")),
    SHARE(EventAnalytics.Manual("comment_share_clicked")),
}
