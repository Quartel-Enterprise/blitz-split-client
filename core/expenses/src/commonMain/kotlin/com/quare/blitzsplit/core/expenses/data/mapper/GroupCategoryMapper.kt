package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.GroupCategoryDto
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.provider.room.entity.GroupCategoryEntity

interface GroupCategoryMapper {
    fun toDomain(entity: GroupCategoryEntity): GroupCategory

    fun toEntity(
        category: GroupCategory,
        createdAt: Long,
    ): GroupCategoryEntity

    fun toEntity(dto: GroupCategoryDto): GroupCategoryEntity

    fun toDto(
        entity: GroupCategoryEntity,
        userId: String,
    ): GroupCategoryDto
}
