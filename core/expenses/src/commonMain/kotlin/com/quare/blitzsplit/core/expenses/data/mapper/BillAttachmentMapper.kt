package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillAttachmentDto
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentEntity

interface BillAttachmentMapper {
    // Why: null for a kind this version doesn't know, which it leaves out.
    fun toDomain(entity: BillAttachmentEntity): BillAttachment?

    fun toEntity(attachment: BillAttachment): BillAttachmentEntity

    fun toEntity(dto: BillAttachmentDto): BillAttachmentEntity

    fun toDto(
        entity: BillAttachmentEntity,
        userId: String,
    ): BillAttachmentDto
}
