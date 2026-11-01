package com.quare.blitzsplit.core.activity.data.mapper

import com.quare.blitzsplit.core.activity.data.dto.ActivityEventDto
import com.quare.blitzsplit.core.activity.data.dto.ChargeEventDto
import com.quare.blitzsplit.core.activity.domain.model.ActivityChange
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityField
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.model.ActivityValue
import com.quare.blitzsplit.core.activity.domain.model.ChargeSent
import com.quare.blitzsplit.core.activity.domain.model.QueuedChange
import com.quare.blitzsplit.core.provider.room.entity.ActivityEventEntity
import com.quare.blitzsplit.core.provider.room.entity.QueuedBillRow
import com.quare.blitzsplit.core.provider.room.entity.QueuedGroupRow
import com.quare.blitzsplit.core.provider.room.entity.QueuedPaymentRow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class ActivityMapperImpl : ActivityMapper {
    private val json = Json { ignoreUnknownKeys = true }

    override fun toDomain(entity: ActivityEventEntity): ActivityEvent? {
        val kind = ActivityKind.fromCodeOrNull(entity.kind) ?: return null
        val data =
            runCatching { json.parseToJsonElement(entity.data) as? JsonObject }.getOrNull() ?: JsonObject(emptyMap())
        return ActivityEvent(
            id = entity.id,
            seq = entity.seq,
            groupId = entity.groupId,
            kind = kind,
            actorUserId = entity.actorUserId,
            actorMemberId = entity.actorMemberId,
            actorName = data.findText("actor_name"),
            groupName = data.findText("group_name"),
            targetId = entity.targetId,
            occurredAt = entity.occurredAt,
            subject = data.findText("description") ?: data.findText("name"),
            amountMinor = data.findLong("amount_minor"),
            code = data.findText("split_mode") ?: data.findText("type") ?: data.findText("method"),
            fromMemberId = data.findText("from_member_id"),
            toMemberId = data.findText("to_member_id"),
            changes = (data["changes"] as? JsonArray).orEmpty().mapNotNull(::changeOf),
            isPending = entity.pendingSync,
        )
    }

    override fun toEntity(dto: ActivityEventDto): ActivityEventEntity = ActivityEventEntity(
        id = dto.id,
        seq = dto.seq,
        groupId = dto.groupId,
        kind = dto.kind,
        actorUserId = dto.actorUserId,
        actorMemberId = dto.actorMemberId,
        targetId = dto.targetId,
        occurredAt = dto.occurredAt,
        data = dto.data.toString(),
        pendingSync = false,
    )

    override fun toEntity(
        charge: ChargeSent,
        id: String,
        actorUserId: String?,
        sentAt: Long,
    ): ActivityEventEntity = ActivityEventEntity(
        id = id,
        seq = null,
        groupId = charge.groupId,
        kind = ActivityKind.CHARGE_SENT.code,
        actorUserId = actorUserId,
        actorMemberId = null,
        targetId = charge.memberId,
        occurredAt = sentAt,
        data = buildJsonObject {
            put("name", charge.memberName)
            put("amount_minor", charge.amountMinor)
        }.toString(),
        pendingSync = true,
    )

    override fun toChargeDto(
        entity: ActivityEventEntity,
        userId: String,
    ): ChargeEventDto = ChargeEventDto(
        id = entity.id,
        groupId = entity.groupId,
        kind = entity.kind,
        actorUserId = userId,
        targetId = entity.targetId,
        occurredAt = entity.occurredAt,
        data = json.parseToJsonElement(entity.data) as JsonObject,
    )

    /*
     * Why: a bill the server never recorded is new, also once edited again before it went up. One older than the
     * device's 90 days reads as new too, which only words its queued row differently.
     */
    override fun toQueued(row: QueuedBillRow): QueuedChange = QueuedChange(
        id = row.id,
        groupId = row.groupId,
        kind = when {
            row.isDeleted -> ActivityKind.BILL_DELETED
            row.hasEvent -> ActivityKind.BILL_EDITED
            else -> ActivityKind.BILL_CREATED
        },
        subject = row.description,
        amountMinor = row.amountMinor,
        code = row.splitMode,
        fromMemberId = null,
        toMemberId = null,
        savedAt = row.updatedAt,
    )

    override fun toQueued(row: QueuedPaymentRow): QueuedChange = QueuedChange(
        id = row.id,
        groupId = row.groupId,
        kind = if (row.isDeleted) ActivityKind.PAYMENT_UNDONE else ActivityKind.PAYMENT_RECORDED,
        subject = null,
        amountMinor = row.amountMinor,
        code = row.method,
        fromMemberId = row.fromMemberId,
        toMemberId = row.toMemberId,
        savedAt = row.updatedAt,
    )

    override fun toQueued(row: QueuedGroupRow): QueuedChange = QueuedChange(
        id = row.id,
        groupId = row.id,
        kind = if (row.isNew) ActivityKind.GROUP_CREATED else ActivityKind.GROUP_EDITED,
        subject = row.name,
        amountMinor = null,
        code = row.type,
        fromMemberId = null,
        toMemberId = null,
        savedAt = row.updatedAt,
    )

    private fun changeOf(element: JsonElement): ActivityChange? {
        val change = element as? JsonObject ?: return null
        val field = change.findText("field")?.let(ActivityField::fromCodeOrNull) ?: return null
        return ActivityChange(
            field = field,
            before = valueOf(field, change["before"]),
            after = valueOf(field, change["after"]),
        )
    }

    private fun valueOf(
        field: ActivityField,
        element: JsonElement?,
    ): ActivityValue {
        val primitive = element as? JsonPrimitive
        return when (field) {
            ActivityField.AMOUNT -> ActivityValue.none.copy(minor = primitive?.longOrNull)

            ActivityField.SPENT_AT -> ActivityValue.none.copy(millis = primitive?.longOrNull)

            ActivityField.DESCRIPTION, ActivityField.NAME -> ActivityValue.none.copy(text = primitive.textOrNull())

            ActivityField.TYPE, ActivityField.COLOR, ActivityField.CURRENCY ->
                ActivityValue.none.copy(code = primitive.textOrNull())

            ActivityField.PAYERS -> ActivityValue.none.copy(memberIds = memberIdsOf(element as? JsonArray))

            ActivityField.SPLIT -> {
                val split = element as? JsonObject
                ActivityValue.none.copy(
                    code = split?.findText("mode"),
                    memberIds = memberIdsOf(split?.get("shares") as? JsonArray, skipZeroWeight = true),
                )
            }

            // Why: no default split is the group's equal split among everyone.
            ActivityField.DEFAULT_SPLIT -> ActivityValue.none.copy(
                code = (element as? JsonObject)?.findText("mode") ?: EQUAL,
            )
        }
    }

    private fun memberIdsOf(
        array: JsonArray?,
        skipZeroWeight: Boolean = false,
    ): List<String> = array
        .orEmpty()
        .mapNotNull { it as? JsonObject }
        .filterNot { skipZeroWeight && it.findLong("weight") == 0L }
        .mapNotNull { it.findText("member_id") }

    private fun JsonObject.findText(key: String): String? = (this[key] as? JsonPrimitive).textOrNull()

    private fun JsonObject.findLong(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull

    private fun JsonPrimitive?.textOrNull(): String? = this?.takeUnless { it is JsonNull }?.contentOrNull

    private companion object {
        const val EQUAL = "equal"
    }
}
