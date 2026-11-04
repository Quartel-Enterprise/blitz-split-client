package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.BillCommentDto
import com.quare.blitzsplit.core.expenses.data.dto.BillCommentLikeDto
import com.quare.blitzsplit.core.expenses.data.dto.RemoteBillCommentLikeDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseBillCommentRemoteDataSource(
    private val postgrest: Postgrest,
) : BillCommentRemoteDataSource {
    /*
     * Why: a comment sent again (its answer lost) changes nothing, nor does an older edit; only its author's newer edit
     * or the tombstone change it, which the server decides.
     */
    override suspend fun upsertComments(comments: List<BillCommentDto>) {
        postgrest.from(BILL_COMMENTS).upsert(comments)
    }

    override suspend fun fetchComments(): List<BillCommentDto> = postgrest.fetchAllById<BillCommentDto>(
        table = BILL_COMMENTS,
        idOf = { it.id },
    )

    // Why: one row per person and comment, the newer toggle kept by the server.
    override suspend fun upsertLikes(likes: List<BillCommentLikeDto>) {
        postgrest.from(BILL_COMMENT_LIKES).upsert(likes) { onConflict = "comment_id,user_id" }
    }

    override suspend fun fetchLikes(): List<RemoteBillCommentLikeDto> = postgrest.fetchAllById<RemoteBillCommentLikeDto>(
        table = BILL_COMMENT_LIKES,
        idOf = { it.id },
    )

    private companion object {
        const val BILL_COMMENTS = "bill_comments"
        const val BILL_COMMENT_LIKES = "bill_comment_likes"
    }
}
