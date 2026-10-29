package com.quare.blitzsplit.core.profile.data.mapper

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.data.dto.PaymentKeyDto
import com.quare.blitzsplit.core.provider.room.entity.PaymentKeyEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaymentKeyMapperImplTest {
    private val mapper = PaymentKeyMapperImpl()

    @Test
    fun `GIVEN a deleted key WHEN mapping to the domain THEN leaves it out`() {
        // Given
        val stored = listOf(
            entity(
                id = "pix-1",
                isDefault = true,
            ),
            entity(
                id = "pix-2",
                isDeleted = true,
                addedAt = 2L,
            ),
        )

        // When
        val keys = mapper.toDomain(stored)

        // Then
        assertEquals(listOf("pix-1"), keys.map { it.id })
    }

    @Test
    fun `GIVEN a default the rules reject WHEN mapping to the domain THEN the next key is the default`() {
        // Given
        val stored = listOf(
            entity(
                id = "pix-1",
                value = "12345678900",
                isDefault = true,
            ),
            entity(
                id = "pix-2",
                addedAt = 2L,
            ),
        )

        // When
        val keys = mapper.toDomain(stored)

        // Then
        assertEquals(listOf("pix-2" to true), keys.map { it.id to it.isDefault })
    }

    @Test
    fun `GIVEN a type this version does not know WHEN mapping to the domain THEN leaves it out`() {
        // Given
        val stored = listOf(
            entity(
                id = "pix-1",
                isDefault = true,
            ),
            entity(
                id = "boleto-1",
                type = "boleto",
                addedAt = 2L,
            ),
        )

        // When
        val keys = mapper.toDomain(stored)

        // Then
        assertEquals(listOf("pix-1"), keys.map { it.id })
    }

    @Test
    fun `GIVEN every type code WHEN mapping to the domain THEN reads each as its type`() {
        // Given
        val stored = listOf(
            entity(
                id = "cpf",
                type = "cpf",
                addedAt = 1L,
            ),
            entity(
                id = "phone",
                type = "phone",
                addedAt = 2L,
            ),
            entity(
                id = "email",
                type = "email",
                addedAt = 3L,
            ),
            entity(
                id = "random",
                type = "random",
                addedAt = 4L,
            ),
            entity(
                id = "lightning",
                type = "lightning",
                addedAt = 5L,
            ),
            entity(
                id = "onchain",
                type = "onchain",
                addedAt = 6L,
            ),
        )

        // When
        val keys = mapper.toDomain(stored)

        // Then
        assertEquals(
            listOf(
                PaymentKeyType.CPF,
                PaymentKeyType.PHONE,
                PaymentKeyType.EMAIL,
                PaymentKeyType.RANDOM,
                PaymentKeyType.LIGHTNING,
                PaymentKeyType.ONCHAIN,
            ),
            keys.map { it.type },
        )
    }

    @Test
    fun `GIVEN two Pix keys flagged default on two devices WHEN mapping to the domain THEN the newest flag wins`() {
        // Given
        val stored = listOf(
            entity(
                id = "pix-1",
                isDefault = true,
                addedAt = 1L,
                updatedAt = 50L,
            ),
            entity(
                id = "pix-2",
                isDefault = true,
                addedAt = 2L,
                updatedAt = 90L,
            ),
        )

        // When
        val keys = mapper.toDomain(stored)

        // Then
        assertEquals(listOf("pix-2" to true, "pix-1" to false), keys.map { it.id to it.isDefault })
    }

    @Test
    fun `GIVEN no Pix key flagged default WHEN mapping to the domain THEN the oldest is the default`() {
        // Given
        val stored = listOf(
            entity(
                id = "pix-new",
                addedAt = 30L,
            ),
            entity(
                id = "pix-old",
                addedAt = 10L,
            ),
        )

        // When
        val keys = mapper.toDomain(stored)

        // Then
        assertEquals(listOf("pix-old" to true, "pix-new" to false), keys.map { it.id to it.isDefault })
    }

    @Test
    fun `GIVEN the default was deleted WHEN mapping to the domain THEN the oldest live key is the default`() {
        // Given
        val stored = listOf(
            entity(
                id = "pix-deleted",
                isDefault = true,
                isDeleted = true,
                addedAt = 1L,
                updatedAt = 99L,
            ),
            entity(
                id = "pix-new",
                addedAt = 30L,
            ),
            entity(
                id = "pix-old",
                addedAt = 10L,
            ),
        )

        // When
        val keys = mapper.toDomain(stored)

        // Then
        assertEquals(listOf("pix-old" to true, "pix-new" to false), keys.map { it.id to it.isDefault })
    }

    @Test
    fun `GIVEN Pix and Bitcoin keys mixed WHEN mapping to the domain THEN Pix first each default first then by age`() {
        // Given
        val stored = listOf(
            entity(
                id = "btc-old",
                type = "onchain",
                addedAt = 1L,
            ),
            entity(
                id = "btc-default",
                type = "lightning",
                isDefault = true,
                addedAt = 9L,
            ),
            entity(
                id = "pix-new",
                type = "email",
                addedAt = 8L,
            ),
            entity(
                id = "pix-default",
                type = "cpf",
                isDefault = true,
                addedAt = 5L,
            ),
            entity(
                id = "pix-old",
                type = "phone",
                addedAt = 2L,
            ),
            entity(
                id = "btc-new",
                type = "onchain",
                addedAt = 12L,
            ),
        )

        // When
        val keys = mapper.toDomain(stored)

        // Then
        assertEquals(
            listOf("pix-default", "pix-old", "pix-new", "btc-default", "btc-old", "btc-new"),
            keys.map { it.id },
        )
    }

    @Test
    fun `GIVEN one default per method WHEN mapping to the domain THEN each method keeps its own default`() {
        // Given
        val stored = listOf(
            entity(
                id = "pix-default",
                type = "cpf",
                isDefault = true,
                addedAt = 1L,
                updatedAt = 10L,
            ),
            entity(
                id = "btc-default",
                type = "lightning",
                isDefault = true,
                addedAt = 2L,
                updatedAt = 99L,
            ),
        )

        // When
        val keys = mapper.toDomain(stored)

        // Then
        assertTrue(keys.all { it.isDefault })
    }

    @Test
    fun `GIVEN a stored row WHEN mapping to the domain THEN keeps its id value and the time it was added`() {
        // Given
        val stored = listOf(
            entity(
                id = "pix-1",
                value = "+5511912345678",
                type = "phone",
                addedAt = 7L,
            ),
        )

        // When
        val keys = mapper.toDomain(stored)

        // Then
        assertEquals(
            listOf(
                PaymentKey(
                    "pix-1",
                    PaymentKeyType.PHONE,
                    "+5511912345678",
                    isDefault = true,
                    addedAt = 7L,
                ),
            ),
            keys,
        )
    }

    @Test
    fun `GIVEN a key WHEN mapping to a row THEN writes a pending live row with the lowercase type code`() {
        // Given
        val key = PaymentKey(
            id = "btc-1",
            type = PaymentKeyType.ONCHAIN,
            value = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
            isDefault = true,
            addedAt = 7L,
        )

        // When
        val row = mapper.toEntity(
            key,
            updatedAt = 42L,
            ownerUserId = "user-1",
        )

        // Then
        assertEquals(
            PaymentKeyEntity(
                id = "btc-1",
                type = "onchain",
                value = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
                isDefault = true,
                isDeleted = false,
                addedAt = 7L,
                updatedAt = 42L,
                pendingSync = true,
                ownerUserId = "user-1",
            ),
            row,
        )
    }

    @Test
    fun `GIVEN a key made before any account WHEN mapping to a row THEN the row has no owner`() {
        // Given
        val key = PaymentKey(
            "pix-1",
            PaymentKeyType.CPF,
            "12345678909",
            isDefault = true,
            addedAt = 7L,
        )

        // When
        val row = mapper.toEntity(
            key,
            updatedAt = 42L,
            ownerUserId = null,
        )

        // Then
        assertEquals(null, row.ownerUserId)
    }

    @Test
    fun `GIVEN a deleted row WHEN mapping to the server THEN sends the tombstone under the user id`() {
        // Given
        val stored = entity(
            id = "pix-1",
            isDeleted = true,
            addedAt = 7L,
            updatedAt = 42L,
            pendingSync = true,
        )

        // When
        val dto = mapper.toDto(stored, userId = "user-1")

        // Then
        assertEquals(
            PaymentKeyDto(
                id = "pix-1",
                userId = "user-1",
                type = "cpf",
                value = "12345678909",
                isDefault = false,
                isDeleted = true,
                addedAt = 7L,
                updatedAt = 42L,
            ),
            dto,
        )
    }

    @Test
    fun `GIVEN a key from the server WHEN mapping to a row THEN stores it synced under the owner`() {
        // Given
        val dto = PaymentKeyDto(
            id = "pix-1",
            userId = "user-1",
            type = "email",
            value = "pierre@email.com",
            isDefault = true,
            isDeleted = false,
            addedAt = 7L,
            updatedAt = 42L,
        )

        // When
        val row = mapper.toEntity(dto, ownerUserId = "user-1")

        // Then
        assertEquals(
            PaymentKeyEntity(
                id = "pix-1",
                type = "email",
                value = "pierre@email.com",
                isDefault = true,
                isDeleted = false,
                addedAt = 7L,
                updatedAt = 42L,
                pendingSync = false,
                ownerUserId = "user-1",
            ),
            row,
        )
    }

    private fun entity(
        id: String,
        type: String = "cpf",
        value: String = validValues[type] ?: "12345678909",
        isDefault: Boolean = false,
        isDeleted: Boolean = false,
        addedAt: Long = 1L,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
    ): PaymentKeyEntity = PaymentKeyEntity(
        id = id,
        type = type,
        value = value,
        isDefault = isDefault,
        isDeleted = isDeleted,
        addedAt = addedAt,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        ownerUserId = "user-1",
    )

    // Why: a value each type's rules accept, since the mapper leaves out the keys they reject.
    private val validValues = mapOf(
        "cpf" to "12345678909",
        "phone" to "+5511912345678",
        "email" to "pierre@email.com",
        "random" to "7d9f3c2a-1b4e-4c8a-9f2d-3e5a6b4d1a37",
        "lightning" to "pierre@getalby.com",
        "onchain" to "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
    )
}
