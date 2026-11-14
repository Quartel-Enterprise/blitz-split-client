package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillPayingMethodDto
import com.quare.blitzsplit.core.expenses.data.dto.PayingMethodDto
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.provider.room.entity.BillPayingMethodEntity
import com.quare.blitzsplit.core.provider.room.entity.PayingMethodEntity

interface PayingMethodMapper {
    // Why: the live methods in the order they were added, the default first.
    fun toDomain(entities: List<PayingMethodEntity>): List<PayingMethod>

    fun toEntity(
        method: PayingMethod,
        updatedAt: Long,
        ownerUserId: String?,
    ): PayingMethodEntity

    fun toEntity(
        dto: PayingMethodDto,
        ownerUserId: String,
    ): PayingMethodEntity

    fun toDto(
        entity: PayingMethodEntity,
        userId: String,
    ): PayingMethodDto

    fun toEntity(
        dto: BillPayingMethodDto,
        ownerUserId: String,
    ): BillPayingMethodEntity

    fun toDto(
        entity: BillPayingMethodEntity,
        userId: String,
    ): BillPayingMethodDto
}
