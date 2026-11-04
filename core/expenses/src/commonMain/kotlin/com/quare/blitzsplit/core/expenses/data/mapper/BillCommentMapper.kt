package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillCommentDto
import com.quare.blitzsplit.core.expenses.data.dto.BillCommentLikeDto
import com.quare.blitzsplit.core.expenses.data.dto.RemoteBillCommentLikeDto
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLike
import com.quare.blitzsplit.core.provider.room.entity.BillCommentEntity
import com.quare.blitzsplit.core.provider.room.entity.BillCommentLikeEntity

interface BillCommentMapper {
    fun toDomain(
        entity: BillCommentEntity,
        likes: List<BillCommentLikeEntity>,
    ): BillComment

    fun toEntity(comment: BillComment): BillCommentEntity

    fun toEntity(dto: BillCommentDto): BillCommentEntity

    fun toDto(
        entity: BillCommentEntity,
        userId: String,
    ): BillCommentDto

    fun toDomain(like: BillCommentLikeEntity): BillCommentLike

    // Why: the user's own like comes down under ME, as it is kept on the device.
    fun toEntity(
        dto: RemoteBillCommentLikeDto,
        userId: String,
    ): BillCommentLikeEntity

    fun toDto(
        like: BillCommentLikeEntity,
        userId: String,
    ): BillCommentLikeDto
}
