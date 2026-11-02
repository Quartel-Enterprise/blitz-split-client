package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import kotlinx.coroutines.flow.Flow

interface BillCommentRepository {
    // Why: a bill's comments in the order they were written.
    fun observeComments(expenseId: String): Flow<List<BillComment>>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: the comments waiting to go up, so a new one triggers a sync after a failed push.
    fun observePendingIds(): Flow<List<String>>

    // Why: on the device first, offline included (7.3f); the sync takes it up after its bill.
    suspend fun addComment(comment: BillComment): Result<Unit>

    suspend fun uploadPending(userId: String): Result<Unit>

    suspend fun download(userId: String): Result<Unit>
}
