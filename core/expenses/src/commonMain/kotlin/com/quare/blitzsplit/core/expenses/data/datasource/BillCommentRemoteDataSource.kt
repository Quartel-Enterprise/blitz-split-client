package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.BillCommentDto

interface BillCommentRemoteDataSource {
    suspend fun insertComments(comments: List<BillCommentDto>)

    // Why: every comment of the user's groups, page by page.
    suspend fun fetchComments(): List<BillCommentDto>
}
