package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.CategoryTermDto
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTermChange
import com.quare.blitzsplit.core.provider.room.entity.CategoryTermEntity

interface CategoryTermMapper {
    fun toDomain(entity: CategoryTermEntity): CategoryTermChange

    fun toEntity(
        dto: CategoryTermDto,
        ownerUserId: String,
    ): CategoryTermEntity

    fun toDto(
        entity: CategoryTermEntity,
        userId: String,
    ): CategoryTermDto
}
