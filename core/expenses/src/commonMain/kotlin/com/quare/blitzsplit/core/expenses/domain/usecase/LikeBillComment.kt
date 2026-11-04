package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface LikeBillComment {
    // Why: isLiked false undoes it; the liker is this device's member, by the name the others see.
    suspend operator fun invoke(
        commentId: String,
        groupId: String,
        isLiked: Boolean,
        memberId: String?,
        likerName: String?,
    ): Result<Unit>
}
