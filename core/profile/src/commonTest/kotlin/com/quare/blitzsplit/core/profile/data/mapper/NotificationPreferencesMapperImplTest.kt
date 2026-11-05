package com.quare.blitzsplit.core.profile.data.mapper

import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.profile.data.dto.NotificationPreferencesDto
import com.quare.blitzsplit.core.provider.room.entity.NotificationPreferencesEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        assertEquals(setOf(NotificationType.BILLS, NotificationType.BALANCE_REMINDER), preferences.push)
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
        assertEquals(setOf(NotificationType.PAYMENTS, NotificationType.PEOPLE_AND_GROUPS), preferences.push)
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
        assertTrue(preferences.push.isEmpty())
    }

    @Test
    fun `GIVEN chosen preferences WHEN mapping to the entity THEN stores each flag pending for the owner`() {
        // Given
        val preferences =
            NotificationPreferences(
                push = setOf(NotificationType.PAYMENTS, NotificationType.PEOPLE_AND_GROUPS),
                inApp = NotificationPreferences.DEFAULT.inApp,
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
                comments = true,
                paymentRequests = true,
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
        val singles = NotificationType.entries.map {
            NotificationPreferences(
                push = setOf(it),
                inApp = NotificationPreferences.DEFAULT.inApp,
            )
        }

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
            comments = false,
            paymentRequests = false,
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
                comments = false,
                paymentRequests = false,
                inAppBills = true,
                inAppPayments = true,
                inAppComments = true,
                inAppPeopleAndGroups = true,
                inAppPaymentRequests = true,
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
            comments = false,
            paymentRequests = false,
            inAppBills = true,
            inAppPayments = true,
            inAppComments = true,
            inAppPeopleAndGroups = true,
            inAppPaymentRequests = true,
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
                comments = false,
                paymentRequests = false,
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
            comments = true,
            paymentRequests = true,
            inAppBills = true,
            inAppPayments = true,
            inAppComments = true,
            inAppPeopleAndGroups = true,
            inAppPaymentRequests = true,
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

    @Test
    fun `GIVEN each in-app column alone on WHEN mapping to the domain THEN only its type is on in-app`() {
        // Given
        val rows = inAppTypes.associateWith(::inAppOnlyEntity)

        // When
        val preferences = rows.mapValues { (_, row) -> mapper.toDomain(row) }

        // Then
        assertEquals(
            inAppTypes.associateWith {
                NotificationPreferences(
                    push = emptySet(),
                    inApp = setOf(it),
                )
            },
            preferences,
        )
    }

    @Test
    fun `GIVEN each push column alone on WHEN mapping to the domain THEN only its type is on for push`() {
        // Given
        val rows = NotificationType.entries.associateWith(::pushOnlyEntity)

        // When
        val preferences = rows.mapValues { (_, row) -> mapper.toDomain(row) }

        // Then
        assertEquals(
            NotificationType.entries.associateWith {
                NotificationPreferences(
                    push = setOf(it),
                    inApp = emptySet(),
                )
            },
            preferences,
        )
    }

    @Test
    fun `GIVEN each in-app type alone WHEN mapping to the entity THEN only its column is on`() {
        // Given
        val choices = inAppTypes.associateWith {
            NotificationPreferences(
                push = emptySet(),
                inApp = setOf(it),
            )
        }

        // When
        val rows = choices.mapValues { (_, preferences) -> mapper.toEntity(preferences, 1L, "user-1") }

        // Then
        assertEquals(
            inAppTypes.associateWith { inAppOnlyEntity(it).copy(pendingSync = true) },
            rows,
        )
    }

    @Test
    fun `GIVEN each push type alone WHEN mapping to the entity THEN only its column is on`() {
        // Given
        val choices = NotificationType.entries.associateWith {
            NotificationPreferences(
                push = setOf(it),
                inApp = emptySet(),
            )
        }

        // When
        val rows = choices.mapValues { (_, preferences) -> mapper.toEntity(preferences, 1L, "user-1") }

        // Then
        assertEquals(
            NotificationType.entries.associateWith { pushOnlyEntity(it).copy(pendingSync = true) },
            rows,
        )
    }

    @Test
    fun `GIVEN each in-app column alone on WHEN mapping to the server row THEN only its server column is on`() {
        // Given
        val rows = inAppTypes.associateWith(::inAppOnlyEntity)

        // When
        val dtos = rows.mapValues { (_, row) -> mapper.toDto(row, "user-1") }

        // Then
        assertEquals(inAppTypes.associateWith(::inAppOnlyDto), dtos)
    }

    @Test
    fun `GIVEN each push column alone on WHEN mapping to the server row THEN only its server column is on`() {
        // Given
        val rows = NotificationType.entries.associateWith(::pushOnlyEntity)

        // When
        val dtos = rows.mapValues { (_, row) -> mapper.toDto(row, "user-1") }

        // Then
        assertEquals(NotificationType.entries.associateWith(::pushOnlyDto), dtos)
    }

    @Test
    fun `GIVEN each in-app server column alone on WHEN mapping to the entity THEN only its column is on`() {
        // Given
        val dtos = inAppTypes.associateWith(::inAppOnlyDto)

        // When
        val rows = dtos.mapValues { (_, dto) -> mapper.toEntity(dto, "user-1") }

        // Then
        assertEquals(inAppTypes.associateWith(::inAppOnlyEntity), rows)
    }

    @Test
    fun `GIVEN each push server column alone on WHEN mapping to the entity THEN only its column is on`() {
        // Given
        val dtos = NotificationType.entries.associateWith(::pushOnlyDto)

        // When
        val rows = dtos.mapValues { (_, dto) -> mapper.toEntity(dto, "user-1") }

        // Then
        assertEquals(NotificationType.entries.associateWith(::pushOnlyEntity), rows)
    }

    @Test
    fun `GIVEN bills with push on and in-app off WHEN mapping to the domain THEN only the push of bills is on`() {
        // Given
        val stored = entity(
            newExpense = true,
            paymentMarked = true,
            settleReminders = true,
            invites = false,
            inAppBills = false,
        )

        // When
        val preferences = mapper.toDomain(stored)

        // Then
        assertTrue(preferences.isEnabled(NotificationType.BILLS, NotificationChannel.PUSH))
        assertFalse(preferences.isEnabled(NotificationType.BILLS, NotificationChannel.IN_APP))
        assertEquals(inAppTypes - NotificationType.BILLS, preferences.inApp)
    }

    @Test
    fun `GIVEN people and groups with push off and in-app on WHEN mapping to the domain THEN only its in-app is on`() {
        // Given
        val stored = entity(
            newExpense = true,
            paymentMarked = true,
            settleReminders = true,
            invites = false,
            inAppPeopleAndGroups = true,
        )

        // When
        val preferences = mapper.toDomain(stored)

        // Then
        assertFalse(preferences.isEnabled(NotificationType.PEOPLE_AND_GROUPS, NotificationChannel.PUSH))
        assertTrue(preferences.isEnabled(NotificationType.PEOPLE_AND_GROUPS, NotificationChannel.IN_APP))
    }

    @Test
    fun `GIVEN the in-app of comments off WHEN mapping to the entity THEN its column is off and its push stays on`() {
        // Given
        val preferences = NotificationPreferences.DEFAULT.with(
            NotificationType.COMMENTS,
            NotificationChannel.IN_APP,
            isEnabled = false,
        )

        // When
        val stored = mapper.toEntity(
            preferences = preferences,
            updatedAt = 1L,
            ownerUserId = "user-1",
        )

        // Then
        assertEquals(
            entity(
                newExpense = true,
                paymentMarked = true,
                settleReminders = true,
                invites = false,
                comments = true,
                paymentRequests = true,
                inAppComments = false,
                pendingSync = true,
            ),
            stored,
        )
    }

    private val inAppTypes = NotificationType.entries.filterTo(mutableSetOf()) { it.hasInApp }

    // Why: the column each type is stored under, written out here so the test checks the mapper against it.
    private fun pushOnlyEntity(type: NotificationType): NotificationPreferencesEntity = entity(
        newExpense = type == NotificationType.BILLS,
        paymentMarked = type == NotificationType.PAYMENTS,
        settleReminders = type == NotificationType.BALANCE_REMINDER,
        invites = type == NotificationType.PEOPLE_AND_GROUPS,
        comments = type == NotificationType.COMMENTS,
        paymentRequests = type == NotificationType.PAYMENT_REQUESTS,
        inAppBills = false,
        inAppPayments = false,
        inAppComments = false,
        inAppPeopleAndGroups = false,
        inAppPaymentRequests = false,
    )

    private fun inAppOnlyEntity(type: NotificationType): NotificationPreferencesEntity = entity(
        newExpense = false,
        paymentMarked = false,
        settleReminders = false,
        invites = false,
        inAppBills = type == NotificationType.BILLS,
        inAppPayments = type == NotificationType.PAYMENTS,
        inAppComments = type == NotificationType.COMMENTS,
        inAppPeopleAndGroups = type == NotificationType.PEOPLE_AND_GROUPS,
        inAppPaymentRequests = type == NotificationType.PAYMENT_REQUESTS,
    )

    private fun pushOnlyDto(type: NotificationType): NotificationPreferencesDto = NotificationPreferencesDto(
        userId = "user-1",
        newExpense = type == NotificationType.BILLS,
        paymentMarked = type == NotificationType.PAYMENTS,
        settleReminders = type == NotificationType.BALANCE_REMINDER,
        invites = type == NotificationType.PEOPLE_AND_GROUPS,
        comments = type == NotificationType.COMMENTS,
        paymentRequests = type == NotificationType.PAYMENT_REQUESTS,
        inAppBills = false,
        inAppPayments = false,
        inAppComments = false,
        inAppPeopleAndGroups = false,
        inAppPaymentRequests = false,
        updatedAt = 1L,
    )

    private fun inAppOnlyDto(type: NotificationType): NotificationPreferencesDto = NotificationPreferencesDto(
        userId = "user-1",
        newExpense = false,
        paymentMarked = false,
        settleReminders = false,
        invites = false,
        comments = false,
        paymentRequests = false,
        inAppBills = type == NotificationType.BILLS,
        inAppPayments = type == NotificationType.PAYMENTS,
        inAppComments = type == NotificationType.COMMENTS,
        inAppPeopleAndGroups = type == NotificationType.PEOPLE_AND_GROUPS,
        inAppPaymentRequests = type == NotificationType.PAYMENT_REQUESTS,
        updatedAt = 1L,
    )

    private fun entity(
        newExpense: Boolean,
        paymentMarked: Boolean,
        settleReminders: Boolean,
        invites: Boolean,
        comments: Boolean = false,
        paymentRequests: Boolean = false,
        inAppBills: Boolean = true,
        inAppPayments: Boolean = true,
        inAppComments: Boolean = true,
        inAppPeopleAndGroups: Boolean = true,
        inAppPaymentRequests: Boolean = true,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        ownerUserId: String? = "user-1",
    ): NotificationPreferencesEntity = NotificationPreferencesEntity(
        id = SINGLE_ROW_ID,
        newExpense = newExpense,
        paymentMarked = paymentMarked,
        settleReminders = settleReminders,
        invites = invites,
        comments = comments,
        paymentRequests = paymentRequests,
        inAppBills = inAppBills,
        inAppPayments = inAppPayments,
        inAppComments = inAppComments,
        inAppPeopleAndGroups = inAppPeopleAndGroups,
        inAppPaymentRequests = inAppPaymentRequests,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        ownerUserId = ownerUserId,
    )
}
