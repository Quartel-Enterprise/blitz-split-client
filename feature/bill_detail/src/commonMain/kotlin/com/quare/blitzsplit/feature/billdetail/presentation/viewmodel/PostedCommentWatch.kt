package com.quare.blitzsplit.feature.billdetail.presentation.viewmodel

import com.quare.blitzsplit.feature.billdetail.presentation.mapper.findComment
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentThread

/*
 * Why: the comment this device just posted is scrolled to once the screen draws it, whenever the device's copy or the
 * page read again brings it, never on a timer; a read that fails or never brings it scrolls nowhere.
 */
internal class PostedCommentWatch {
    private var postedId: String? = null

    fun posted(commentId: String) {
        postedId = commentId
    }

    // Why: the id to scroll to, once, when these threads are the first to hold it.
    fun findDrawnIn(threads: List<BillCommentThread>): String? {
        val commentId = postedId?.takeIf { threads.findComment(it) != null } ?: return null
        postedId = null
        return commentId
    }
}
