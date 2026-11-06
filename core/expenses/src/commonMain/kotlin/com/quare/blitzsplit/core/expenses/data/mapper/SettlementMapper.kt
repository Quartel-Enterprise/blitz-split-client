package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.SettlementDto
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.provider.room.entity.SettlementEntity

interface SettlementMapper {
    // Why: null for a payment in a currency this version can't read, which the list skips like such a bill.
    fun toDomain(entity: SettlementEntity): Settlement?

    fun toEntity(
        settlement: Settlement,
        recordedAt: Long,
    ): SettlementEntity

    fun toEntity(dto: SettlementDto): SettlementEntity

    fun toDto(
        entity: SettlementEntity,
        userId: String,
    ): SettlementDto
}
