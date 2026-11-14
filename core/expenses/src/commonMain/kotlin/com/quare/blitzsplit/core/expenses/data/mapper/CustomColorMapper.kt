package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.CustomColorDto
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.provider.room.entity.CustomColorEntity

interface CustomColorMapper {
    // Why: null for a row this version can't read, which the palette leaves out.
    fun toDomainOrNull(entity: CustomColorEntity): CustomColor?

    // Why: null for a row whose color isn't a hex; any other is keyed by its uppercase #RRGGBB, as every lookup is.
    fun toEntityOrNull(
        dto: CustomColorDto,
        ownerUserId: String,
    ): CustomColorEntity?

    fun toDto(
        entity: CustomColorEntity,
        userId: String,
    ): CustomColorDto
}
