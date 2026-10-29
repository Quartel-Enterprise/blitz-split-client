package com.quare.blitzsplit.feature.notificationsettings

import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveNotificationPreferencesUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.UpdateNotificationPreferenceUseCase
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationPermission
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiAction
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiEvent
import com.quare.blitzsplit.feature.notificationsettings.presentation.viewmodel.NotificationSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationSettingsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var preferences: FakeNotificationPreferencesRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: NotificationSettingsViewModel
    private val actions = mutableListOf<NotificationSettingsUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN choices never changed WHEN the screen opens THEN every notice is on but invites`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                setOf(
                    NotificationType.NEW_EXPENSE,
                    NotificationType.PAYMENT_MARKED,
                    NotificationType.SETTLE_REMINDERS,
                ),
                state.types.filter(state::isEnabled).toSet(),
            )
            assertFalse(state.isEnabled(NotificationType.INVITES))
        }

    @Test
    fun `GIVEN the permission not read yet WHEN the screen opens THEN enables the toggles with no denied notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.permission)
            assertTrue(state.areTogglesEnabled)
            assertFalse(state.showsPermissionDenied)
        }

    @Test
    fun `GIVEN saved choices WHEN the screen opens THEN shows them`() = runTest(testDispatcher) {
        // Given
        prepareScenario(saved = setOf(NotificationType.INVITES))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(listOf(NotificationType.INVITES), state.types.filter(state::isEnabled))
    }

    @Test
    fun `GIVEN the screen open WHEN the saved choices change THEN shows the new ones`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val synced = NotificationPreferences(setOf(NotificationType.PAYMENT_MARKED, NotificationType.INVITES))

        // When
        preferences.preferences.value = synced

        // Then
        assertEquals(synced.enabledTypes, viewModel.uiState.value.enabledTypes)
    }

    @Test
    fun `GIVEN the screen open WHEN the system permission is reported THEN keeps it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val reported = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.GRANTED)

        // When
        viewModel.onEvent(reported)

        // Then
        assertEquals(NotificationPermission.GRANTED, viewModel.uiState.value.permission)
    }

    @Test
    fun `GIVEN the permission granted WHEN a notice is turned off THEN saves only that choice and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.GRANTED)
            val toggled = NotificationSettingsUiEvent.OnToggled(NotificationType.PAYMENT_MARKED, isEnabled = false)

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(listOf(NotificationType.PAYMENT_MARKED to false), preferences.updates)
            assertFalse(viewModel.uiState.value.isEnabled(NotificationType.PAYMENT_MARKED))
            assertTrue(viewModel.uiState.value.isEnabled(NotificationType.NEW_EXPENSE))
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission granted WHEN a notice is turned on THEN saves it and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.GRANTED)
            val toggled = NotificationSettingsUiEvent.OnToggled(NotificationType.INVITES, isEnabled = true)

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(listOf(NotificationType.INVITES to true), preferences.updates)
            assertTrue(viewModel.uiState.value.isEnabled(NotificationType.INVITES))
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission never asked WHEN a notice is turned on THEN saves it and asks for the permission`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.NOT_DETERMINED)
            val toggled = NotificationSettingsUiEvent.OnToggled(NotificationType.INVITES, isEnabled = true)

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(listOf(NotificationType.INVITES to true), preferences.updates)
            assertEquals(listOf<NotificationSettingsUiAction>(NotificationSettingsUiAction.RequestPermission), actions)
        }

    @Test
    fun `GIVEN the permission never asked WHEN a notice is turned off THEN saves it and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.NOT_DETERMINED)
            val toggled = NotificationSettingsUiEvent.OnToggled(NotificationType.NEW_EXPENSE, isEnabled = false)

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(listOf(NotificationType.NEW_EXPENSE to false), preferences.updates)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission not read yet WHEN a notice is turned on THEN saves it and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val toggled = NotificationSettingsUiEvent.OnToggled(NotificationType.INVITES, isEnabled = true)

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(listOf(NotificationType.INVITES to true), preferences.updates)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN saved choices WHEN the permission is reported denied THEN shows the notice and every toggle off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val reported = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.DENIED)

            // When
            viewModel.onEvent(reported)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPermissionDenied)
            assertFalse(state.areTogglesEnabled)
            assertTrue(state.types.none(state::isEnabled))
            assertEquals(NotificationPreferences.DEFAULT.enabledTypes, state.enabledTypes)
            assertEquals(NotificationPreferences.DEFAULT, preferences.preferences.value)
            assertTrue(preferences.updates.isEmpty())
        }

    @Test
    fun `GIVEN the permission denied WHEN a notice is turned on THEN saves nothing and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.DENIED)
            val toggled = NotificationSettingsUiEvent.OnToggled(NotificationType.INVITES, isEnabled = true)

            // When
            viewModel.onEvent(toggled)

            // Then
            assertTrue(preferences.updates.isEmpty())
            assertEquals(NotificationPreferences.DEFAULT, preferences.preferences.value)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission denied WHEN a notice is turned off THEN saves nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(permission = NotificationPermission.DENIED)
        val toggled = NotificationSettingsUiEvent.OnToggled(NotificationType.NEW_EXPENSE, isEnabled = false)

        // When
        viewModel.onEvent(toggled)

        // Then
        assertTrue(preferences.updates.isEmpty())
        assertEquals(NotificationPreferences.DEFAULT, preferences.preferences.value)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN the permission denied WHEN it comes back granted THEN shows the saved choices again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = setOf(NotificationType.NEW_EXPENSE, NotificationType.INVITES),
                permission = NotificationPermission.DENIED,
            )
            val reported = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.GRANTED)

            // When
            viewModel.onEvent(reported)

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.showsPermissionDenied)
            assertTrue(state.areTogglesEnabled)
            assertEquals(
                listOf(NotificationType.NEW_EXPENSE, NotificationType.INVITES),
                state.types.filter(state::isEnabled),
            )
        }

    @Test
    fun `GIVEN the permission denied WHEN open settings is tapped THEN opens the system settings`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.DENIED)
            val tapped = NotificationSettingsUiEvent.OnOpenSettingsClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertEquals(
                listOf<NotificationSettingsUiAction>(NotificationSettingsUiAction.OpenSystemSettings),
                actions,
            )
        }

    @Test
    fun `GIVEN the screen pushed over the profile WHEN back is tapped THEN returns to the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val back = NotificationSettingsUiEvent.OnBackClicked

            // When
            viewModel.onEvent(back)

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the screen drawn inside the profile WHEN back is tapped THEN keeps the profile open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(insideProfile = true)
            val back = NotificationSettingsUiEvent.OnBackClicked

            // When
            viewModel.onEvent(back)

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    private fun TestScope.prepareScenario(
        saved: Set<NotificationType> = NotificationPreferences.DEFAULT.enabledTypes,
        permission: NotificationPermission? = null,
        insideProfile: Boolean = false,
    ) {
        preferences = FakeNotificationPreferencesRepository(initialPreferences = NotificationPreferences(saved))
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.Profile)
            if (!insideProfile) it.navigate(Route.NotificationSettings)
        }
        viewModel = NotificationSettingsViewModel(
            navigator = navigator,
            updateNotificationPreference = UpdateNotificationPreferenceUseCase(preferences),
            observeNotificationPreferences = ObserveNotificationPreferencesUseCase(preferences),
        )
        permission?.let { viewModel.onEvent(NotificationSettingsUiEvent.OnPermissionChecked(it)) }
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
