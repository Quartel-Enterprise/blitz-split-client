package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.CategoryTermDto
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTermChange
import com.quare.blitzsplit.core.provider.room.entity.CategoryTermEntity

class CategoryTermMapperImpl : CategoryTermMapper {
    override fun toDomain(entity: CategoryTermEntity): CategoryTermChange = CategoryTermChange(
        key = entity.termKey,
        term = entity.term,
        categoryId = entity.categoryId,
        isRestored = entity.isDeleted,
        isPending = entity.pendingSync,
    )

    override fun toEntity(
        dto: CategoryTermDto,
        ownerUserId: String,
    ): CategoryTermEntity = CategoryTermEntity(
        termKey = dto.termKey,
        term = dto.term,
        categoryId = dto.categoryId,
        isDeleted = dto.isDeleted,
        addedAt = dto.addedAt,
        updatedAt = dto.updatedAt,
        pendingSync = false,
        ownerUserId = ownerUserId,
    )

    override fun toDto(
        entity: CategoryTermEntity,
        userId: String,
    ): CategoryTermDto = CategoryTermDto(
        userId = userId,
        termKey = entity.termKey,
        term = entity.term,
        categoryId = entity.categoryId,
        isDeleted = entity.isDeleted,
        addedAt = entity.addedAt,
        updatedAt = entity.updatedAt,
    )
}
