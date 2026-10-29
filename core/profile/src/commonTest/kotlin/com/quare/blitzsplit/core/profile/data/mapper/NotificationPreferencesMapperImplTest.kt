package com.quare.blitzsplit.core.profile.data.mapper

import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.profile.data.dto.NotificationPreferencesDto
import com.quare.blitzsplit.core.provider.room.entity.NotificationPreferencesEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationPreferencesMapperImplTest {
    private val mapper = NotificationPreferencesMapperImpl()

    @Test
    fun `GIVEN no stored preferences WHEN mapping to the domain THEN gives the defaults`() {
        // Given
        val stored: NotificationPreferencesEntity? = null

        // When
        val preferences = mapper.toDomain(stored)

        // Then
        assertEquals(NotificationPreferences.DEFAULT, preferences)
    }

    @Test
    fun `GIVEN new expenses and settle reminders on WHEN mapping to the domain THEN only those types are enabled`() {
        // Given
        val stored = entity(
            newExpense = true,
            paymentMarked = false,
            settleReminders = true,
            invites = false,
        )

        // When
        val preferences = mapper.toDomain(stored)

        // Then
        assertEquals(setOf(NotificationType.NEW_EXPENSE, NotificationType.SETTLE_REMINDERS), preferences.enabledTypes)
    }

    @Test
    fun `GIVEN marked payments and invites on WHEN mapping to the domain THEN only those types are enabled`() {
        // Given
        val stored = entity(
            newExpense = false,
            paymentMarked = true,
            settleReminders = false,
            invites = true,
        )

        // When
        val preferences = mapper.toDomain(stored)

        // Then
        assertEquals(setOf(NotificationType.PAYMENT_MARKED, NotificationType.INVITES), preferences.enabledTypes)
    }

    @Test
    fun `GIVEN every toggle off WHEN mapping to the domain THEN no type is enabled rather than the defaults`() {
        // Given
        val stored = entity(
            newExpense = false,
            paymentMarked = false,
            settleReminders = false,
            invites = false,
        )

        // When
        val preferences = mapper.toDomain(stored)

        // Then
        assertTrue(preferences.enabledTypes.isEmpty())
    }

    @Test
    fun `GIVEN chosen preferences WHEN mapping to the entity THEN stores each flag pending for the owner`() {
        // Given
        val preferences = NotificationPreferences(
            enabledTypes = setOf(NotificationType.PAYMENT_MARKED, NotificationType.INVITES),
        )

        // When
        val stored = mapper.toEntity(
            preferences = preferences,
            updatedAt = 1_000L,
            ownerUserId = "user-1",
        )

        // Then
        assertEquals(
            entity(
                newExpense = false,
                paymentMarked = true,
                settleReminders = false,
                invites = true,
                updatedAt = 1_000L,
                pendingSync = true,
                ownerUserId = "user-1",
            ),
            stored,
        )
    }

    @Test
    fun `GIVEN preferences changed before any account WHEN mapping to the entity THEN stores them without an owner`() {
        // Given
        val preferences = NotificationPreferences.DEFAULT

        // When
        val stored = mapper.toEntity(
            preferences = preferences,
            updatedAt = 1_000L,
            ownerUserId = null,
        )

        // Then
        assertEquals(
            entity(
                newExpense = true,
                paymentMarked = true,
                settleReminders = true,
                invites = false,
                updatedAt = 1_000L,
                pendingSync = true,
                ownerUserId = null,
            ),
            stored,
        )
    }

    @Test
    fun `GIVEN each type alone WHEN mapping to the entity and back THEN the same type comes back`() {
        // Given
        val singles = NotificationType.entries.map { NotificationPreferences(setOf(it)) }

        // When
        val roundTrips = singles.map { preferences ->
            mapper.toDomain(
                mapper.toEntity(
                    preferences = preferences,
                    updatedAt = 1L,
                    ownerUserId = "user-1",
                ),
            )
        }

        // Then
        assertEquals(singles, roundTrips)
    }

    @Test
    fun `GIVEN stored preferences WHEN mapping to the server row THEN sends each flag for the given user`() {
        // Given
        val stored = entity(
            newExpense = false,
            paymentMarked = true,
            settleReminders = false,
            invites = true,
            updatedAt = 42L,
            pendingSync = true,
            ownerUserId = null,
        )

        // When
        val dto = mapper.toDto(
            entity = stored,
            userId = "user-1",
        )

        // Then
        assertEquals(
            NotificationPreferencesDto(
                userId = "user-1",
                newExpense = false,
                paymentMarked = true,
                settleReminders = false,
                invites = true,
                updatedAt = 42L,
            ),
            dto,
        )
    }

    @Test
    fun `GIVEN the other flags on WHEN mapping to the server row THEN sends each flag as stored`() {
        // Given
        val stored = entity(
            newExpense = true,
            paymentMarked = false,
            settleReminders = true,
            invites = false,
        )

        // When
        val dto = mapper.toDto(
            entity = stored,
            userId = "user-1",
        )

        // Then
        assertEquals(
            listOf(true, false, true, false),
            listOf(dto.newExpense, dto.paymentMarked, dto.settleReminders, dto.invites),
        )
    }

    @Test
    fun `GIVEN a server row WHEN mapping to the entity THEN stores each flag synced for the given user`() {
        // Given
        val dto = NotificationPreferencesDto(
            userId = "user-1",
            newExpense = false,
            paymentMarked = true,
            settleReminders = false,
            invites = true,
            updatedAt = 100L,
        )

        // When
        val stored = mapper.toEntity(
            dto = dto,
            ownerUserId = "user-1",
        )

        // Then
        assertEquals(
            entity(
                newExpense = false,
                paymentMarked = true,
                settleReminders = false,
                invites = true,
                updatedAt = 100L,
                pendingSync = false,
                ownerUserId = "user-1",
            ),
            stored,
        )
    }

    @Test
    fun `GIVEN a server row WHEN mapping to the entity and back THEN nothing changes`() {
        // Given
        val dto = NotificationPreferencesDto(
            userId = "user-1",
            newExpense = true,
            paymentMarked = false,
            settleReminders = true,
            invites = false,
            updatedAt = 100L,
        )

        // When
        val roundTrip = mapper.toDto(
            entity = mapper.toEntity(
                dto = dto,
                ownerUserId = "user-1",
            ),
            userId = "user-1",
        )

        // Then
        assertEquals(dto, roundTrip)
    }

    private fun entity(
        newExpense: Boolean,
        paymentMarked: Boolean,
        settleReminders: Boolean,
        invites: Boolean,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        ownerUserId: String? = "user-1",
    ): NotificationPreferencesEntity = NotificationPreferencesEntity(
        id = SINGLE_ROW_ID,
        newExpense = newExpense,
        paymentMarked = paymentMarked,
        settleReminders = settleReminders,
        invites = invites,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        ownerUserId = ownerUserId,
    )
}
