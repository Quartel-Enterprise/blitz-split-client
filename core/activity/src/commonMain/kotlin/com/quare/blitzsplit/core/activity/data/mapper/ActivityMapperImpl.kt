package com.quare.blitzsplit.core.activity.data.mapper

import com.quare.blitzsplit.core.activity.data.dto.ActivityEventDto
import com.quare.blitzsplit.core.activity.data.dto.ChargeEventDto
import com.quare.blitzsplit.core.activity.domain.model.ActivityChange
import com.quare.blitzsplit.core.activity.domain.model.ActivityDebt
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityField
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.model.ActivityMemberValue
import com.quare.blitzsplit.core.activity.domain.model.ActivityValue
import com.quare.blitzsplit.core.activity.domain.model.ChargeSent
import com.quare.blitzsplit.core.activity.domain.model.QueuedChange
import com.quare.blitzsplit.core.model.GroupShape
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
            code = codeOf(kind, data),
            fromMemberId = data.findText("from_member_id"),
            toMemberId = data.findText("to_member_id"),
            changes = (data["changes"] as? JsonArray).orEmpty().mapNotNull(::changeOf),
            sharesCount = data.findLong("shares_count")?.toInt(),
            body = data.findText("body"),
            commentId = data.findText("comment_id"),
            isCommentDeleted = (data["deleted"] as? JsonPrimitive)?.contentOrNull == "true",
            isPending = entity.pendingSync,
            debtsBefore = debtsOf(data["before"]),
            debtsAfter = debtsOf(data["after"]),
            memberNames = (data["names"] as? JsonObject)
                .orEmpty()
                .mapNotNull { (id, name) -> (name as? JsonPrimitive).textOrNull()?.let { id to it } }
                .toMap(),
        )
    }

    // Why: a pair a newer server writes differently is left out rather than read wrong.
    private fun debtsOf(element: JsonElement?): List<ActivityDebt> = (element as? JsonArray)
        .orEmpty()
        .mapNotNull { it as? JsonObject }
        .mapNotNull { pair ->
            ActivityDebt(
                fromMemberId = pair.findText("from") ?: return@mapNotNull null,
                toMemberId = pair.findText("to") ?: return@mapNotNull null,
                amountMinor = pair.findLong("amount") ?: return@mapNotNull null,
            )
        }

    // Why: a photo's kind and the link's visibility by their own keys, so another key in the same data never wins.
    private fun codeOf(
        kind: ActivityKind,
        data: JsonObject,
    ): String? = when (kind) {
        ActivityKind.ATTACHMENT_ADDED, ActivityKind.ATTACHMENT_REMOVED -> data.findText("attachment_kind")
        ActivityKind.BILL_LINK_CHANGED -> data.findText("visibility")
        ActivityKind.SIMPLIFY_DEBTS_CHANGED -> simplifyCodeOf((data["simplify_debts"] as? JsonPrimitive)?.contentOrNull)
        else -> data.findText("split_mode") ?: data.findText("type") ?: data.findText("method")
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

    // Why: a switch of "Simplificar dívidas" waiting to go up words as one, even with other edits waiting with it.
    override fun toQueued(row: QueuedGroupRow): QueuedChange = QueuedChange(
        id = row.id,
        groupId = row.id,
        kind = when {
            row.isNew -> ActivityKind.GROUP_CREATED
            row.simplifyDebtsPending -> ActivityKind.SIMPLIFY_DEBTS_CHANGED
            else -> ActivityKind.GROUP_EDITED
        },
        subject = row.name,
        amountMinor = null,
        code = if (!row.isNew && row.simplifyDebtsPending) simplifyCodeOf(row.simplifyDebts.toString()) else row.type,
        fromMemberId = null,
        toMemberId = null,
        savedAt = row.updatedAt,
    )

    private fun changeOf(element: JsonElement): ActivityChange? {
        val change = element as? JsonObject ?: return null
        val field = change.findText("field")?.let(ActivityField::fromCodeOrNull) ?: return null
        return ActivityChange(
            field = field,
            before = valueOf(field, change["before"], photoCode = ActivityField.PHOTO_PREVIOUS),
            after = valueOf(field, change["after"], photoCode = ActivityField.PHOTO_NEW),
        )
    }

    /*
     * Why: a photo's URL means nothing to read, so the value only says whether there was one, as the photo before
     * or the new one (photoCode).
     */
    private fun valueOf(
        field: ActivityField,
        element: JsonElement?,
        photoCode: String,
    ): ActivityValue {
        val primitive = element as? JsonPrimitive
        return when (field) {
            ActivityField.AMOUNT -> ActivityValue.none.copy(minor = primitive?.longOrNull)

            ActivityField.SPENT_AT -> ActivityValue.none.copy(millis = primitive?.longOrNull)

            ActivityField.DESCRIPTION, ActivityField.NAME -> ActivityValue.none.copy(text = primitive.textOrNull())

            ActivityField.SHAPE -> ActivityValue.none.copy(code = primitive.textOrNull() ?: GroupShape.fallback.code)

            ActivityField.PHOTO -> ActivityValue.none.copy(code = photoCode.takeIf { primitive.textOrNull() != null })

            ActivityField.TYPE,
            ActivityField.COLOR,
            ActivityField.CURRENCY,
            ActivityField.CATEGORY,
            ActivityField.VISIBILITY,
            -> ActivityValue.none.copy(code = primitive.textOrNull())

            ActivityField.PAYERS -> {
                val payers = element as? JsonArray
                ActivityValue.none.copy(
                    memberIds = memberIdsOf(payers),
                    memberValues = memberValuesOf(payers, "amount_minor"),
                )
            }

            ActivityField.SPLIT -> {
                val split = element as? JsonObject
                val shares = split?.get("shares") as? JsonArray
                ActivityValue.none.copy(
                    code = split?.findText("mode"),
                    memberIds = memberIdsOf(shares, skipZeroWeight = true),
                    memberValues = memberValuesOf(shares, "weight"),
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

    private fun memberValuesOf(
        array: JsonArray?,
        key: String,
    ): List<ActivityMemberValue> = array
        .orEmpty()
        .mapNotNull { it as? JsonObject }
        .mapNotNull { entry ->
            val memberId = entry.findText("member_id") ?: return@mapNotNull null
            val value = entry.findLong(key) ?: return@mapNotNull null
            ActivityMemberValue(
                memberId = memberId,
                value = value,
            )
        }

    private fun simplifyCodeOf(value: String?): String? = when (value) {
        "true" -> ActivityEvent.SIMPLIFY_ON
        "false" -> ActivityEvent.SIMPLIFY_OFF
        else -> null
    }

    private fun JsonObject.findText(key: String): String? = (this[key] as? JsonPrimitive).textOrNull()

    private fun JsonObject.findLong(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull

    private fun JsonPrimitive?.textOrNull(): String? = this?.takeUnless { it is JsonNull }?.contentOrNull

    private companion object {
        const val EQUAL = "equal"
    }
}
