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
            devicePush = true.toLoaded(),
            permission = null,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = null,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = emptySet(),
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) }

        // Then
        assertTrue(state.arePushTogglesEnabled)
        assertFalse(state.showsPermissionDenied)
        assertEquals(listOf(NotificationType.BILLS, NotificationType.PEOPLE_AND_GROUPS), enabled)
    }

    @Test
    fun `GIVEN the permission never asked WHEN drawing the toggles THEN shows every push off and disabled`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = pushOf(newExpenseAndInvites).toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.NOT_DETERMINED,
            systemBlockedTypes = emptySet(),
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) }

        // Then
        assertFalse(state.isPushOn)
        assertFalse(state.arePushTogglesEnabled)
        assertFalse(state.showsPermissionDenied)
        assertTrue(enabled.isEmpty())
    }

    @Test
    fun `GIVEN the permission denied WHEN drawing the toggles THEN shows the notice and every push toggle off`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = pushOf(NotificationType.entries.toSet()).toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.DENIED,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = null,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = NotificationPermission.DENIED,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = NotificationPermission.DENIED,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = NotificationPermission.NOT_DETERMINED,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = null,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = NotificationPermission.DENIED,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = emptySet(),
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
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = emptySet(),
        )

        // When
        val inAppEnabled = state.isEnabled(NotificationType.PAYMENTS, NotificationChannel.IN_APP)

        // Then
        assertFalse(inAppEnabled)
        assertTrue(state.isEnabled(NotificationType.PAYMENTS, NotificationChannel.PUSH))
    }

    @Test
    fun `GIVEN the permission refused once WHEN drawing the toggles THEN shows the notice and every push toggle off`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = pushOf(NotificationType.entries.toSet()).toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.REFUSED_ONCE,
            systemBlockedTypes = emptySet(),
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) }

        // Then
        assertTrue(state.showsPermissionDenied)
        assertFalse(state.arePushTogglesEnabled)
        assertFalse(state.areTogglesEnabled(NotificationChannel.PUSH))
        assertFalse(state.showsTogglesLoading)
        assertTrue(enabled.isEmpty())
        assertEquals(NotificationType.entries.toSet(), state.preferences.valueOrNull()?.push)
    }

    @Test
    fun `GIVEN the permission refused once WHEN drawing the In app toggles THEN enables them with the saved choices`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = inAppOf(billsAndComments).toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.REFUSED_ONCE,
            systemBlockedTypes = emptySet(),
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.IN_APP) }

        // Then
        assertTrue(state.areInAppTogglesEnabled)
        assertTrue(state.areTogglesEnabled(NotificationChannel.IN_APP))
        assertEquals(listOf(NotificationType.BILLS, NotificationType.COMMENTS), enabled)
    }

    @Test
    fun `GIVEN the permission refused once WHEN drawing the notice THEN offers to ask again`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.REFUSED_ONCE,
            systemBlockedTypes = emptySet(),
        )

        // When
        val canAskAgain = state.canAskAgain

        // Then
        assertTrue(canAskAgain)
    }

    @Test
    fun `GIVEN the permission denied WHEN drawing the notice THEN offers the system settings instead`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.DENIED,
            systemBlockedTypes = emptySet(),
        )

        // When
        val canAskAgain = state.canAskAgain

        // Then
        assertFalse(canAskAgain)
        assertTrue(state.showsPermissionDenied)
    }

    @Test
    fun `GIVEN the permission granted WHEN checking the notice THEN cannot ask again`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = emptySet(),
        )

        // When
        val canAskAgain = state.canAskAgain

        // Then
        assertFalse(canAskAgain)
    }

    @Test
    fun `GIVEN the permission never asked WHEN checking the notice THEN cannot ask again`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.NOT_DETERMINED,
            systemBlockedTypes = emptySet(),
        )

        // When
        val canAskAgain = state.canAskAgain

        // Then
        assertFalse(canAskAgain)
    }

    @Test
    fun `GIVEN the permission not read yet WHEN checking the notice THEN cannot ask again`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            devicePush = true.toLoaded(),
            permission = null,
            systemBlockedTypes = emptySet(),
        )

        // When
        val canAskAgain = state.canAskAgain

        // Then
        assertFalse(canAskAgain)
    }

    @Test
    fun `GIVEN the permission granted and the bills channel silenced WHEN checking the bills row THEN is blocked`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = setOf(NotificationType.BILLS),
        )

        // When
        val blocked = state.isBlockedBySystem(NotificationType.BILLS)

        // Then
        assertTrue(blocked)
    }

    @Test
    fun `GIVEN quiet delivery and the bills channel silenced WHEN checking the bills row THEN is blocked`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.PROVISIONAL,
            systemBlockedTypes = setOf(NotificationType.BILLS),
        )

        // When
        val blocked = state.isBlockedBySystem(NotificationType.BILLS)

        // Then
        assertTrue(blocked)
    }

    @Test
    fun `GIVEN the permission denied and the bills channel silenced WHEN checking the bills row THEN is not blocked`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.DENIED,
            systemBlockedTypes = setOf(NotificationType.BILLS),
        )

        // When
        val blocked = state.isBlockedBySystem(NotificationType.BILLS)

        // Then
        assertFalse(blocked)
    }

    @Test
    fun `GIVEN the permission refused once and the bills channel silenced WHEN checking it THEN is not blocked`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.REFUSED_ONCE,
            systemBlockedTypes = setOf(NotificationType.BILLS),
        )

        // When
        val blocked = state.isBlockedBySystem(NotificationType.BILLS)

        // Then
        assertFalse(blocked)
    }

    @Test
    fun `GIVEN the permission not read yet and the bills channel silenced WHEN checking it THEN is not blocked`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            devicePush = true.toLoaded(),
            permission = null,
            systemBlockedTypes = setOf(NotificationType.BILLS),
        )

        // When
        val blocked = state.isBlockedBySystem(NotificationType.BILLS)

        // Then
        assertFalse(blocked)
    }

    @Test
    fun `GIVEN the permission granted and the bills channel silenced WHEN checking another row THEN is not blocked`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = setOf(NotificationType.BILLS),
        )

        // When
        val blocked = state.isBlockedBySystem(NotificationType.PAYMENTS)

        // Then
        assertFalse(blocked)
    }

    @Test
    fun `GIVEN the bills channel silenced and its push on WHEN drawing its Push THEN shows it off and disabled`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = pushOf(billsAndComments).toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = setOf(NotificationType.BILLS),
        )

        // When
        val enabled = state.isEnabled(NotificationType.BILLS, NotificationChannel.PUSH)

        // Then
        assertFalse(enabled)
        assertFalse(state.isToggleEnabled(NotificationType.BILLS, NotificationChannel.PUSH))
        assertEquals(billsAndComments, state.preferences.valueOrNull()?.push)
    }

    @Test
    fun `GIVEN the bills channel silenced WHEN drawing its In app THEN keeps it enabled with the saved choice`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = inAppOf(billsAndComments).toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = setOf(NotificationType.BILLS),
        )

        // When
        val enabled = state.isEnabled(NotificationType.BILLS, NotificationChannel.IN_APP)

        // Then
        assertTrue(enabled)
        assertTrue(state.isToggleEnabled(NotificationType.BILLS, NotificationChannel.IN_APP))
    }

    @Test
    fun `GIVEN the bills channel silenced WHEN drawing the other Push toggles THEN shows the saved choices`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = pushOf(billsAndComments).toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = setOf(NotificationType.BILLS),
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) }

        // Then
        assertEquals(listOf(NotificationType.COMMENTS), enabled)
        assertTrue(
            (state.types - NotificationType.BILLS).all { state.isToggleEnabled(it, NotificationChannel.PUSH) },
        )
        assertTrue(state.arePushTogglesEnabled)
    }

    @Test
    fun `GIVEN the weekly reminder channel silenced WHEN drawing its Push THEN shows it off and disabled`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = pushOf(NotificationType.entries.toSet()).toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.GRANTED,
            systemBlockedTypes = setOf(NotificationType.BALANCE_REMINDER),
        )

        // When
        val enabled = state.isEnabled(NotificationType.BALANCE_REMINDER, NotificationChannel.PUSH)

        // Then
        assertFalse(enabled)
        assertTrue(state.isBlockedBySystem(NotificationType.BALANCE_REMINDER))
        assertFalse(state.isToggleEnabled(NotificationType.BALANCE_REMINDER, NotificationChannel.PUSH))
    }

    @Test
    fun `GIVEN quiet delivery WHEN drawing the toggles THEN enables Push with the saved choices`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = pushOf(newExpenseAndInvites).toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.PROVISIONAL,
            systemBlockedTypes = emptySet(),
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) }

        // Then
        assertEquals(listOf(NotificationType.BILLS, NotificationType.PEOPLE_AND_GROUPS), enabled)
        assertTrue(state.arePushTogglesEnabled)
        assertTrue(state.types.all { state.isToggleEnabled(it, NotificationChannel.PUSH) })
        assertFalse(state.showsPermissionDenied)
        assertFalse(state.canAskAgain)
        assertFalse(state.showsTogglesLoading)
    }

    @Test
    fun `GIVEN quiet delivery WHEN drawing the notice THEN shows the quiet notice`() {
        // Given
        val state = NotificationSettingsUiState(
            preferences = NotificationPreferences.DEFAULT.toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.PROVISIONAL,
            systemBlockedTypes = emptySet(),
        )

        // When
        val showsQuietNotice = state.showsQuietNotice

        // Then
        assertTrue(showsQuietNotice)
    }

    @Test
    fun `GIVEN any other permission or none read WHEN drawing the notice THEN shows no quiet notice`() {
        // Given
        val permissions: List<NotificationPermission?> =
            listOf(null) + (NotificationPermission.entries - NotificationPermission.PROVISIONAL)

        // When
        val showingQuietNotice = permissions.filter { permission ->
            NotificationSettingsUiState(
                preferences = NotificationPreferences.DEFAULT.toLoaded(),
                devicePush = true.toLoaded(),
                permission = permission,
                systemBlockedTypes = emptySet(),
            ).showsQuietNotice
        }

        // Then
        assertEquals(5, permissions.size)
        assertTrue(showingQuietNotice.isEmpty())
    }

    @Test
    fun `GIVEN every permission or none read WHEN checking which allow pushes THEN only granted and quiet do`() {
        // Given
        val permissions = everyPermission

        // When
        val allowing = permissions.filter { permission -> state(permission = permission).isPermissionAllowed }

        // Then
        assertEquals(listOf(NotificationPermission.GRANTED, NotificationPermission.PROVISIONAL), allowing)
    }

    @Test
    fun `GIVEN the switch saved on WHEN drawing it under every permission THEN shows it on only when allowed`() {
        // Given
        val permissions = everyPermission

        // When
        val showingOn = permissions.filter { permission ->
            state(
                devicePush = true.toLoaded(),
                permission = permission,
            ).isPushOn
        }

        // Then
        assertEquals(listOf(NotificationPermission.GRANTED, NotificationPermission.PROVISIONAL), showingOn)
    }

    @Test
    fun `GIVEN the switch saved off WHEN drawing it under every permission THEN shows it off`() {
        // Given
        val permissions = everyPermission

        // When
        val showingOn = permissions.filter { permission ->
            state(
                devicePush = false.toLoaded(),
                permission = permission,
            ).isPushOn
        }

        // Then
        assertTrue(showingOn.isEmpty())
    }

    @Test
    fun `GIVEN the switch not read yet and granted WHEN drawing it THEN shimmers it off and untappable`() {
        // Given
        val state = state(
            devicePush = Loadable.Loading,
            permission = NotificationPermission.GRANTED,
        )

        // When
        val showsLoading = state.showsPushSwitchLoading

        // Then
        assertTrue(showsLoading)
        assertFalse(state.isPushOn)
        assertFalse(state.isPushSwitchEnabled)
    }

    @Test
    fun `GIVEN the permission not read yet WHEN drawing the switch THEN shimmers it off and untappable`() {
        // Given
        val state = state(
            devicePush = true.toLoaded(),
            permission = null,
        )

        // When
        val showsLoading = state.showsPushSwitchLoading

        // Then
        assertTrue(showsLoading)
        assertFalse(state.isPushOn)
        assertFalse(state.isPushSwitchEnabled)
    }

    @Test
    fun `GIVEN the switch and the permission both read WHEN drawing it THEN never shimmers and always takes a tap`() {
        // Given
        val states = listOf(true, false).flatMap { isEnabled ->
            NotificationPermission.entries.map {
                state(
                    devicePush = isEnabled.toLoaded(),
                    permission = it,
                )
            }
        }

        // When
        val shimmering = states.filter { it.showsPushSwitchLoading }

        // Then
        assertTrue(shimmering.isEmpty())
        assertTrue(states.all { it.isPushSwitchEnabled })
    }

    @Test
    fun `GIVEN the switch not read yet WHEN drawing the toggles THEN shimmers them with every push off and disabled`() {
        // Given
        val state = state(
            preferences = pushOf(newExpenseAndInvites).toLoaded(),
            devicePush = Loadable.Loading,
            permission = NotificationPermission.GRANTED,
        )

        // When
        val showsLoading = state.showsTogglesLoading

        // Then
        assertTrue(showsLoading)
        assertFalse(state.arePushTogglesEnabled)
        assertTrue(state.types.none { state.isEnabled(it, NotificationChannel.PUSH) })
    }

    @Test
    fun `GIVEN the choices the switch and the permission all read WHEN drawing the toggles THEN shimmers none`() {
        // Given
        val state = state(
            devicePush = false.toLoaded(),
            permission = NotificationPermission.DENIED,
        )

        // When
        val showsLoading = state.showsTogglesLoading

        // Then
        assertFalse(showsLoading)
    }

    @Test
    fun `GIVEN the switch off and granted WHEN drawing the toggles THEN shows every push off and disabled`() {
        // Given
        val state = state(
            preferences = pushOf(NotificationType.entries.toSet()).toLoaded(),
            devicePush = false.toLoaded(),
            permission = NotificationPermission.GRANTED,
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) }

        // Then
        assertTrue(enabled.isEmpty())
        assertFalse(state.arePushTogglesEnabled)
        assertTrue(state.types.none { state.isToggleEnabled(it, NotificationChannel.PUSH) })
        assertEquals(NotificationType.entries.toSet(), state.preferences.valueOrNull()?.push)
    }

    @Test
    fun `GIVEN the switch off during quiet delivery WHEN drawing the notice THEN shows no quiet notice`() {
        // Given
        val state = state(
            preferences = pushOf(newExpenseAndInvites).toLoaded(),
            devicePush = false.toLoaded(),
            permission = NotificationPermission.PROVISIONAL,
        )

        // When
        val showsQuietNotice = state.showsQuietNotice

        // Then
        assertFalse(showsQuietNotice)
    }

    @Test
    fun `GIVEN the switch off during quiet delivery WHEN drawing the toggles THEN shows every push off`() {
        // Given
        val state = state(
            preferences = pushOf(newExpenseAndInvites).toLoaded(),
            devicePush = false.toLoaded(),
            permission = NotificationPermission.PROVISIONAL,
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) }

        // Then
        assertTrue(enabled.isEmpty())
        assertFalse(state.arePushTogglesEnabled)
    }

    @Test
    fun `GIVEN the switch off and granted WHEN drawing the In app toggles THEN shows the saved choices`() {
        // Given
        val state = state(
            preferences = inAppOf(billsAndComments).toLoaded(),
            devicePush = false.toLoaded(),
            permission = NotificationPermission.GRANTED,
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.IN_APP) }

        // Then
        assertTrue(state.areInAppTogglesEnabled)
        assertEquals(listOf(NotificationType.BILLS, NotificationType.COMMENTS), enabled)
    }

    @Test
    fun `GIVEN the switch off and never asked WHEN drawing the In app toggles THEN keeps them enabled`() {
        // Given
        val state = state(
            preferences = inAppOf(billsAndComments).toLoaded(),
            devicePush = false.toLoaded(),
            permission = NotificationPermission.NOT_DETERMINED,
        )

        // When
        val enabled = state.types.filter { state.isEnabled(it, NotificationChannel.IN_APP) }

        // Then
        assertTrue(state.areInAppTogglesEnabled)
        assertEquals(listOf(NotificationType.BILLS, NotificationType.COMMENTS), enabled)
    }

    @Test
    fun `GIVEN the switch on and the permission refused once WHEN drawing the toggles THEN shows the switch off`() {
        // Given
        val state = state(
            preferences = pushOf(newExpenseAndInvites).toLoaded(),
            devicePush = true.toLoaded(),
            permission = NotificationPermission.REFUSED_ONCE,
        )

        // When
        val isPushOn = state.isPushOn

        // Then
        assertFalse(isPushOn)
        assertFalse(state.arePushTogglesEnabled)
    }

    private val everyPermission: List<NotificationPermission?> = NotificationPermission.entries + null

    private fun state(
        preferences: Loadable<NotificationPreferences> = NotificationPreferences.DEFAULT.toLoaded(),
        devicePush: Loadable<Boolean> = true.toLoaded(),
        permission: NotificationPermission?,
    ): NotificationSettingsUiState = NotificationSettingsUiState(
        preferences = preferences,
        devicePush = devicePush,
        permission = permission,
        systemBlockedTypes = emptySet(),
    )

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
