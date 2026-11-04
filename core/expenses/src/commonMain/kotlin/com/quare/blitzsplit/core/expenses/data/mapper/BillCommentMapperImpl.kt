package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillCommentDto
import com.quare.blitzsplit.core.expenses.data.dto.BillCommentLikeDto
import com.quare.blitzsplit.core.expenses.data.dto.RemoteBillCommentLikeDto
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLike
import com.quare.blitzsplit.core.provider.room.entity.BillCommentEntity
import com.quare.blitzsplit.core.provider.room.entity.BillCommentLikeEntity

class BillCommentMapperImpl : BillCommentMapper {
    // Why: in the app the author's member says whose it is, so isMine is a link page's only.
    override fun toDomain(
        entity: BillCommentEntity,
        likes: List<BillCommentLikeEntity>,
    ): BillComment = BillComment(
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
        editedAt = entity.editedAt,
        isDeleted = entity.isDeleted,
        isMine = false,
        likes = likes.map(::toDomain),
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
        editedAt = comment.editedAt,
        isDeleted = comment.isDeleted,
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
        editedAt = dto.editedAt,
        isDeleted = dto.isDeleted,
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
        editedAt = entity.editedAt,
        isDeleted = entity.isDeleted,
    )

    override fun toDomain(like: BillCommentLikeEntity): BillCommentLike = BillCommentLike(
        memberId = like.memberId,
        name = like.likerName,
        isMine = like.likerKey == BillCommentLikeEntity.ME,
    )

    override fun toEntity(
        dto: RemoteBillCommentLikeDto,
        userId: String,
    ): BillCommentLikeEntity = BillCommentLikeEntity(
        commentId = dto.commentId,
        likerKey = if (dto.userId == userId) BillCommentLikeEntity.ME else dto.userId,
        groupId = dto.groupId,
        memberId = dto.memberId,
        likerName = dto.likerName,
        isLiked = dto.isLiked,
        updatedAt = dto.updatedAt,
        pendingSync = false,
    )

    override fun toDto(
        like: BillCommentLikeEntity,
        userId: String,
    ): BillCommentLikeDto = BillCommentLikeDto(
        commentId = like.commentId,
        userId = userId,
        groupId = like.groupId,
        isLiked = like.isLiked,
        updatedAt = like.updatedAt,
    )
}
