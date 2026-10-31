package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.SettlementDto
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.provider.room.entity.SettlementEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class SettlementMapperImplTest {
    private val mapper = SettlementMapperImpl()

    @Test
    fun `GIVEN a stored payment waiting to go up WHEN mapping to the domain THEN reads it pending`() {
        // Given
        val entity = entity(
            method = "transfer",
            pendingSync = true,
        )

        // When
        val settlement = mapper.toDomain(entity)

        // Then
        assertEquals(
            Settlement(
                id = SETTLEMENT_ID,
                groupId = GROUP_ID,
                fromMemberId = "ana",
                toMemberId = "bruno",
                amountMinor = 5_000L,
                method = SettlementMethod.TRANSFER,
                paidAt = 3_000L,
                isPending = true,
            ),
            settlement,
        )
    }

    @Test
    fun `GIVEN a method this version does not know WHEN mapping to the domain THEN it is other`() {
        // Given
        val entity = entity(method = "crypto")

        // When
        val settlement = mapper.toDomain(entity)

        // Then
        assertEquals(SettlementMethod.OTHER, settlement.method)
    }

    @Test
    fun `GIVEN a new payment WHEN mapping it to store THEN it is pending with no creator at the recorded time`() {
        // Given
        val settlement = Settlement(
            id = SETTLEMENT_ID,
            groupId = GROUP_ID,
            fromMemberId = "ana",
            toMemberId = "bruno",
            amountMinor = 5_000L,
            method = SettlementMethod.CASH,
            paidAt = 3_000L,
            isPending = true,
        )

        // When
        val entity = mapper.toEntity(settlement, recordedAt = 7_000L)

        // Then
        assertEquals(
            entity(
                createdBy = null,
                method = "cash",
                addedAt = 7_000L,
                updatedAt = 7_000L,
                pendingSync = true,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a payment from the server WHEN mapping it to store THEN keeps its creator and it is synced`() {
        // Given
        val dto = dto(
            createdBy = "user-2",
            isDeleted = true,
        )

        // When
        val entity = mapper.toEntity(dto)

        // Then
        assertEquals(
            entity(
                createdBy = "user-2",
                isDeleted = true,
                pendingSync = false,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a payment another member recorded WHEN mapping it for the server THEN its creator is who pushes`() {
        // Given
        val entity = entity(
            createdBy = "user-2",
            isDeleted = true,
            pendingSync = true,
        )

        // When
        val dto = mapper.toDto(entity, userId = "user-1")

        // Then
        assertEquals(
            dto(
                createdBy = "user-1",
                isDeleted = true,
            ),
            dto,
        )
    }

    @Test
    fun `GIVEN a payment recorded before any account WHEN mapping it for the server THEN its creator is who pushes`() {
        // Given
        val entity = entity(
            createdBy = null,
            pendingSync = true,
        )

        // When
        val dto = mapper.toDto(entity, userId = "user-1")

        // Then
        assertEquals("user-1", dto.createdBy)
    }

    @Test
    fun `GIVEN a payment for the server WHEN encoding it THEN uses the server snake case columns`() {
        // Given
        val dto = dto(createdBy = "user-1")

        // When
        val json = Json.encodeToJsonElement(SettlementDto.serializer(), dto).jsonObject

        // Then
        assertEquals(
            setOf(
                "id",
                "group_id",
                "created_by",
                "from_member_id",
                "to_member_id",
                "amount_minor",
                "method",
                "paid_at",
                "is_deleted",
                "added_at",
                "updated_at",
            ),
            json.keys,
        )
    }

    private fun entity(
        createdBy: String? = "user-1",
        method: String = "pix",
        isDeleted: Boolean = false,
        addedAt: Long = 2_000L,
        updatedAt: Long = 4_000L,
        pendingSync: Boolean = false,
    ): SettlementEntity = SettlementEntity(
        id = SETTLEMENT_ID,
        groupId = GROUP_ID,
        createdBy = createdBy,
        fromMemberId = "ana",
        toMemberId = "bruno",
        amountMinor = 5_000L,
        method = method,
        paidAt = 3_000L,
        isDeleted = isDeleted,
        addedAt = addedAt,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
    )

    private fun dto(
        createdBy: String?,
        isDeleted: Boolean = false,
    ): SettlementDto = SettlementDto(
        id = SETTLEMENT_ID,
        groupId = GROUP_ID,
        createdBy = createdBy,
        fromMemberId = "ana",
        toMemberId = "bruno",
        amountMinor = 5_000L,
        method = "pix",
        paidAt = 3_000L,
        isDeleted = isDeleted,
        addedAt = 2_000L,
        updatedAt = 4_000L,
    )

    private companion object {
        const val SETTLEMENT_ID = "payment-1"
        const val GROUP_ID = "group-1"
    }
}
