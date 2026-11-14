package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillPayingMethodDto
import com.quare.blitzsplit.core.expenses.data.dto.PayingMethodDto
import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.core.provider.room.entity.BillPayingMethodEntity
import com.quare.blitzsplit.core.provider.room.entity.PayingMethodEntity

class PayingMethodMapperImpl : PayingMethodMapper {
    /*
     * Why: two devices can each flag a different default offline, so the most recently updated flagged method wins;
     * with none flagged there is no default. A type this version doesn't know (a newer app's) is left out.
     */
    override fun toDomain(entities: List<PayingMethodEntity>): List<PayingMethod> {
        val live = entities
            .filterNot { it.isDeleted }
            .mapNotNull { entity -> PayingMethodType.ofCode(entity.type)?.let { entity to it } }
            .sortedWith(compareBy({ (entity, _) -> entity.addedAt }, { (entity, _) -> entity.id }))
        val defaultId = live
            .filter { (entity, _) -> entity.isDefault }
            .maxByOrNull { (entity, _) -> entity.updatedAt }
            ?.first
            ?.id
        return live
            .map { (entity, type) ->
                PayingMethod(
                    id = entity.id,
                    type = type,
                    name = entity.name,
                    cardColor = if (type.isCard) CardColor.ofCode(entity.icon) else null,
                    isDefault = entity.id == defaultId,
                    addedAt = entity.addedAt,
                )
            }.sortedByDescending { it.isDefault }
    }

    override fun toEntity(
        method: PayingMethod,
        updatedAt: Long,
        ownerUserId: String?,
    ): PayingMethodEntity = PayingMethodEntity(
        id = method.id,
        type = method.type.code,
        name = method.name.trim(),
        icon = method.cardColor
            ?.takeIf { method.type.isCard }
            ?.code
            .orEmpty(),
        isDefault = method.isDefault,
        isDeleted = false,
        addedAt = method.addedAt,
        updatedAt = updatedAt,
        pendingSync = true,
        ownerUserId = ownerUserId,
    )

    override fun toEntity(
        dto: PayingMethodDto,
        ownerUserId: String,
    ): PayingMethodEntity = PayingMethodEntity(
        id = dto.id,
        type = dto.type,
        name = dto.name,
        icon = dto.icon,
        isDefault = dto.isDefault,
        isDeleted = dto.isDeleted,
        addedAt = dto.addedAt,
        updatedAt = dto.updatedAt,
        pendingSync = false,
        ownerUserId = ownerUserId,
    )

    override fun toDto(
        entity: PayingMethodEntity,
        userId: String,
    ): PayingMethodDto = PayingMethodDto(
        id = entity.id,
        userId = userId,
        type = entity.type,
        name = entity.name,
        icon = entity.icon,
        isDefault = entity.isDefault,
        isDeleted = entity.isDeleted,
        addedAt = entity.addedAt,
        updatedAt = entity.updatedAt,
    )

    override fun toEntity(
        dto: BillPayingMethodDto,
        ownerUserId: String,
    ): BillPayingMethodEntity = BillPayingMethodEntity(
        expenseId = dto.expenseId,
        methodId = dto.methodId,
        updatedAt = dto.updatedAt,
        pendingSync = false,
        ownerUserId = ownerUserId,
    )

    override fun toDto(
        entity: BillPayingMethodEntity,
        userId: String,
    ): BillPayingMethodDto = BillPayingMethodDto(
        userId = userId,
        expenseId = entity.expenseId,
        methodId = entity.methodId,
        updatedAt = entity.updatedAt,
    )
}
