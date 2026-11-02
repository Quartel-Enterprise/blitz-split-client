package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.BillCommentDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseBillCommentRemoteDataSource(
    private val postgrest: Postgrest,
) : BillCommentRemoteDataSource {
    // Why: a comment that went up once and whose answer was lost goes up again as the same one, which stays as it was.
    override suspend fun insertComments(comments: List<BillCommentDto>) {
        postgrest.from(BILL_COMMENTS).upsert(comments) { ignoreDuplicates = true }
    }

    override suspend fun fetchComments(): List<BillCommentDto> = postgrest.fetchAllById<BillCommentDto>(
        table = BILL_COMMENTS,
        idOf = { it.id },
    )

    private companion object {
        const val BILL_COMMENTS = "bill_comments"
    }
}
