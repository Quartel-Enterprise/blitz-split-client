package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.BillCommentRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.LikeBillComment
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

// Why: stamped with this device's clock, so the newer toggle wins between the user's devices.
class LikeBillCommentUseCase(
    private val repository: BillCommentRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : LikeBillComment {
    override suspend fun invoke(
        commentId: String,
        groupId: String,
        isLiked: Boolean,
        memberId: String?,
        likerName: String?,
    ): Result<Unit> = repository.updateLike(
        commentId = commentId,
        groupId = groupId,
        isLiked = isLiked,
        memberId = memberId,
        likerName = likerName,
        at = currentTimeMillis(),
    )
}
