package com.quare.blitzsplit.core.profile.data.mapper

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.profile.data.dto.PaymentKeyDto
import com.quare.blitzsplit.core.provider.room.entity.PaymentKeyEntity

interface PaymentKeyMapper {
    // Why: drops tombstones and resolves each method's default, as every reader of the keys must.
    fun toDomain(entities: List<PaymentKeyEntity>): List<PaymentKey>

    fun toEntity(
        key: PaymentKey,
        updatedAt: Long,
        ownerUserId: String?,
    ): PaymentKeyEntity

    fun toDto(
        entity: PaymentKeyEntity,
        userId: String,
    ): PaymentKeyDto

    fun toEntity(
        dto: PaymentKeyDto,
        ownerUserId: String,
    ): PaymentKeyEntity
}
