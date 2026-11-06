package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.SettlementDto
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.provider.room.entity.SettlementEntity

interface SettlementMapper {
    // Why: a currency this version doesn't know stays as its raw code (`GroupCurrency.fromCode`), never skipped.
    fun toDomain(entity: SettlementEntity): Settlement

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
