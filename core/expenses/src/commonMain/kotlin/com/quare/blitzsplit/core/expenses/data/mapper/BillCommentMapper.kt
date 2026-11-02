package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillCommentDto
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.provider.room.entity.BillCommentEntity

interface BillCommentMapper {
    fun toDomain(entity: BillCommentEntity): BillComment

    fun toEntity(comment: BillComment): BillCommentEntity

    fun toEntity(dto: BillCommentDto): BillCommentEntity

    fun toDto(
        entity: BillCommentEntity,
        userId: String,
    ): BillCommentDto
}
