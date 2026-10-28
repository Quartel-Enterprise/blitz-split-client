package com.quare.blitzsplit.core.profile.data.mapper

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.profile.data.dto.PaymentKeyDto
import com.quare.blitzsplit.core.provider.room.entity.PaymentKeyEntity

class PaymentKeyMapperImpl : PaymentKeyMapper {
    /*
     * Why: two devices can each flag a different default offline, so the most recently updated flagged key
     * wins, and with none flagged the oldest key is the default. The list is Pix first, each method's default
     * first and then in the order the keys were added.
     */
    override fun toDomain(entities: List<PaymentKeyEntity>): List<PaymentKey> {
        val live = entities.filterNot { it.isDeleted }.mapNotNull { entity ->
            typeOf(entity.type)?.let { type -> entity to type }
        }
        return PaymentMethod.entries.flatMap { method ->
            val ofMethod = live.filter { (_, type) -> type.method == method }.sortedBy { (entity, _) -> entity.addedAt }
            val defaultId = ofMethod
                .filter { (entity, _) -> entity.isDefault }
                .maxByOrNull { (entity, _) -> entity.updatedAt }
                ?.first
                ?.id
                ?: ofMethod.firstOrNull()?.first?.id
            ofMethod
                .map { (entity, type) -> toDomain(entity, type, isDefault = entity.id == defaultId) }
                .sortedByDescending { it.isDefault }
        }
    }

    override fun toEntity(
        key: PaymentKey,
        updatedAt: Long,
        ownerUserId: String?,
    ): PaymentKeyEntity = PaymentKeyEntity(
        id = key.id,
        type = codeOf(key.type),
        value = key.value,
        isDefault = key.isDefault,
        isDeleted = false,
        addedAt = key.addedAt,
        updatedAt = updatedAt,
        pendingSync = true,
        ownerUserId = ownerUserId,
    )

    override fun toDto(
        entity: PaymentKeyEntity,
        userId: String,
    ): PaymentKeyDto = PaymentKeyDto(
        id = entity.id,
        userId = userId,
        type = entity.type,
        value = entity.value,
        isDefault = entity.isDefault,
        isDeleted = entity.isDeleted,
        addedAt = entity.addedAt,
        updatedAt = entity.updatedAt,
    )

    override fun toEntity(
        dto: PaymentKeyDto,
        ownerUserId: String,
    ): PaymentKeyEntity = PaymentKeyEntity(
        id = dto.id,
        type = dto.type,
        value = dto.value,
        isDefault = dto.isDefault,
        isDeleted = dto.isDeleted,
        addedAt = dto.addedAt,
        updatedAt = dto.updatedAt,
        pendingSync = false,
        ownerUserId = ownerUserId,
    )

    private fun toDomain(
        entity: PaymentKeyEntity,
        type: PaymentKeyType,
        isDefault: Boolean,
    ): PaymentKey = PaymentKey(
        id = entity.id,
        type = type,
        value = entity.value,
        isDefault = isDefault,
        addedAt = entity.addedAt,
    )

    // Why: the server's `type` column, lowercase; a type a newer app version added is skipped, not crashed on.
    private fun typeOf(code: String): PaymentKeyType? = PaymentKeyType.entries.find { codeOf(it) == code }

    private fun codeOf(type: PaymentKeyType): String = type.name.lowercase()
}
