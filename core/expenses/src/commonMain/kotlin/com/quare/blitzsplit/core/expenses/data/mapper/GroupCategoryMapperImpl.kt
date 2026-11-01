package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.GroupCategoryDto
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
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
        isDeleted = dto.isDeleted,
        addedAt = dto.addedAt,
        updatedAt = dto.updatedAt,
        pendingSync = false,
    )

    /*
     * Why: an upsert is checked against the server's insert policy as sent, also for a category that exists, so it
     * always goes up as created by whoever pushes; the server keeps who really created it.
     */
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
        isDeleted = entity.isDeleted,
        addedAt = entity.addedAt,
        updatedAt = entity.updatedAt,
    )
}
