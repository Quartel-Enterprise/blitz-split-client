package com.quare.blitzsplit.feature.notificationsettings

import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationPermission
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiState
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded
import software.quare.loadable.valueOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationSettingsUiStateTest {
    @Test
    fun `GIVEN any state WHEN listing the notices THEN lists the rows in the order of the filter then the reminder`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = pushOf(emptySet()).toLoaded(),
            permission = null,
        )

        // When
        val types = state.types

        // Then
        assertEquals(
            listOf(
                NotificationType.BILLS,
                NotificationType.PAYMENTS,
                NotificationType.COMMENTS,
                NotificationType.PEOPLE_AND_GROUPS,
                NotificationType.PAYMENT_REQUESTS,
                NotificationType.BALANCE_REMINDER,
            ),
            types,
        )
    }

    @Test
    fun `GIVEN the permission not read yet WHEN drawing the toggles THEN shimmers them with no denied notice`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = pushOf(newExpenseAndInvites).toLoaded(),
            permission = null,
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) }

        // Then
        assertTrue(state.showsTogglesLoading)
        assertFalse(state.arePushTogglesEnabled)
        assertFalse(state.showsPermissionDenied)
        assertTrue(enabled.isEmpty())
    }

    @Test
    fun `GIVEN the choices still loading WHEN drawing the toggles THEN shimmers them disabled`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = Loadable.Loading,
            permission = NotificationPermission.GRANTED,
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) }

        // Then
        assertTrue(state.showsTogglesLoading)
        assertFalse(state.arePushTogglesEnabled)
        assertTrue(enabled.isEmpty())
    }

    @Test
    fun `GIVEN the permission granted WHEN drawing the toggles THEN enables them with the saved choices`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = pushOf(newExpenseAndInvites).toLoaded(),
            permission = NotificationPermission.GRANTED,
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) }

        // Then
        assertTrue(state.arePushTogglesEnabled)
        assertFalse(state.showsPermissionDenied)
        assertEquals(listOf(NotificationType.BILLS, NotificationType.PEOPLE_AND_GROUPS), enabled)
    }

    @Test
    fun `GIVEN the permission never asked WHEN drawing the toggles THEN enables them with the saved choices`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = pushOf(newExpenseAndInvites).toLoaded(),
            permission = NotificationPermission.NOT_DETERMINED,
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) }

        // Then
        assertTrue(state.arePushTogglesEnabled)
        assertFalse(state.showsPermissionDenied)
        assertEquals(listOf(NotificationType.BILLS, NotificationType.PEOPLE_AND_GROUPS), enabled)
    }

    @Test
    fun `GIVEN the permission denied WHEN drawing the toggles THEN shows the notice and every push toggle off`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = pushOf(NotificationType.entries.toSet()).toLoaded(),
            permission = NotificationPermission.DENIED,
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) }

        // Then
        assertTrue(state.showsPermissionDenied)
        assertFalse(state.arePushTogglesEnabled)
        assertTrue(enabled.isEmpty())
        assertEquals(NotificationType.entries.toSet(), state.preferences.valueOrNull()?.push)
    }

    @Test
    fun `GIVEN any state WHEN listing the columns THEN lists Push then In app`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = pushOf(emptySet()).toLoaded(),
            permission = null,
        )

        // When
        val channels = state.channels

        // Then
        assertEquals(listOf(NotificationChannel.PUSH, NotificationChannel.IN_APP), channels)
    }

    @Test
    fun `GIVEN the permission denied WHEN drawing the toggles THEN enables In app and disables Push`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = pushOf(NotificationType.entries.toSet()).toLoaded(),
            permission = NotificationPermission.DENIED,
        )

        // When
        val inAppEnabled = state.areTogglesEnabled(NotificationChannel.IN_APP)

        // Then
        assertTrue(inAppEnabled)
        assertTrue(state.areInAppTogglesEnabled)
        assertFalse(state.areTogglesEnabled(NotificationChannel.PUSH))
        assertFalse(state.arePushTogglesEnabled)
    }

    @Test
    fun `GIVEN the permission denied WHEN drawing the In app toggles THEN shows the saved choices`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = inAppOf(billsAndComments).toLoaded(),
            permission = NotificationPermission.DENIED,
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.IN_APP) }

        // Then
        assertEquals(listOf(NotificationType.BILLS, NotificationType.COMMENTS), enabled)
    }

    @Test
    fun `GIVEN the permission granted WHEN drawing the In app toggles THEN enables them with the saved choices`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = inAppOf(billsAndComments).toLoaded(),
            permission = NotificationPermission.GRANTED,
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.IN_APP) }

        // Then
        assertTrue(state.areTogglesEnabled(NotificationChannel.IN_APP))
        assertTrue(state.areTogglesEnabled(NotificationChannel.PUSH))
        assertEquals(listOf(NotificationType.BILLS, NotificationType.COMMENTS), enabled)
    }

    @Test
    fun `GIVEN the permission never asked WHEN drawing the In app toggles THEN enables them with the saved choices`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = inAppOf(billsAndComments).toLoaded(),
            permission = NotificationPermission.NOT_DETERMINED,
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.IN_APP) }

        // Then
        assertTrue(state.areInAppTogglesEnabled)
        assertEquals(listOf(NotificationType.BILLS, NotificationType.COMMENTS), enabled)
    }

    @Test
    fun `GIVEN the permission not read yet WHEN drawing the In app toggles THEN shimmers them disabled and off`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = inAppOf(billsAndComments).toLoaded(),
            permission = null,
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.IN_APP) }

        // Then
        assertTrue(state.showsTogglesLoading)
        assertFalse(state.areInAppTogglesEnabled)
        assertFalse(state.areTogglesEnabled(NotificationChannel.IN_APP))
        assertTrue(enabled.isEmpty())
    }

    @Test
    fun `GIVEN the choices still loading WHEN drawing the In app toggles THEN shimmers them disabled and off`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = Loadable.Loading,
            permission = NotificationPermission.DENIED,
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.IN_APP) }

        // Then
        assertTrue(state.showsTogglesLoading)
        assertFalse(state.areInAppTogglesEnabled)
        assertFalse(state.areTogglesEnabled(NotificationChannel.IN_APP))
        assertTrue(enabled.isEmpty())
    }

    @Test
    fun `GIVEN the weekly reminder WHEN checking its In app cell THEN has no toggle`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            permission = NotificationPermission.GRANTED,
        )

        // When
        val hasToggle = state.hasToggle(NotificationType.BALANCE_REMINDER, NotificationChannel.IN_APP)

        // Then
        assertFalse(hasToggle)
    }

    @Test
    fun `GIVEN the weekly reminder WHEN checking its Push cell THEN has a toggle`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            permission = NotificationPermission.GRANTED,
        )

        // When
        val hasToggle = state.hasToggle(NotificationType.BALANCE_REMINDER, NotificationChannel.PUSH)

        // Then
        assertTrue(hasToggle)
    }

    @Test
    fun `GIVEN every row but the weekly reminder WHEN checking their cells THEN each has both toggles`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            permission = NotificationPermission.GRANTED,
        )
        val rows = state.types - NotificationType.BALANCE_REMINDER

        // When
        val withoutToggle = rows.flatMap { type ->
            state.channels.filterNot { state.hasToggle(type, it) }.map { type to it }
        }

        // Then
        assertEquals(5, rows.size)
        assertTrue(withoutToggle.isEmpty())
    }

    @Test
    fun `GIVEN choices holding the weekly reminder in app WHEN drawing its In app cell THEN shows it off`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = inAppOf(NotificationType.entries.toSet()).toLoaded(),
            permission = NotificationPermission.GRANTED,
        )

        // When
        val enabled = state.isEnabled(NotificationType.BALANCE_REMINDER, NotificationChannel.IN_APP)

        // Then
        assertFalse(enabled)
    }

    @Test
    fun `GIVEN a type on for Push only WHEN drawing its row THEN shows Push on and In app off`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences(
                push = setOf(NotificationType.PAYMENTS),
                inApp = setOf(NotificationType.BILLS),
            ).toLoaded(),
            permission = NotificationPermission.GRANTED,
        )

        // When
        val inAppEnabled = state.isEnabled(NotificationType.PAYMENTS, NotificationChannel.IN_APP)

        // Then
        assertFalse(inAppEnabled)
        assertTrue(state.isEnabled(NotificationType.PAYMENTS, NotificationChannel.PUSH))
    }

    private val newExpenseAndInvites = setOf(NotificationType.BILLS, NotificationType.PEOPLE_AND_GROUPS)

    private val billsAndComments = setOf(NotificationType.BILLS, NotificationType.COMMENTS)

    private fun inAppOf(types: Set<NotificationType>): NotificationPreferences = NotificationPreferences(
        push = NotificationPreferences.DEFAULT.push,
        inApp = types,
    )

    private fun pushOf(types: Set<NotificationType>): NotificationPreferences = NotificationPreferences(
        push = types,
        inApp = NotificationPreferences.DEFAULT.inApp,
    )
}
