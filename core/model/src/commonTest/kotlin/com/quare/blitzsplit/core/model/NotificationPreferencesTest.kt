package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPreferencesTest {
    private val everyType = NotificationType.entries.toSet()
    private val typesWithInApp = setOf(
        NotificationType.BILLS,
        NotificationType.PAYMENTS,
        NotificationType.COMMENTS,
        NotificationType.PEOPLE_AND_GROUPS,
        NotificationType.PAYMENT_REQUESTS,
    )

    @Test
    fun `GIVEN the six rows WHEN asking which have an in-app THEN all but the balance reminder have one`() {
        // Given
        val types = NotificationType.entries

        // When
        val withInApp = types.filter { it.hasInApp }.toSet()

        // Then
        assertEquals(typesWithInApp, withInApp)
    }

    @Test
    fun `GIVEN the defaults WHEN reading the push types THEN every push is on but people and groups`() {
        // Given
        val preferences = NotificationPreferences.DEFAULT

        // When
        val push = preferences.typesOf(NotificationChannel.PUSH)

        // Then
        assertEquals(everyType - NotificationType.PEOPLE_AND_GROUPS, push)
    }

    @Test
    fun `GIVEN the defaults WHEN reading the in-app types THEN every in-app the rows have is on`() {
        // Given
        val preferences = NotificationPreferences.DEFAULT

        // When
        val inApp = preferences.typesOf(NotificationChannel.IN_APP)

        // Then
        assertEquals(typesWithInApp, inApp)
    }

    @Test
    fun `GIVEN bills push only and comments in-app only WHEN asking each channel THEN each is on where chosen`() {
        // Given
        val preferences = NotificationPreferences(
            push = setOf(NotificationType.BILLS),
            inApp = setOf(NotificationType.COMMENTS),
        )

        // When
        val enabled = listOf(
            preferences.isEnabled(NotificationType.BILLS, NotificationChannel.PUSH),
            preferences.isEnabled(NotificationType.BILLS, NotificationChannel.IN_APP),
            preferences.isEnabled(NotificationType.COMMENTS, NotificationChannel.PUSH),
            preferences.isEnabled(NotificationType.COMMENTS, NotificationChannel.IN_APP),
        )

        // Then
        assertEquals(listOf(true, false, false, true), enabled)
    }

    @Test
    fun `GIVEN the defaults WHEN asking the balance reminder in-app THEN is off`() {
        // Given
        val preferences = NotificationPreferences.DEFAULT

        // When
        val enabled = preferences.isEnabled(NotificationType.BALANCE_REMINDER, NotificationChannel.IN_APP)

        // Then
        assertFalse(enabled)
    }

    @Test
    fun `GIVEN different push and in-app choices WHEN reading each channel THEN each gives its own types`() {
        // Given
        val preferences = NotificationPreferences(
            push = setOf(NotificationType.PAYMENTS, NotificationType.BALANCE_REMINDER),
            inApp = setOf(NotificationType.PEOPLE_AND_GROUPS),
        )

        // When
        val byChannel = NotificationChannel.entries.associateWith(preferences::typesOf)

        // Then
        assertEquals(
            mapOf(
                NotificationChannel.PUSH to setOf(NotificationType.PAYMENTS, NotificationType.BALANCE_REMINDER),
                NotificationChannel.IN_APP to setOf(NotificationType.PEOPLE_AND_GROUPS),
            ),
            byChannel,
        )
    }

    @Test
    fun `GIVEN the defaults WHEN turning off the push of bills THEN only that push goes off and in-app stays`() {
        // Given
        val preferences = NotificationPreferences.DEFAULT

        // When
        val changed = preferences.with(NotificationType.BILLS, NotificationChannel.PUSH, isEnabled = false)

        // Then
        assertEquals(
            NotificationPreferences(
                push = everyType - NotificationType.PEOPLE_AND_GROUPS - NotificationType.BILLS,
                inApp = typesWithInApp,
            ),
            changed,
        )
    }

    @Test
    fun `GIVEN the defaults WHEN turning on the push of people and groups THEN every push is on and in-app stays`() {
        // Given
        val preferences = NotificationPreferences.DEFAULT

        // When
        val changed = preferences.with(NotificationType.PEOPLE_AND_GROUPS, NotificationChannel.PUSH, isEnabled = true)

        // Then
        assertEquals(
            NotificationPreferences(
                push = everyType,
                inApp = typesWithInApp,
            ),
            changed,
        )
    }

    @Test
    fun `GIVEN the defaults WHEN turning off the in-app of comments THEN only that in-app goes off and push stays`() {
        // Given
        val preferences = NotificationPreferences.DEFAULT

        // When
        val changed = preferences.with(NotificationType.COMMENTS, NotificationChannel.IN_APP, isEnabled = false)

        // Then
        assertEquals(
            NotificationPreferences(
                push = everyType - NotificationType.PEOPLE_AND_GROUPS,
                inApp = typesWithInApp - NotificationType.COMMENTS,
            ),
            changed,
        )
    }

    @Test
    fun `GIVEN the in-app of payment requests off WHEN turning it on THEN it is on again and push stays`() {
        // Given
        val preferences = NotificationPreferences(
            push = setOf(NotificationType.BILLS),
            inApp = setOf(NotificationType.BILLS),
        )

        // When
        val changed = preferences.with(NotificationType.PAYMENT_REQUESTS, NotificationChannel.IN_APP, isEnabled = true)

        // Then
        assertEquals(
            NotificationPreferences(
                push = setOf(NotificationType.BILLS),
                inApp = setOf(NotificationType.BILLS, NotificationType.PAYMENT_REQUESTS),
            ),
            changed,
        )
    }

    @Test
    fun `GIVEN the defaults WHEN turning on the in-app of the balance reminder THEN nothing changes`() {
        // Given
        val preferences = NotificationPreferences.DEFAULT

        // When
        val changed = preferences.with(NotificationType.BALANCE_REMINDER, NotificationChannel.IN_APP, isEnabled = true)

        // Then
        assertEquals(NotificationPreferences.DEFAULT, changed)
        assertFalse(changed.isEnabled(NotificationType.BALANCE_REMINDER, NotificationChannel.IN_APP))
    }

    @Test
    fun `GIVEN every in-app off WHEN turning on the in-app of the balance reminder THEN in-app stays empty`() {
        // Given
        val preferences = NotificationPreferences(
            push = emptySet(),
            inApp = emptySet(),
        )

        // When
        val changed = preferences.with(NotificationType.BALANCE_REMINDER, NotificationChannel.IN_APP, isEnabled = true)

        // Then
        assertTrue(changed.inApp.isEmpty())
        assertTrue(changed.push.isEmpty())
    }

    @Test
    fun `GIVEN the defaults WHEN turning off the push of the balance reminder THEN only that push goes off`() {
        // Given
        val preferences = NotificationPreferences.DEFAULT

        // When
        val changed = preferences.with(NotificationType.BALANCE_REMINDER, NotificationChannel.PUSH, isEnabled = false)

        // Then
        assertEquals(
            NotificationPreferences(
                push = everyType - NotificationType.PEOPLE_AND_GROUPS - NotificationType.BALANCE_REMINDER,
                inApp = typesWithInApp,
            ),
            changed,
        )
    }

    @Test
    fun `GIVEN the in-app of payments already on WHEN turning it on THEN the preferences stay equal`() {
        // Given
        val preferences = NotificationPreferences.DEFAULT

        // When
        val changed = preferences.with(NotificationType.PAYMENTS, NotificationChannel.IN_APP, isEnabled = true)

        // Then
        assertEquals(preferences, changed)
    }

    @Test
    fun `GIVEN the push of bills off WHEN turning off the in-app of bills too THEN both channels of bills are off`() {
        // Given
        val preferences = NotificationPreferences.DEFAULT.with(
            NotificationType.BILLS,
            NotificationChannel.PUSH,
            isEnabled = false,
        )

        // When
        val changed = preferences.with(NotificationType.BILLS, NotificationChannel.IN_APP, isEnabled = false)

        // Then
        assertFalse(changed.isEnabled(NotificationType.BILLS, NotificationChannel.PUSH))
        assertFalse(changed.isEnabled(NotificationType.BILLS, NotificationChannel.IN_APP))
        assertEquals(typesWithInApp - NotificationType.BILLS, changed.inApp)
    }
}
