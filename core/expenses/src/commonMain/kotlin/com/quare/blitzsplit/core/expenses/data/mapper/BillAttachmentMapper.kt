package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillAttachmentDto
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentEntity
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentRow

interface BillAttachmentMapper {
    // Why: null for a kind this version doesn't know, which it leaves out.
    fun toDomain(row: BillAttachmentRow): BillAttachment?

    fun toEntity(
        attachment: BillAttachment,
        jpeg: ByteArray,
    ): BillAttachmentEntity

    fun toEntity(dto: BillAttachmentDto): BillAttachmentEntity

    fun toDto(
        entity: BillAttachmentEntity,
        userId: String,
    ): BillAttachmentDto
}
