package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.GroupCategoryDto
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.provider.room.entity.GroupCategoryEntity

class GroupCategoryMapperImpl : GroupCategoryMapper {
    // Why: an icon this version doesn't know draws as "Outros"' rather than hiding the category.
    override fun toDomain(entity: GroupCategoryEntity): GroupCategory = GroupCategory(
        id = entity.id,
        groupId = entity.groupId,
        parentId = entity.parentId,
        name = entity.name,
        icon = entity.icon?.let { CategoryIcon.fromCodeOrNull(it) ?: CategoryIcon.DOTS },
        // Why: a color this version doesn't know reads as none picked, the place's default.
        color = CategoryTint.fromCodeOrNull(entity.color),
        createdByMemberId = entity.createdByMemberId,
        isDeleted = entity.isDeleted,
        isPending = entity.pendingSync,
    )

    override fun toEntity(
        category: GroupCategory,
        createdAt: Long,
    ): GroupCategoryEntity = GroupCategoryEntity(
        id = category.id,
        groupId = category.groupId,
        createdBy = null,
        createdByMemberId = category.createdByMemberId,
        parentId = category.parentId,
        name = category.name,
        icon = category.icon?.code,
        color = category.color?.code,
        isDeleted = false,
        addedAt = createdAt,
        updatedAt = createdAt,
        pendingSync = true,
    )

    override fun toEntity(dto: GroupCategoryDto): GroupCategoryEntity = GroupCategoryEntity(
        id = dto.id,
        groupId = dto.groupId,
        createdBy = dto.createdBy,
        createdByMemberId = dto.createdByMemberId,
        parentId = dto.parentId,
        name = dto.name,
        icon = dto.icon,
        color = dto.color,
        isDeleted = dto.isDeleted,
        addedAt = dto.addedAt,
        updatedAt = dto.updatedAt,
        pendingSync = false,
    )

    // Why: the server's insert policy checks each upsert as sent, so it names whoever pushes; the trigger keeps the first.
    override fun toDto(
        entity: GroupCategoryEntity,
        userId: String,
    ): GroupCategoryDto = GroupCategoryDto(
        id = entity.id,
        groupId = entity.groupId,
        createdBy = userId,
        createdByMemberId = entity.createdByMemberId,
        parentId = entity.parentId,
        name = entity.name,
        icon = entity.icon,
        color = entity.color,
        isDeleted = entity.isDeleted,
        addedAt = entity.addedAt,
        updatedAt = entity.updatedAt,
    )
}
