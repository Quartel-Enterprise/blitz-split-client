package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.BillCommentDto
import com.quare.blitzsplit.core.expenses.data.dto.BillCommentLikeDto
import com.quare.blitzsplit.core.expenses.data.dto.RemoteBillCommentLikeDto

interface BillCommentRemoteDataSource {
    // Why: new comments, edits and tombstones alike; the server keeps the newer edit and the tombstone.
    suspend fun upsertComments(comments: List<BillCommentDto>)

    // Why: every comment of the user's groups, page by page.
    suspend fun fetchComments(): List<BillCommentDto>

    suspend fun upsertLikes(likes: List<BillCommentLikeDto>)

    // Why: every like of the user's groups, undone ones included, page by page.
    suspend fun fetchLikes(): List<RemoteBillCommentLikeDto>
}
