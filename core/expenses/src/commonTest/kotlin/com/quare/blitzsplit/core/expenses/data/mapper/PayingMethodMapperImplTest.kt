package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillPayingMethodDto
import com.quare.blitzsplit.core.expenses.data.dto.PayingMethodDto
import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.core.provider.room.entity.BillPayingMethodEntity
import com.quare.blitzsplit.core.provider.room.entity.PayingMethodEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PayingMethodMapperImplTest {
    private val mapper = PayingMethodMapperImpl()

    @Test
    fun `GIVEN a stored credit card WHEN mapping to the domain THEN reads every field`() {
        // Given
        val entities = listOf(
            entity(
                id = "card",
                name = "Nubank",
                icon = "blue",
                addedAt = 3L,
            ),
        )

        // When
        val methods = mapper.toDomain(entities)

        // Then
        assertEquals(
            listOf(
                PayingMethod(
                    id = "card",
                    type = PayingMethodType.CREDIT_CARD,
                    name = "Nubank",
                    cardColor = CardColor.BLUE,
                    isDefault = false,
                    addedAt = 3L,
                ),
            ),
            methods,
        )
    }

    @Test
    fun `GIVEN a deleted method WHEN mapping to the domain THEN leaves it out`() {
        // Given
        val entities = listOf(
            entity(id = "card"),
            entity(
                id = "gone",
                isDeleted = true,
            ),
        )

        // When
        val methods = mapper.toDomain(entities)

        // Then
        assertEquals(listOf("card"), methods.map { it.id })
    }

    @Test
    fun `GIVEN a type this version does not know WHEN mapping to the domain THEN leaves it out`() {
        // Given
        val entities = listOf(
            entity(id = "card"),
            entity(
                id = "voucher",
                type = "meal_voucher",
            ),
        )

        // When
        val methods = mapper.toDomain(entities)

        // Then
        assertEquals(listOf("card"), methods.map { it.id })
    }

    @Test
    fun `GIVEN two methods flagged default WHEN mapping to the domain THEN only the latest updated is default`() {
        // Given
        val entities = listOf(
            entity(
                id = "older",
                isDefault = true,
                updatedAt = 5L,
            ),
            entity(
                id = "newer",
                isDefault = true,
                updatedAt = 9L,
            ),
        )

        // When
        val methods = mapper.toDomain(entities)

        // Then
        assertEquals(listOf("newer"), methods.filter { it.isDefault }.map { it.id })
    }

    @Test
    fun `GIVEN a deleted method flagged default WHEN mapping to the domain THEN the live flagged one is default`() {
        // Given
        val entities = listOf(
            entity(
                id = "live",
                isDefault = true,
                updatedAt = 5L,
            ),
            entity(
                id = "gone",
                isDefault = true,
                isDeleted = true,
                updatedAt = 9L,
            ),
        )

        // When
        val methods = mapper.toDomain(entities)

        // Then
        assertEquals(listOf(true), methods.map { it.isDefault })
    }

    @Test
    fun `GIVEN no method flagged default WHEN mapping to the domain THEN none is the default`() {
        // Given
        val entities = listOf(
            entity(id = "card"),
            entity(
                id = "pix",
                type = "pix",
            ),
        )

        // When
        val methods = mapper.toDomain(entities)

        // Then
        assertTrue(methods.none { it.isDefault })
    }

    @Test
    fun `GIVEN methods added at different times WHEN mapping to the domain THEN the default first then by addition`() {
        // Given
        val entities = listOf(
            entity(
                id = "third",
                addedAt = 30L,
            ),
            entity(
                id = "default",
                addedAt = 20L,
                isDefault = true,
            ),
            entity(
                id = "first",
                addedAt = 10L,
            ),
            entity(
                id = "fourth",
                addedAt = 40L,
            ),
        )

        // When
        val methods = mapper.toDomain(entities)

        // Then
        assertEquals(listOf("default", "first", "third", "fourth"), methods.map { it.id })
    }

    @Test
    fun `GIVEN a pix stored with a color code WHEN mapping to the domain THEN it has no color`() {
        // Given
        val entities = listOf(
            entity(
                id = "pix",
                type = "pix",
                icon = "blue",
            ),
        )

        // When
        val methods = mapper.toDomain(entities)

        // Then
        assertNull(methods.single().cardColor)
    }

    @Test
    fun `GIVEN a debit card with a color this version does not know WHEN mapping to the domain THEN it has no color`() {
        // Given
        val entities = listOf(
            entity(
                id = "debit",
                type = "debit_card",
                icon = "teal",
            ),
        )

        // When
        val methods = mapper.toDomain(entities)

        // Then
        assertEquals(PayingMethodType.DEBIT_CARD, methods.single().type)
        assertNull(methods.single().cardColor)
    }

    @Test
    fun `GIVEN a card named with spaces around WHEN mapping to an entity THEN stores it trimmed and pending`() {
        // Given
        val method = PayingMethod(
            id = "card",
            type = PayingMethodType.CREDIT_CARD,
            name = "  Nubank  ",
            cardColor = CardColor.GOLD,
            isDefault = true,
            addedAt = 3L,
        )

        // When
        val entity = mapper.toEntity(
            method,
            updatedAt = 42L,
            ownerUserId = USER,
        )

        // Then
        assertEquals(
            PayingMethodEntity(
                id = "card",
                type = "credit_card",
                name = "Nubank",
                icon = "gold",
                isDefault = true,
                isDeleted = false,
                addedAt = 3L,
                updatedAt = 42L,
                pendingSync = true,
                ownerUserId = USER,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a pix with a card color WHEN mapping to an entity THEN stores no color`() {
        // Given
        val method = PayingMethod(
            id = "pix",
            type = PayingMethodType.PIX,
            name = "",
            cardColor = CardColor.GOLD,
            isDefault = false,
            addedAt = 3L,
        )

        // When
        val entity = mapper.toEntity(
            method,
            updatedAt = 42L,
            ownerUserId = null,
        )

        // Then
        assertEquals("", entity.icon)
        assertNull(entity.ownerUserId)
    }

    @Test
    fun `GIVEN a method from the server WHEN mapping to an entity THEN stores it synced and owned by the user`() {
        // Given
        val dto = dto(isDeleted = true)

        // When
        val entity = mapper.toEntity(dto, ownerUserId = USER)

        // Then
        assertEquals(
            PayingMethodEntity(
                id = "card",
                type = "credit_card",
                name = "Nubank",
                icon = "purple",
                isDefault = true,
                isDeleted = true,
                addedAt = 3L,
                updatedAt = 9L,
                pendingSync = false,
                ownerUserId = USER,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a stored method WHEN mapping to the server THEN carries the user and every field`() {
        // Given
        val entity = entity(
            id = "card",
            isDefault = true,
            addedAt = 3L,
            updatedAt = 9L,
        ).copy(
            pendingSync = true,
            ownerUserId = null,
        )

        // When
        val dto = mapper.toDto(entity, userId = USER)

        // Then
        assertEquals(dto(isDeleted = false), dto)
    }

    @Test
    fun `GIVEN a cleared mark from the server WHEN mapping to an entity THEN stores it synced with no method`() {
        // Given
        val dto = BillPayingMethodDto(
            userId = USER,
            expenseId = "dinner",
            methodId = null,
            updatedAt = 9L,
        )

        // When
        val entity = mapper.toEntity(dto, ownerUserId = USER)

        // Then
        assertEquals(
            BillPayingMethodEntity(
                expenseId = "dinner",
                methodId = null,
                updatedAt = 9L,
                pendingSync = false,
                ownerUserId = USER,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a stored mark WHEN mapping to the server THEN carries the user bill method and version`() {
        // Given
        val entity = BillPayingMethodEntity(
            expenseId = "dinner",
            methodId = "card",
            updatedAt = 9L,
            pendingSync = true,
            ownerUserId = null,
        )

        // When
        val dto = mapper.toDto(entity, userId = USER)

        // Then
        assertEquals(
            BillPayingMethodDto(
                userId = USER,
                expenseId = "dinner",
                methodId = "card",
                updatedAt = 9L,
            ),
            dto,
        )
    }

    private fun entity(
        id: String,
        type: String = "credit_card",
        name: String = "Nubank",
        icon: String = "purple",
        isDefault: Boolean = false,
        isDeleted: Boolean = false,
        addedAt: Long = 1L,
        updatedAt: Long = 1L,
    ): PayingMethodEntity = PayingMethodEntity(
        id = id,
        type = type,
        name = name,
        icon = icon,
        isDefault = isDefault,
        isDeleted = isDeleted,
        addedAt = addedAt,
        updatedAt = updatedAt,
        pendingSync = false,
        ownerUserId = USER,
    )

    private fun dto(isDeleted: Boolean): PayingMethodDto = PayingMethodDto(
        id = "card",
        userId = USER,
        type = "credit_card",
        name = "Nubank",
        icon = "purple",
        isDefault = true,
        isDeleted = isDeleted,
        addedAt = 3L,
        updatedAt = 9L,
    )

    private companion object {
        const val USER = "user-1"
    }
}
