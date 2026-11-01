package com.quare.blitzsplit.core.activity.data.mapper

import com.quare.blitzsplit.core.activity.data.dto.ActivityEventDto
import com.quare.blitzsplit.core.activity.data.dto.ChargeEventDto
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ChargeSent
import com.quare.blitzsplit.core.activity.domain.model.QueuedChange
import com.quare.blitzsplit.core.provider.room.entity.ActivityEventEntity
import com.quare.blitzsplit.core.provider.room.entity.QueuedBillRow
import com.quare.blitzsplit.core.provider.room.entity.QueuedGroupRow
import com.quare.blitzsplit.core.provider.room.entity.QueuedPaymentRow

interface ActivityMapper {
    // Why: null for a kind this version doesn't know, which stays out of the feed.
    fun toDomain(entity: ActivityEventEntity): ActivityEvent?

    fun toEntity(dto: ActivityEventDto): ActivityEventEntity

    fun toEntity(
        charge: ChargeSent,
        id: String,
        actorUserId: String?,
        sentAt: Long,
    ): ActivityEventEntity

    fun toChargeDto(
        entity: ActivityEventEntity,
        userId: String,
    ): ChargeEventDto

    fun toQueued(row: QueuedBillRow): QueuedChange

    fun toQueued(row: QueuedPaymentRow): QueuedChange

    fun toQueued(row: QueuedGroupRow): QueuedChange
}
