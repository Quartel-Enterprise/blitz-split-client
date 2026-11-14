package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.CustomColorDto
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.provider.room.entity.CustomColorEntity

class CustomColorMapperImpl : CustomColorMapper {
    override fun toDomainOrNull(entity: CustomColorEntity): CustomColor? = CustomColor.fromHexOrNull(entity.color)

    override fun toEntity(
        dto: CustomColorDto,
        ownerUserId: String,
    ): CustomColorEntity = CustomColorEntity(
        color = dto.color,
        isDeleted = dto.isDeleted,
        addedAt = dto.addedAt,
        updatedAt = dto.updatedAt,
        pendingSync = false,
        ownerUserId = ownerUserId,
    )

    override fun toDto(
        entity: CustomColorEntity,
        userId: String,
    ): CustomColorDto = CustomColorDto(
        userId = userId,
        color = entity.color,
        isDeleted = entity.isDeleted,
        addedAt = entity.addedAt,
        updatedAt = entity.updatedAt,
    )
}
