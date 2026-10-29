package com.quare.blitzsplit.feature.notificationsettings

import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationPermission
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationSettingsUiStateTest {
    @Test
    fun `GIVEN any state WHEN listing the notices THEN lists the four in the order of the design`() {
        // Given
        val state = NotificationSettingsUiState(
            enabledTypes = emptySet(),
            permission = null,
        )

        // When
        val types = state.types

        // Then
        assertEquals(
            listOf(
                NotificationType.NEW_EXPENSE,
                NotificationType.PAYMENT_MARKED,
                NotificationType.SETTLE_REMINDERS,
                NotificationType.INVITES,
            ),
            types,
        )
    }

    @Test
    fun `GIVEN the permission not read yet WHEN drawing the toggles THEN enables them with the saved choices`() {
        // Given
        val state = NotificationSettingsUiState(
            enabledTypes = newExpenseAndInvites,
            permission = null,
        )

        // When
        val enabled = state.types.filter(state::isEnabled)

        // Then
        assertTrue(state.areTogglesEnabled)
        assertFalse(state.showsPermissionDenied)
        assertEquals(listOf(NotificationType.NEW_EXPENSE, NotificationType.INVITES), enabled)
    }

    @Test
    fun `GIVEN the permission granted WHEN drawing the toggles THEN enables them with the saved choices`() {
        // Given
        val state = NotificationSettingsUiState(
            enabledTypes = newExpenseAndInvites,
            permission = NotificationPermission.GRANTED,
        )

        // When
        val enabled = state.types.filter(state::isEnabled)

        // Then
        assertTrue(state.areTogglesEnabled)
        assertFalse(state.showsPermissionDenied)
        assertEquals(listOf(NotificationType.NEW_EXPENSE, NotificationType.INVITES), enabled)
    }

    @Test
    fun `GIVEN the permission never asked WHEN drawing the toggles THEN enables them with the saved choices`() {
        // Given
        val state = NotificationSettingsUiState(
            enabledTypes = newExpenseAndInvites,
            permission = NotificationPermission.NOT_DETERMINED,
        )

        // When
        val enabled = state.types.filter(state::isEnabled)

        // Then
        assertTrue(state.areTogglesEnabled)
        assertFalse(state.showsPermissionDenied)
        assertEquals(listOf(NotificationType.NEW_EXPENSE, NotificationType.INVITES), enabled)
    }

    @Test
    fun `GIVEN the permission denied WHEN drawing the toggles THEN shows the notice and every toggle off`() {
        // Given
        val state = NotificationSettingsUiState(
            enabledTypes = NotificationType.entries.toSet(),
            permission = NotificationPermission.DENIED,
        )

        // When
        val enabled = state.types.filter(state::isEnabled)

        // Then
        assertTrue(state.showsPermissionDenied)
        assertFalse(state.areTogglesEnabled)
        assertTrue(enabled.isEmpty())
        assertEquals(NotificationType.entries.toSet(), state.enabledTypes)
    }

    private val newExpenseAndInvites = setOf(NotificationType.NEW_EXPENSE, NotificationType.INVITES)
}
