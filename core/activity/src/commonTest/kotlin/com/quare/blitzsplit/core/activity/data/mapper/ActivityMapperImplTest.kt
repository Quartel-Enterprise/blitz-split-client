package com.quare.blitzsplit.core.activity.data.mapper

import com.quare.blitzsplit.core.activity.data.dto.ActivityEventDto
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActivityMapperImplTest {
    private val mapper = ActivityMapperImpl()

    @Test
    fun `GIVEN a bill created on the server WHEN storing and reading it THEN keeps its snapshot`() {
        // Given
        val dto = dto(
            kind = "bill_created",
            data = """
                {"actor_name":"Ana","group_name":"Apê 302","description":"Mercado","amount_minor":31200,
                "split_mode":"equal"}
            """,
        )

        // When
        val event = mapper.toDomain(mapper.toEntity(dto))

        // Then
        assertEquals(
            ActivityEvent(
                id = EVENT_ID,
                seq = 42L,
                groupId = GROUP_ID,
                kind = ActivityKind.BILL_CREATED,
                actorUserId = "ana-user",
                actorMemberId = "ana",
                actorName = "Ana",
                groupName = "Apê 302",
                targetId = "bill-1",
                occurredAt = 1_000L,
                subject = "Mercado",
                amountMinor = 31_200L,
                code = "equal",
                fromMemberId = null,
                toMemberId = null,
                changes = emptyList(),
                isPending = false,
            ),
            event,
        )
    }

    @Test
    fun `GIVEN an event from the server WHEN storing it THEN it is synced with the server data`() {
        // Given
        val dto = dto(
            kind = "member_joined",
            data = """{"actor_name":"Carla","name":"Carla"}""",
        )

        // When
        val entity = mapper.toEntity(dto)

        // Then
        assertEquals(
            ActivityEventEntity(
                id = EVENT_ID,
                seq = 42L,
                groupId = GROUP_ID,
                kind = "member_joined",
                actorUserId = "ana-user",
                actorMemberId = "ana",
                targetId = "bill-1",
                occurredAt = 1_000L,
                data = """{"actor_name":"Carla","name":"Carla"}""",
                pendingSync = false,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a payment recorded WHEN reading it THEN keeps who paid whom and the method`() {
        // Given
        val entity = entity(
            kind = "payment_recorded",
            data = """{"amount_minor":5000,"method":"pix","from_member_id":"ana","to_member_id":"bruno"}""",
        )

        // When
        val event = mapper.toDomain(entity)

        // Then
        assertEquals(ActivityKind.PAYMENT_RECORDED, event?.kind)
        assertEquals(5_000L, event?.amountMinor)
        assertEquals("pix", event?.code)
        assertEquals("ana", event?.fromMemberId)
        assertEquals("bruno", event?.toMemberId)
    }

    @Test
    fun `GIVEN a group created WHEN reading it THEN its name is the subject and its type the code`() {
        // Given
        val entity = entity(
            kind = "group_created",
            data = """{"name":"Viagem Floripa","type":"trip"}""",
        )

        // When
        val event = mapper.toDomain(entity)

        // Then
        assertEquals("Viagem Floripa", event?.subject)
        assertEquals("trip", event?.code)
    }

    @Test
    fun `GIVEN a kind this version does not know WHEN reading it THEN it stays out of the feed`() {
        // Given
        val entity = entity(kind = "comment_added")

        // When
        val event = mapper.toDomain(entity)

        // Then
        assertNull(event)
    }

    @Test
    fun `GIVEN data that is not an object WHEN reading the event THEN reads it with no snapshot`() {
        // Given
        val entity = entity(
            kind = "bill_deleted",
            data = "not json",
        )

        // When
        val event = mapper.toDomain(entity)

        // Then
        assertEquals(ActivityKind.BILL_DELETED, event?.kind)
        assertNull(event?.actorName)
        assertNull(event?.subject)
        assertEquals(emptyList(), event?.changes)
    }

    @Test
    fun `GIVEN a bill edited WHEN reading its changes THEN reads each field before and after`() {
        // Given
        val entity = entity(
            kind = "bill_edited",
            data = """
                {"changes":[
                  {"field":"amount","before":61200,"after":64200},
                  {"field":"description","before":"Mercado","after":"Mercado e feira"},
                  {"field":"spent_at","before":1000,"after":2000},
                  {"field":"payers","before":[{"member_id":"ana","amount_minor":61200}],
                   "after":[{"member_id":"ana","amount_minor":32100},{"member_id":"bruno","amount_minor":32100}]},
                  {"field":"split","before":{"mode":"equal","shares":[{"member_id":"ana","weight":1},
                   {"member_id":"bruno","weight":0}]},"after":{"mode":"shares","shares":[{"member_id":"ana","weight":3},
                   {"member_id":"bruno","weight":1}]}}
                ]}
            """,
        )

        // When
        val changes = mapper.toDomain(entity)?.changes

        // Then
        assertEquals(
            listOf(
                ActivityChange(
                    field = ActivityField.AMOUNT,
                    before = ActivityValue.none.copy(minor = 61_200L),
                    after = ActivityValue.none.copy(minor = 64_200L),
                ),
                ActivityChange(
                    field = ActivityField.DESCRIPTION,
                    before = ActivityValue.none.copy(text = "Mercado"),
                    after = ActivityValue.none.copy(text = "Mercado e feira"),
                ),
                ActivityChange(
                    field = ActivityField.SPENT_AT,
                    before = ActivityValue.none.copy(millis = 1_000L),
                    after = ActivityValue.none.copy(millis = 2_000L),
                ),
                ActivityChange(
                    field = ActivityField.PAYERS,
                    before = ActivityValue.none.copy(memberIds = listOf("ana")),
                    after = ActivityValue.none.copy(memberIds = listOf("ana", "bruno")),
                ),
                ActivityChange(
                    field = ActivityField.SPLIT,
                    before = ActivityValue.none.copy(
                        code = "equal",
                        memberIds = listOf("ana"),
                    ),
                    after = ActivityValue.none.copy(
                        code = "shares",
                        memberIds = listOf("ana", "bruno"),
                    ),
                ),
            ),
            changes,
        )
    }

    @Test
    fun `GIVEN a group edited WHEN reading its changes THEN reads the name type color currency and default split`() {
        // Given
        val entity = entity(
            kind = "group_edited",
            data = """
                {"changes":[
                  {"field":"name","before":"Apê","after":"Apê 302"},
                  {"field":"type","before":"home","after":"trip"},
                  {"field":"color","before":"blue","after":"green"},
                  {"field":"currency","before":"BRL","after":"USD"},
                  {"field":"default_split","before":null,"after":{"mode":"percent","weights":[]}}
                ]}
            """,
        )

        // When
        val changes = mapper.toDomain(entity)?.changes

        // Then
        assertEquals(
            listOf(
                ActivityChange(
                    field = ActivityField.NAME,
                    before = ActivityValue.none.copy(text = "Apê"),
                    after = ActivityValue.none.copy(text = "Apê 302"),
                ),
                ActivityChange(
                    field = ActivityField.TYPE,
                    before = ActivityValue.none.copy(code = "home"),
                    after = ActivityValue.none.copy(code = "trip"),
                ),
                ActivityChange(
                    field = ActivityField.COLOR,
                    before = ActivityValue.none.copy(code = "blue"),
                    after = ActivityValue.none.copy(code = "green"),
                ),
                ActivityChange(
                    field = ActivityField.CURRENCY,
                    before = ActivityValue.none.copy(code = "BRL"),
                    after = ActivityValue.none.copy(code = "USD"),
                ),
                ActivityChange(
                    field = ActivityField.DEFAULT_SPLIT,
                    before = ActivityValue.none.copy(code = "equal"),
                    after = ActivityValue.none.copy(code = "percent"),
                ),
            ),
            changes,
        )
    }

    @Test
    fun `GIVEN a change of a field unknown to this version WHEN reading the edit THEN leaves that change out`() {
        // Given
        val entity = entity(
            kind = "bill_edited",
            data = """
                {"changes":[
                  {"field":"category","before":"food","after":"home"},
                  {"field":"amount","before":100,"after":200}
                ]}
            """,
        )

        // When
        val changes = mapper.toDomain(entity)?.changes

        // Then
        assertEquals(listOf(ActivityField.AMOUNT), changes?.map { it.field })
    }

    @Test
    fun `GIVEN a description cleared WHEN reading the edit THEN its after has no text`() {
        // Given
        val entity = entity(
            kind = "bill_edited",
            data = """{"changes":[{"field":"description","before":"Mercado","after":null}]}""",
        )

        // When
        val change = mapper.toDomain(entity)?.changes?.single()

        // Then
        assertEquals(ActivityValue.none, change?.after)
    }

    @Test
    fun `GIVEN a charge sent with no account WHEN storing it THEN is a pending charge with no seq nor actor`() {
        // Given
        val charge = ChargeSent(
            groupId = GROUP_ID,
            memberId = "carla",
            memberName = "Carla",
            amountMinor = 3_290L,
        )

        // When
        val entity = mapper.toEntity(
            charge = charge,
            id = EVENT_ID,
            actorUserId = null,
            sentAt = 9_000L,
        )

        // Then
        assertEquals(
            ActivityEventEntity(
                id = EVENT_ID,
                seq = null,
                groupId = GROUP_ID,
                kind = "charge_sent",
                actorUserId = null,
                actorMemberId = null,
                targetId = "carla",
                occurredAt = 9_000L,
                data = """{"name":"Carla","amount_minor":3290}""",
                pendingSync = true,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a charge stored here WHEN reading it THEN it is pending with whom and how much`() {
        // Given
        val entity = mapper.toEntity(
            charge = ChargeSent(
                groupId = GROUP_ID,
                memberId = "carla",
                memberName = "Carla",
                amountMinor = 3_290L,
            ),
            id = EVENT_ID,
            actorUserId = "me-user",
            sentAt = 9_000L,
        )

        // When
        val event = mapper.toDomain(entity)

        // Then
        assertEquals(ActivityKind.CHARGE_SENT, event?.kind)
        assertEquals("Carla", event?.subject)
        assertEquals(3_290L, event?.amountMinor)
        assertEquals("carla", event?.targetId)
        assertNull(event?.seq)
        assertEquals(true, event?.isPending)
    }

    @Test
    fun `GIVEN a charge sent before any account WHEN sending it up THEN the actor is the syncing user`() {
        // Given
        val entity = entity(
            kind = "charge_sent",
            data = """{"name":"Carla","amount_minor":3290}""",
        ).copy(
            seq = null,
            actorUserId = null,
            targetId = "carla",
            pendingSync = true,
        )

        // When
        val dto = mapper.toChargeDto(entity, userId = "me-user")

        // Then
        assertEquals(EVENT_ID, dto.id)
        assertEquals(GROUP_ID, dto.groupId)
        assertEquals("charge_sent", dto.kind)
        assertEquals("me-user", dto.actorUserId)
        assertEquals("carla", dto.targetId)
        assertEquals(1_000L, dto.occurredAt)
        assertEquals(json("""{"name":"Carla","amount_minor":3290}"""), dto.data)
    }

    @Test
    fun `GIVEN a pending bill the server never recorded WHEN reading it queued THEN it is a bill created`() {
        // Given
        val row = billRow()

        // When
        val queued = mapper.toQueued(row)

        // Then
        assertEquals(
            QueuedChange(
                id = "bill-1",
                groupId = GROUP_ID,
                kind = ActivityKind.BILL_CREATED,
                subject = "Mercado",
                amountMinor = 31_200L,
                code = "equal",
                fromMemberId = null,
                toMemberId = null,
                savedAt = 7_000L,
            ),
            queued,
        )
    }

    @Test
    fun `GIVEN a pending bill the server already recorded WHEN reading it queued THEN it is a bill edited`() {
        // Given
        val row = billRow(hasEvent = true)

        // When
        val queued = mapper.toQueued(row)

        // Then
        assertEquals(ActivityKind.BILL_EDITED, queued.kind)
    }

    @Test
    fun `GIVEN a pending bill deleted WHEN reading it queued THEN it is a bill deleted`() {
        // Given
        val row = billRow(
            isDeleted = true,
            hasEvent = true,
        )

        // When
        val queued = mapper.toQueued(row)

        // Then
        assertEquals(ActivityKind.BILL_DELETED, queued.kind)
    }

    @Test
    fun `GIVEN a pending payment WHEN reading it queued THEN it is a payment recorded`() {
        // Given
        val row = paymentRow(isDeleted = false)

        // When
        val queued = mapper.toQueued(row)

        // Then
        assertEquals(
            QueuedChange(
                id = "payment-1",
                groupId = GROUP_ID,
                kind = ActivityKind.PAYMENT_RECORDED,
                subject = null,
                amountMinor = 5_000L,
                code = "cash",
                fromMemberId = "ana",
                toMemberId = "bruno",
                savedAt = 8_000L,
            ),
            queued,
        )
    }

    @Test
    fun `GIVEN a pending payment undone WHEN reading it queued THEN it is a payment undone`() {
        // Given
        val row = paymentRow(isDeleted = true)

        // When
        val queued = mapper.toQueued(row)

        // Then
        assertEquals(ActivityKind.PAYMENT_UNDONE, queued.kind)
    }

    @Test
    fun `GIVEN a new group not up yet WHEN reading it queued THEN it is a group created in itself`() {
        // Given
        val row = groupRow(isNew = true)

        // When
        val queued = mapper.toQueued(row)

        // Then
        assertEquals(
            QueuedChange(
                id = GROUP_ID,
                groupId = GROUP_ID,
                kind = ActivityKind.GROUP_CREATED,
                subject = "Apê 302",
                amountMinor = null,
                code = "home",
                fromMemberId = null,
                toMemberId = null,
                savedAt = 6_000L,
            ),
            queued,
        )
    }

    @Test
    fun `GIVEN a group already on the server edited here WHEN reading it queued THEN it is a group edited`() {
        // Given
        val row = groupRow(isNew = false)

        // When
        val queued = mapper.toQueued(row)

        // Then
        assertEquals(ActivityKind.GROUP_EDITED, queued.kind)
    }

    private fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private fun dto(
        kind: String,
        data: String,
    ): ActivityEventDto = ActivityEventDto(
        id = EVENT_ID,
        seq = 42L,
        groupId = GROUP_ID,
        kind = kind,
        actorUserId = "ana-user",
        actorMemberId = "ana",
        targetId = "bill-1",
        occurredAt = 1_000L,
        data = json(data),
    )

    private fun entity(
        kind: String,
        data: String = "{}",
    ): ActivityEventEntity = ActivityEventEntity(
        id = EVENT_ID,
        seq = 42L,
        groupId = GROUP_ID,
        kind = kind,
        actorUserId = "ana-user",
        actorMemberId = "ana",
        targetId = "target-1",
        occurredAt = 1_000L,
        data = data.trimIndent(),
        pendingSync = false,
    )

    private fun billRow(
        isDeleted: Boolean = false,
        hasEvent: Boolean = false,
    ): QueuedBillRow = QueuedBillRow(
        id = "bill-1",
        groupId = GROUP_ID,
        description = "Mercado",
        amountMinor = 31_200L,
        splitMode = "equal",
        isDeleted = isDeleted,
        updatedAt = 7_000L,
        hasEvent = hasEvent,
    )

    private fun paymentRow(isDeleted: Boolean): QueuedPaymentRow = QueuedPaymentRow(
        id = "payment-1",
        groupId = GROUP_ID,
        fromMemberId = "ana",
        toMemberId = "bruno",
        amountMinor = 5_000L,
        method = "cash",
        isDeleted = isDeleted,
        updatedAt = 8_000L,
    )

    private fun groupRow(isNew: Boolean): QueuedGroupRow = QueuedGroupRow(
        id = GROUP_ID,
        name = "Apê 302",
        type = "home",
        isNew = isNew,
        updatedAt = 6_000L,
    )

    private companion object {
        const val EVENT_ID = "event-1"
        const val GROUP_ID = "group-1"
    }
}
