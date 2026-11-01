package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExpenseDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GIVEN items missing a field WHEN the bill is read THEN reads it without items`() {
        // Given
        val row = rowWith(items = """{"updated_at":5,"items":[],"service_percent":10}""")

        // When
        val dto = json.decodeFromString(ExpenseDto.serializer(), row)

        // Then
        assertNull(dto.items)
        assertEquals("bill", dto.id)
    }

    @Test
    fun `GIVEN items with a field of another type WHEN the bill is read THEN reads it without items`() {
        // Given
        val row = rowWith(
            items = """{"updated_at":"five","items":[],"service_percent":10,"couvert_minor":0,""" +
                """"couvert_member_ids":[],"extra_minor":0,"from_receipt":false}""",
        )

        // When
        val dto = json.decodeFromString(ExpenseDto.serializer(), row)

        // Then
        assertNull(dto.items)
    }

    @Test
    fun `GIVEN well formed items WHEN the bill is read THEN keeps them`() {
        // Given
        val row = rowWith(
            items = """{"updated_at":5,"items":[],"service_percent":10,"couvert_minor":0,""" +
                """"couvert_member_ids":[],"extra_minor":0,"from_receipt":false}""",
        )

        // When
        val dto = json.decodeFromString(ExpenseDto.serializer(), row)

        // Then
        assertEquals(10L, dto.items?.servicePercent)
    }

    private fun rowWith(items: String): String =
        """{"id":"bill","group_id":"g","created_by":null,"description":"Pizza","amount_minor":1000,""" +
            """"split_mode":"amount","payers":[],"shares":[],"items":$items,"spent_at":1,"is_deleted":false,""" +
            """"added_at":1,"updated_at":5,"updated_by":null}"""
}
