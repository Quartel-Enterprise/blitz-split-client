package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillCommentDto
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.provider.room.entity.BillCommentEntity

class BillCommentMapperImpl : BillCommentMapper {
    override fun toDomain(entity: BillCommentEntity): BillComment = BillComment(
        id = entity.id,
        expenseId = entity.expenseId,
        groupId = entity.groupId,
        authorUserId = entity.authorUserId,
        authorMemberId = entity.authorMemberId,
        authorName = entity.authorName,
        threadId = entity.threadId,
        replyToName = entity.replyToName,
        body = entity.body,
        viaLink = entity.viaLink,
        postedAt = entity.postedAt,
        isPending = entity.pendingSync,
    )

    override fun toEntity(comment: BillComment): BillCommentEntity = BillCommentEntity(
        id = comment.id,
        expenseId = comment.expenseId,
        groupId = comment.groupId,
        authorUserId = comment.authorUserId,
        authorMemberId = comment.authorMemberId,
        authorName = comment.authorName,
        threadId = comment.threadId,
        replyToName = comment.replyToName,
        body = comment.body,
        viaLink = comment.viaLink,
        postedAt = comment.postedAt,
        pendingSync = comment.isPending,
    )

    override fun toEntity(dto: BillCommentDto): BillCommentEntity = BillCommentEntity(
        id = dto.id,
        expenseId = dto.expenseId,
        groupId = dto.groupId,
        authorUserId = dto.authorUserId,
        authorMemberId = dto.authorMemberId,
        authorName = dto.authorName,
        threadId = dto.threadId,
        replyToName = dto.replyToName,
        body = dto.body,
        viaLink = dto.viaLink,
        postedAt = dto.postedAt,
        pendingSync = false,
    )

    // Why: the policy wants the caller as the author, which the server stamps again anyway.
    override fun toDto(
        entity: BillCommentEntity,
        userId: String,
    ): BillCommentDto = BillCommentDto(
        id = entity.id,
        expenseId = entity.expenseId,
        groupId = entity.groupId,
        authorUserId = userId,
        authorMemberId = entity.authorMemberId,
        authorName = entity.authorName,
        threadId = entity.threadId,
        replyToName = entity.replyToName,
        body = entity.body,
        viaLink = entity.viaLink,
        postedAt = entity.postedAt,
    )
}
