package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.repository.BillShareLinkRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.LikeSharedBillComment

class LikeSharedBillCommentUseCase(
    private val repository: BillShareLinkRepository,
) : LikeSharedBillComment {
    override suspend fun invoke(
        code: String,
        id: String,
        isLiked: Boolean,
    ): Result<SharedBillComment> = repository.likeSharedBillComment(code, id, isLiked)
}
