package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.SettlementDto
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.provider.room.entity.SettlementEntity

class SettlementMapperImpl : SettlementMapper {
    override fun toDomain(entity: SettlementEntity): Settlement = Settlement(
        id = entity.id,
        groupId = entity.groupId,
        fromMemberId = entity.fromMemberId,
        toMemberId = entity.toMemberId,
        amountMinor = entity.amountMinor,
        method = SettlementMethod.fromCode(entity.method),
        paidAt = entity.paidAt,
        isPending = entity.pendingSync,
        updatedAt = entity.updatedAt,
    )

    override fun toEntity(
        settlement: Settlement,
        recordedAt: Long,
    ): SettlementEntity = SettlementEntity(
        id = settlement.id,
        groupId = settlement.groupId,
        createdBy = null,
        fromMemberId = settlement.fromMemberId,
        toMemberId = settlement.toMemberId,
        amountMinor = settlement.amountMinor,
        method = settlement.method.code,
        paidAt = settlement.paidAt,
        isDeleted = false,
        addedAt = recordedAt,
        updatedAt = recordedAt,
        pendingSync = true,
    )

    override fun toEntity(dto: SettlementDto): SettlementEntity = SettlementEntity(
        id = dto.id,
        groupId = dto.groupId,
        createdBy = dto.createdBy,
        fromMemberId = dto.fromMemberId,
        toMemberId = dto.toMemberId,
        amountMinor = dto.amountMinor,
        method = dto.method,
        paidAt = dto.paidAt,
        isDeleted = dto.isDeleted,
        addedAt = dto.addedAt,
        updatedAt = dto.updatedAt,
        pendingSync = false,
    )

    /*
     * Why: an upsert is checked against the server's insert policy as sent, also for a payment that exists, so it
     * always goes up as recorded by whoever pushes; the server keeps who really recorded it.
     */
    override fun toDto(
        entity: SettlementEntity,
        userId: String,
    ): SettlementDto = SettlementDto(
        id = entity.id,
        groupId = entity.groupId,
        createdBy = userId,
        fromMemberId = entity.fromMemberId,
        toMemberId = entity.toMemberId,
        amountMinor = entity.amountMinor,
        method = entity.method,
        paidAt = entity.paidAt,
        isDeleted = entity.isDeleted,
        addedAt = entity.addedAt,
        updatedAt = entity.updatedAt,
    )
}
