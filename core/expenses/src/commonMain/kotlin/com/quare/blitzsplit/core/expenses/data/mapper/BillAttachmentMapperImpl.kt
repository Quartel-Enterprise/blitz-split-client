package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillAttachmentDto
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentEntity
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentRow

class BillAttachmentMapperImpl : BillAttachmentMapper {
    override fun toDomain(row: BillAttachmentRow): BillAttachment? {
        val kind = BillAttachmentKind.fromCodeOrNull(row.kind) ?: return null
        return BillAttachment(
            id = row.id,
            expenseId = row.expenseId,
            groupId = row.groupId,
            kind = kind,
            width = row.width,
            height = row.height,
            addedByUserId = row.addedByUserId,
            addedByMemberId = row.addedByMemberId,
            addedAt = row.addedAt,
            updatedAt = row.updatedAt,
            isPending = row.pendingSync,
            hasPendingImage = row.hasPendingJpeg,
        )
    }

    override fun toEntity(
        attachment: BillAttachment,
        jpeg: ByteArray,
    ): BillAttachmentEntity = BillAttachmentEntity(
        id = attachment.id,
        expenseId = attachment.expenseId,
        groupId = attachment.groupId,
        kind = attachment.kind.code,
        width = attachment.width,
        height = attachment.height,
        addedByUserId = attachment.addedByUserId,
        addedByMemberId = attachment.addedByMemberId,
        addedAt = attachment.addedAt,
        updatedAt = attachment.updatedAt,
        isDeleted = false,
        pendingJpeg = jpeg,
        pendingSync = attachment.isPending,
    )

    override fun toEntity(dto: BillAttachmentDto): BillAttachmentEntity = BillAttachmentEntity(
        id = dto.id,
        expenseId = dto.expenseId,
        groupId = dto.groupId,
        kind = dto.kind,
        width = dto.width,
        height = dto.height,
        addedByUserId = dto.addedByUserId,
        addedByMemberId = dto.addedByMemberId,
        addedAt = dto.addedAt,
        updatedAt = dto.updatedAt,
        isDeleted = dto.isDeleted,
        pendingJpeg = null,
        pendingSync = false,
    )

    // Why: the policy wants the caller as who added it, which the server keeps as it was for one already there.
    override fun toDto(
        entity: BillAttachmentEntity,
        userId: String,
    ): BillAttachmentDto = BillAttachmentDto(
        id = entity.id,
        expenseId = entity.expenseId,
        groupId = entity.groupId,
        kind = entity.kind,
        addedByUserId = userId,
        addedByMemberId = entity.addedByMemberId,
        width = entity.width,
        height = entity.height,
        isDeleted = entity.isDeleted,
        addedAt = entity.addedAt,
        updatedAt = entity.updatedAt,
    )
}
