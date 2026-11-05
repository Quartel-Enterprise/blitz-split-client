package com.quare.blitzsplit.feature.notificationsettings

import com.quare.blitzsplit.core.activity.domain.usecase.MarkActivityTypeRead
import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationPermission
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveNotificationPreferencesUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.UpdateNotificationPreferenceUseCase
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiAction
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiEvent
import com.quare.blitzsplit.feature.notificationsettings.presentation.viewmodel.NotificationSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.loadable.Loadable
import software.quare.loadable.valueOrNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationSettingsViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var preferences: FakeNotificationPreferencesRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var firstDownload: MutableStateFlow<FirstDownloadProgress?>
    private lateinit var viewModel: NotificationSettingsViewModel
    private val actions = mutableListOf<NotificationSettingsUiAction>()
    private val markedReadTypes = mutableListOf<NotificationType>()

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
                    NotificationType.BILLS,
                    NotificationType.PAYMENTS,
                    NotificationType.COMMENTS,
                    NotificationType.PAYMENT_REQUESTS,
                    NotificationType.BALANCE_REMINDER,
                ),
                state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) }.toSet(),
            )
            assertFalse(state.isEnabled(NotificationType.PEOPLE_AND_GROUPS, NotificationChannel.PUSH))
        }

    @Test
    fun `GIVEN the permission not read yet WHEN the screen opens THEN shimmers the toggles with no denied notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.permission)
            assertTrue(state.showsTogglesLoading)
            assertFalse(state.arePushTogglesEnabled)
            assertFalse(state.showsPermissionDenied)
        }

    @Test
    fun `GIVEN a restored account not whole yet WHEN the screen opens THEN its choices load`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(firstDownload = pendingDownload)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(Loadable.Loading, state.preferences)
            assertTrue(state.showsTogglesLoading)
            assertFalse(state.isEnabled(NotificationType.BILLS, NotificationChannel.PUSH))
        }

    @Test
    fun `GIVEN a restored account not whole yet WHEN a notice is turned on THEN saves nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(firstDownload = pendingDownload)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.PEOPLE_AND_GROUPS,
                NotificationChannel.PUSH,
                isEnabled = true,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertTrue(preferences.updates.isEmpty())
        }

    @Test
    fun `GIVEN a restored account not whole yet WHEN its first full sync ends THEN shows the server choices`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = setOf(NotificationType.PEOPLE_AND_GROUPS),
                firstDownload = pendingDownload,
            )

            // When
            firstDownload.value = null

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.showsTogglesLoading)
            assertEquals(
                listOf(NotificationType.PEOPLE_AND_GROUPS),
                state.types.filter {
                    state.isEnabled(it, NotificationChannel.PUSH)
                },
            )
        }

    @Test
    fun `GIVEN saved choices WHEN the screen opens THEN shows them`() = runTest(testDispatcher) {
        // Given
        prepareScenario(saved = setOf(NotificationType.PEOPLE_AND_GROUPS))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(
            listOf(NotificationType.PEOPLE_AND_GROUPS),
            state.types.filter {
                state.isEnabled(it, NotificationChannel.PUSH)
            },
        )
    }

    @Test
    fun `GIVEN the screen open WHEN the saved choices change THEN shows the new ones`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val synced =
            NotificationPreferences(
                push = setOf(NotificationType.PAYMENTS, NotificationType.PEOPLE_AND_GROUPS),
                inApp = NotificationPreferences.DEFAULT.inApp,
            )

        // When
        preferences.preferences.value = synced

        // Then
        assertEquals(
            synced,
            viewModel.uiState.value.preferences
                .valueOrNull(),
        )
    }

    @Test
    fun `GIVEN the screen open WHEN the system permission is reported THEN keeps it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(permission = null)
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
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.PAYMENTS,
                NotificationChannel.PUSH,
                isEnabled = false,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(
                listOf(Triple(NotificationType.PAYMENTS, NotificationChannel.PUSH, false)),
                preferences.updates,
            )
            assertFalse(viewModel.uiState.value.isEnabled(NotificationType.PAYMENTS, NotificationChannel.PUSH))
            assertTrue(viewModel.uiState.value.isEnabled(NotificationType.BILLS, NotificationChannel.PUSH))
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission granted WHEN a notice is turned on THEN saves it and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.GRANTED)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.PEOPLE_AND_GROUPS,
                NotificationChannel.PUSH,
                isEnabled = true,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(
                listOf(Triple(NotificationType.PEOPLE_AND_GROUPS, NotificationChannel.PUSH, true)),
                preferences.updates,
            )
            assertTrue(viewModel.uiState.value.isEnabled(NotificationType.PEOPLE_AND_GROUPS, NotificationChannel.PUSH))
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission never asked WHEN a notice is turned on THEN saves it and asks for the permission`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.NOT_DETERMINED)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.PEOPLE_AND_GROUPS,
                NotificationChannel.PUSH,
                isEnabled = true,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(
                listOf(Triple(NotificationType.PEOPLE_AND_GROUPS, NotificationChannel.PUSH, true)),
                preferences.updates,
            )
            assertEquals(listOf<NotificationSettingsUiAction>(NotificationSettingsUiAction.RequestPermission), actions)
        }

    @Test
    fun `GIVEN the permission never asked WHEN a notice is turned off THEN saves it and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.NOT_DETERMINED)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.BILLS,
                NotificationChannel.PUSH,
                isEnabled = false,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(listOf(Triple(NotificationType.BILLS, NotificationChannel.PUSH, false)), preferences.updates)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission not read yet WHEN a notice is turned on THEN saves nothing and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = null)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.PEOPLE_AND_GROUPS,
                NotificationChannel.PUSH,
                isEnabled = true,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertTrue(preferences.updates.isEmpty())
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
            assertFalse(state.arePushTogglesEnabled)
            assertTrue(state.types.none { state.isEnabled(it, NotificationChannel.PUSH) })
            assertEquals(NotificationPreferences.DEFAULT.push, state.preferences.valueOrNull()?.push)
            assertEquals(NotificationPreferences.DEFAULT, preferences.preferences.value)
            assertTrue(preferences.updates.isEmpty())
        }

    @Test
    fun `GIVEN the permission denied WHEN a notice is turned on THEN saves nothing and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.DENIED)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.PEOPLE_AND_GROUPS,
                NotificationChannel.PUSH,
                isEnabled = true,
            )

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
        val toggled = NotificationSettingsUiEvent.OnToggled(
            NotificationType.BILLS,
            NotificationChannel.PUSH,
            isEnabled = false,
        )

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
                saved = setOf(NotificationType.BILLS, NotificationType.PEOPLE_AND_GROUPS),
                permission = NotificationPermission.DENIED,
            )
            val reported = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.GRANTED)

            // When
            viewModel.onEvent(reported)

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.showsPermissionDenied)
            assertTrue(state.arePushTogglesEnabled)
            assertEquals(
                listOf(NotificationType.BILLS, NotificationType.PEOPLE_AND_GROUPS),
                state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) },
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

    @Test
    fun `GIVEN the permission granted WHEN a notice is turned off THEN sends its type turned off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.GRANTED)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.PAYMENTS,
                NotificationChannel.PUSH,
                isEnabled = false,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(
                listOf(
                    "notification_toggled" to mapOf<String, Any>(
                        "type" to NotificationType.PAYMENTS,
                        "channel" to NotificationChannel.PUSH,
                        "is_enabled" to false,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the permission never asked WHEN a notice is turned on THEN sends its type turned on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.NOT_DETERMINED)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.PEOPLE_AND_GROUPS,
                NotificationChannel.PUSH,
                isEnabled = true,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(
                listOf(
                    "notification_toggled" to mapOf<String, Any>(
                        "type" to NotificationType.PEOPLE_AND_GROUPS,
                        "channel" to NotificationChannel.PUSH,
                        "is_enabled" to true,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the permission not read yet WHEN a notice is toggled THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(permission = null)
        val toggled = NotificationSettingsUiEvent.OnToggled(
            NotificationType.PEOPLE_AND_GROUPS,
            NotificationChannel.PUSH,
            isEnabled = true,
        )

        // When
        viewModel.onEvent(toggled)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the permission denied WHEN a notice is toggled THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(permission = NotificationPermission.DENIED)
        val toggled = NotificationSettingsUiEvent.OnToggled(
            NotificationType.BILLS,
            NotificationChannel.PUSH,
            isEnabled = false,
        )

        // When
        viewModel.onEvent(toggled)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a restored account not whole yet WHEN a notice is toggled THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(firstDownload = pendingDownload)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.PEOPLE_AND_GROUPS,
                NotificationChannel.PUSH,
                isEnabled = true,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the permission denied WHEN open settings is tapped twice THEN sends the tap each time`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.DENIED)
            viewModel.onEvent(NotificationSettingsUiEvent.OnOpenSettingsClicked)
            val tapped = NotificationSettingsUiEvent.OnOpenSettingsClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertEquals(
                listOf(
                    "notification_system_settings_clicked" to emptyMap<String, Any>(),
                    "notification_system_settings_clicked" to emptyMap(),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the screen open WHEN the system permission is reported THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val reported = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.DENIED)

        // When
        viewModel.onEvent(reported)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the screen pushed over the profile WHEN back is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val back = NotificationSettingsUiEvent.OnBackClicked

        // When
        viewModel.onEvent(back)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the permission denied WHEN an In app notice is turned off THEN saves it and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.DENIED)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.BILLS,
                NotificationChannel.IN_APP,
                isEnabled = false,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(listOf(Triple(NotificationType.BILLS, NotificationChannel.IN_APP, false)), preferences.updates)
            assertFalse(viewModel.uiState.value.isEnabled(NotificationType.BILLS, NotificationChannel.IN_APP))
            assertTrue(viewModel.uiState.value.isEnabled(NotificationType.PAYMENTS, NotificationChannel.IN_APP))
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission denied WHEN an In app notice is turned on THEN saves it and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                permission = NotificationPermission.DENIED,
                savedInApp = emptySet(),
            )
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.COMMENTS,
                NotificationChannel.IN_APP,
                isEnabled = true,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(
                listOf(Triple(NotificationType.COMMENTS, NotificationChannel.IN_APP, true)),
                preferences.updates,
            )
            assertTrue(viewModel.uiState.value.isEnabled(NotificationType.COMMENTS, NotificationChannel.IN_APP))
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission denied WHEN an In app notice is toggled THEN sends its type channel and choice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.DENIED)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.BILLS,
                NotificationChannel.IN_APP,
                isEnabled = false,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(
                listOf(
                    "notification_toggled" to mapOf<String, Any>(
                        "type" to NotificationType.BILLS,
                        "channel" to NotificationChannel.IN_APP,
                        "is_enabled" to false,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the permission never asked WHEN an In app notice is turned on THEN saves it and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                permission = NotificationPermission.NOT_DETERMINED,
                savedInApp = emptySet(),
            )
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.PAYMENTS,
                NotificationChannel.IN_APP,
                isEnabled = true,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(
                listOf(Triple(NotificationType.PAYMENTS, NotificationChannel.IN_APP, true)),
                preferences.updates,
            )
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission granted WHEN an In app notice is turned off THEN keeps its Push on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.GRANTED)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.BILLS,
                NotificationChannel.IN_APP,
                isEnabled = false,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isEnabled(NotificationType.BILLS, NotificationChannel.IN_APP))
            assertTrue(state.isEnabled(NotificationType.BILLS, NotificationChannel.PUSH))
            assertEquals(NotificationPreferences.DEFAULT.push, preferences.preferences.value.push)
        }

    @Test
    fun `GIVEN the permission granted WHEN the weekly reminder In app cell is turned on THEN ignores it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.GRANTED)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.BALANCE_REMINDER,
                NotificationChannel.IN_APP,
                isEnabled = true,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertTrue(preferences.updates.isEmpty())
            assertTrue(trackedEvents.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission never asked WHEN the weekly reminder In app cell is turned on THEN asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.NOT_DETERMINED)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.BALANCE_REMINDER,
                NotificationChannel.IN_APP,
                isEnabled = true,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertTrue(preferences.updates.isEmpty())
            assertTrue(trackedEvents.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission not read yet WHEN an In app notice is toggled THEN saves nothing and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = null)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.BILLS,
                NotificationChannel.IN_APP,
                isEnabled = false,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertTrue(preferences.updates.isEmpty())
            assertTrue(trackedEvents.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN a restored account not whole yet WHEN an In app notice is toggled THEN ignores it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                permission = NotificationPermission.DENIED,
                firstDownload = pendingDownload,
            )
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.BILLS,
                NotificationChannel.IN_APP,
                isEnabled = false,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertTrue(preferences.updates.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Push refused under a denied permission WHEN the same In app notice is turned off THEN takes it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.DENIED)
            viewModel.onEvent(
                NotificationSettingsUiEvent.OnToggled(
                    NotificationType.BILLS,
                    NotificationChannel.PUSH,
                    isEnabled = false,
                ),
            )
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.BILLS,
                NotificationChannel.IN_APP,
                isEnabled = false,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(listOf(Triple(NotificationType.BILLS, NotificationChannel.IN_APP, false)), preferences.updates)
            assertEquals(
                listOf(NotificationChannel.IN_APP),
                trackedEvents.map { (_, params) -> params["channel"] },
            )
            assertEquals(NotificationPreferences.DEFAULT.push, preferences.preferences.value.push)
        }

    private val pendingDownload = FirstDownloadProgress(
        arrivedGroupCount = 0,
        groupCount = null,
        isWaitingForInternet = false,
    )

    @Test
    fun `GIVEN the in-app of comments off WHEN turning it back on THEN saves its events read before turning it on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.DENIED)
            viewModel.onEvent(
                NotificationSettingsUiEvent.OnToggled(NotificationType.COMMENTS, NotificationChannel.IN_APP, false),
            )

            // When
            viewModel.onEvent(
                NotificationSettingsUiEvent.OnToggled(NotificationType.COMMENTS, NotificationChannel.IN_APP, true),
            )

            // Then
            assertEquals(listOf(NotificationType.COMMENTS), markedReadTypes)
            assertTrue(NotificationType.COMMENTS in preferences.preferences.value.inApp)
        }

    @Test
    fun `GIVEN the permission granted WHEN turning a push on or an in-app off THEN marks nothing read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.GRANTED)

            // When
            viewModel.onEvent(
                NotificationSettingsUiEvent.OnToggled(
                    NotificationType.PEOPLE_AND_GROUPS,
                    NotificationChannel.PUSH,
                    true,
                ),
            )
            viewModel.onEvent(
                NotificationSettingsUiEvent.OnToggled(NotificationType.BILLS, NotificationChannel.IN_APP, false),
            )

            // Then
            assertTrue(markedReadTypes.isEmpty())
        }

    @Test
    fun `GIVEN saved choices WHEN the permission is reported refused once THEN shows the notice that asks again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(saved = NotificationType.entries.toSet())
            val reported = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.REFUSED_ONCE)

            // When
            viewModel.onEvent(reported)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPermissionDenied)
            assertTrue(state.canAskAgain)
            assertFalse(state.arePushTogglesEnabled)
            assertTrue(state.areInAppTogglesEnabled)
            assertTrue(state.types.none { state.isEnabled(it, NotificationChannel.PUSH) })
            assertEquals(NotificationType.entries.toSet(), preferences.preferences.value.push)
            assertTrue(preferences.updates.isEmpty())
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the permission refused once WHEN a notice is turned on THEN saves sends and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.REFUSED_ONCE)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.PEOPLE_AND_GROUPS,
                NotificationChannel.PUSH,
                isEnabled = true,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertTrue(preferences.updates.isEmpty())
            assertEquals(NotificationPreferences.DEFAULT, preferences.preferences.value)
            assertTrue(trackedEvents.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission refused once WHEN a notice is turned off THEN saves nothing and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.REFUSED_ONCE)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.BILLS,
                NotificationChannel.PUSH,
                isEnabled = false,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertTrue(preferences.updates.isEmpty())
            assertEquals(NotificationPreferences.DEFAULT, preferences.preferences.value)
            assertTrue(trackedEvents.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission refused once WHEN an In app notice is turned off THEN saves it and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.REFUSED_ONCE)
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.BILLS,
                NotificationChannel.IN_APP,
                isEnabled = false,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(listOf(Triple(NotificationType.BILLS, NotificationChannel.IN_APP, false)), preferences.updates)
            assertFalse(viewModel.uiState.value.isEnabled(NotificationType.BILLS, NotificationChannel.IN_APP))
            assertTrue(viewModel.uiState.value.isEnabled(NotificationType.PAYMENTS, NotificationChannel.IN_APP))
            assertEquals(
                listOf(
                    "notification_toggled" to mapOf<String, Any>(
                        "type" to NotificationType.BILLS,
                        "channel" to NotificationChannel.IN_APP,
                        "is_enabled" to false,
                    ),
                ),
                trackedEvents,
            )
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the permission refused once WHEN turn on is tapped THEN asks the system again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.REFUSED_ONCE)
            val tapped = NotificationSettingsUiEvent.OnAskAgainClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertEquals(
                listOf<NotificationSettingsUiAction>(NotificationSettingsUiAction.RequestPermission),
                actions,
            )
            assertTrue(preferences.updates.isEmpty())
        }

    @Test
    fun `GIVEN the permission refused once WHEN turn on is tapped THEN sends the tap once with no params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.REFUSED_ONCE)
            val tapped = NotificationSettingsUiEvent.OnAskAgainClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertEquals(
                listOf("notification_ask_again_clicked" to emptyMap<String, Any>()),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN turn on WHEN reading its analytics THEN is sent by hand as notification_ask_again_clicked`() {
        // Given
        val event = NotificationSettingsUiEvent.OnAskAgainClicked

        // When
        val analytics = event.analytics

        // Then
        assertEquals(EventAnalytics.Manual("notification_ask_again_clicked"), analytics)
    }

    @Test
    fun `GIVEN the permission denied WHEN a late turn on arrives THEN asks nothing and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.DENIED)
            val tapped = NotificationSettingsUiEvent.OnAskAgainClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the permission granted WHEN a late turn on arrives THEN asks nothing and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.GRANTED)
            val tapped = NotificationSettingsUiEvent.OnAskAgainClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the permission never asked WHEN a late turn on arrives THEN asks nothing and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.NOT_DETERMINED)
            val tapped = NotificationSettingsUiEvent.OnAskAgainClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the permission not read yet WHEN a turn on arrives THEN asks nothing and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = null)
            val tapped = NotificationSettingsUiEvent.OnAskAgainClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the permission refused once that came back denied WHEN turn on arrives THEN asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.REFUSED_ONCE)
            viewModel.onEvent(NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.DENIED))
            val tapped = NotificationSettingsUiEvent.OnAskAgainClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the permission refused once WHEN it comes back granted THEN shows the saved choices again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = setOf(NotificationType.BILLS, NotificationType.PEOPLE_AND_GROUPS),
                permission = NotificationPermission.REFUSED_ONCE,
            )
            val reported = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.GRANTED)

            // When
            viewModel.onEvent(reported)

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.showsPermissionDenied)
            assertFalse(state.canAskAgain)
            assertTrue(state.arePushTogglesEnabled)
            assertEquals(
                listOf(NotificationType.BILLS, NotificationType.PEOPLE_AND_GROUPS),
                state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) },
            )
        }

    @Test
    fun `GIVEN the permission refused once WHEN it comes back denied THEN keeps the notice with the system settings`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.REFUSED_ONCE)
            val reported = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.DENIED)

            // When
            viewModel.onEvent(reported)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPermissionDenied)
            assertFalse(state.canAskAgain)
            assertFalse(state.arePushTogglesEnabled)
            assertTrue(state.types.none { state.isEnabled(it, NotificationChannel.PUSH) })
            assertEquals(NotificationPreferences.DEFAULT, preferences.preferences.value)
        }

    @Test
    fun `GIVEN granted WHEN Android reports the bills channel silenced THEN shows its Push off and disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.GRANTED)
            val checked = NotificationSettingsUiEvent.OnSystemBlockedTypesChecked(setOf(NotificationType.BILLS))

            // When
            viewModel.onEvent(checked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(setOf(NotificationType.BILLS), state.systemBlockedTypes)
            assertTrue(state.isBlockedBySystem(NotificationType.BILLS))
            assertFalse(state.isEnabled(NotificationType.BILLS, NotificationChannel.PUSH))
            assertFalse(state.isToggleEnabled(NotificationType.BILLS, NotificationChannel.PUSH))
            assertTrue(state.isEnabled(NotificationType.BILLS, NotificationChannel.IN_APP))
            assertTrue(state.isToggleEnabled(NotificationType.BILLS, NotificationChannel.IN_APP))
            assertTrue(state.isEnabled(NotificationType.PAYMENTS, NotificationChannel.PUSH))
            assertTrue(NotificationType.BILLS in preferences.preferences.value.push)
            assertTrue(preferences.updates.isEmpty())
        }

    @Test
    fun `GIVEN the permission granted WHEN Android reports a silenced channel THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.GRANTED)
            val checked = NotificationSettingsUiEvent.OnSystemBlockedTypesChecked(setOf(NotificationType.BILLS))

            // When
            viewModel.onEvent(checked)

            // Then
            assertTrue(trackedEvents.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the bills and payments channels silenced WHEN Android reports only payments THEN unblocks bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(systemBlocked = setOf(NotificationType.BILLS, NotificationType.PAYMENTS))
            val checked = NotificationSettingsUiEvent.OnSystemBlockedTypesChecked(setOf(NotificationType.PAYMENTS))

            // When
            viewModel.onEvent(checked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(setOf(NotificationType.PAYMENTS), state.systemBlockedTypes)
            assertFalse(state.isBlockedBySystem(NotificationType.BILLS))
            assertTrue(state.isEnabled(NotificationType.BILLS, NotificationChannel.PUSH))
            assertTrue(state.isToggleEnabled(NotificationType.BILLS, NotificationChannel.PUSH))
            assertFalse(state.isEnabled(NotificationType.PAYMENTS, NotificationChannel.PUSH))
        }

    @Test
    fun `GIVEN the bills channel silenced WHEN its Push is turned off THEN saves sends and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(systemBlocked = setOf(NotificationType.BILLS))
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.BILLS,
                NotificationChannel.PUSH,
                isEnabled = false,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertTrue(preferences.updates.isEmpty())
            assertEquals(NotificationPreferences.DEFAULT, preferences.preferences.value)
            assertTrue(trackedEvents.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN never asked and the invites channel silenced WHEN its Push is turned on THEN asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                permission = NotificationPermission.NOT_DETERMINED,
                systemBlocked = setOf(NotificationType.PEOPLE_AND_GROUPS),
            )
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.PEOPLE_AND_GROUPS,
                NotificationChannel.PUSH,
                isEnabled = true,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(preferences.updates.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the bills channel silenced WHEN its In app is turned off THEN saves it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(systemBlocked = setOf(NotificationType.BILLS))
        val toggled = NotificationSettingsUiEvent.OnToggled(
            NotificationType.BILLS,
            NotificationChannel.IN_APP,
            isEnabled = false,
        )

        // When
        viewModel.onEvent(toggled)

        // Then
        assertEquals(listOf(Triple(NotificationType.BILLS, NotificationChannel.IN_APP, false)), preferences.updates)
        assertFalse(viewModel.uiState.value.isEnabled(NotificationType.BILLS, NotificationChannel.IN_APP))
        assertEquals(listOf("notification_toggled"), trackedEvents.map { (name, _) -> name })
    }

    @Test
    fun `GIVEN the bills channel silenced WHEN its open settings is tapped THEN opens that channel settings`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(systemBlocked = setOf(NotificationType.BILLS))
            val tapped = NotificationSettingsUiEvent.OnChannelSettingsClicked(NotificationType.BILLS)

            // When
            viewModel.onEvent(tapped)

            // Then
            assertEquals(
                listOf<NotificationSettingsUiAction>(
                    NotificationSettingsUiAction.OpenChannelSettings(NotificationType.BILLS),
                ),
                actions,
            )
        }

    @Test
    fun `GIVEN the bills channel silenced WHEN its open settings is tapped THEN sends the tap with its type`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(systemBlocked = setOf(NotificationType.BILLS))
            val tapped = NotificationSettingsUiEvent.OnChannelSettingsClicked(NotificationType.BILLS)

            // When
            viewModel.onEvent(tapped)

            // Then
            assertEquals(
                listOf(
                    "notification_channel_settings_clicked" to mapOf<String, Any>("type" to NotificationType.BILLS),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the weekly reminder channel silenced WHEN its open settings is tapped THEN opens its channel`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(systemBlocked = setOf(NotificationType.BALANCE_REMINDER))
            val tapped = NotificationSettingsUiEvent.OnChannelSettingsClicked(NotificationType.BALANCE_REMINDER)

            // When
            viewModel.onEvent(tapped)

            // Then
            assertEquals(
                listOf<NotificationSettingsUiAction>(
                    NotificationSettingsUiAction.OpenChannelSettings(NotificationType.BALANCE_REMINDER),
                ),
                actions,
            )
            assertFalse(
                viewModel.uiState.value.isEnabled(NotificationType.BALANCE_REMINDER, NotificationChannel.PUSH),
            )
        }

    @Test
    fun `GIVEN no channel silenced WHEN a row open settings arrives THEN opens nothing and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.GRANTED)
            val tapped = NotificationSettingsUiEvent.OnChannelSettingsClicked(NotificationType.BILLS)

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the bills channel silenced WHEN the payments row open settings arrives THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(systemBlocked = setOf(NotificationType.BILLS))
            val tapped = NotificationSettingsUiEvent.OnChannelSettingsClicked(NotificationType.PAYMENTS)

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN denied and the bills channel silenced WHEN its open settings arrives THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                permission = NotificationPermission.DENIED,
                systemBlocked = setOf(NotificationType.BILLS),
            )
            val tapped = NotificationSettingsUiEvent.OnChannelSettingsClicked(NotificationType.BILLS)

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN refused once and the bills channel silenced WHEN its open settings arrives THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                permission = NotificationPermission.REFUSED_ONCE,
                systemBlocked = setOf(NotificationType.BILLS),
            )
            val tapped = NotificationSettingsUiEvent.OnChannelSettingsClicked(NotificationType.BILLS)

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN no permission read and the bills channel silenced WHEN its open settings arrives THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                permission = null,
                systemBlocked = setOf(NotificationType.BILLS),
            )
            val tapped = NotificationSettingsUiEvent.OnChannelSettingsClicked(NotificationType.BILLS)

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN quiet delivery WHEN the screen opens THEN shows the quiet notice with the Push toggles enabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = setOf(NotificationType.BILLS, NotificationType.PEOPLE_AND_GROUPS),
                permission = NotificationPermission.PROVISIONAL,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsQuietNotice)
            assertFalse(state.showsPermissionDenied)
            assertFalse(state.canAskAgain)
            assertTrue(state.arePushTogglesEnabled)
            assertEquals(
                listOf(NotificationType.BILLS, NotificationType.PEOPLE_AND_GROUPS),
                state.types.filter { state.isEnabled(it, NotificationChannel.PUSH) },
            )
        }

    @Test
    fun `GIVEN quiet delivery WHEN turn on is tapped THEN asks for the full prompt`() = runTest(testDispatcher) {
        // Given
        prepareScenario(permission = NotificationPermission.PROVISIONAL)
        val tapped = NotificationSettingsUiEvent.OnQuietTurnOnClicked

        // When
        viewModel.onEvent(tapped)

        // Then
        assertEquals(listOf<NotificationSettingsUiAction>(NotificationSettingsUiAction.RequestPermission), actions)
        assertTrue(preferences.updates.isEmpty())
    }

    @Test
    fun `GIVEN quiet delivery WHEN turn on is tapped THEN sends the tap once with no params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.PROVISIONAL)
            val tapped = NotificationSettingsUiEvent.OnQuietTurnOnClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertEquals(listOf("notification_quiet_turn_on_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the permission granted WHEN a late quiet turn on arrives THEN asks nothing and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.GRANTED)
            val tapped = NotificationSettingsUiEvent.OnQuietTurnOnClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the permission denied WHEN a late quiet turn on arrives THEN asks nothing and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.DENIED)
            val tapped = NotificationSettingsUiEvent.OnQuietTurnOnClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the permission never asked WHEN a quiet turn on arrives THEN asks nothing and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.NOT_DETERMINED)
            val tapped = NotificationSettingsUiEvent.OnQuietTurnOnClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the permission refused once WHEN a quiet turn on arrives THEN asks nothing and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.REFUSED_ONCE)
            val tapped = NotificationSettingsUiEvent.OnQuietTurnOnClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the permission not read yet WHEN a quiet turn on arrives THEN asks nothing and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = null)
            val tapped = NotificationSettingsUiEvent.OnQuietTurnOnClicked

            // When
            viewModel.onEvent(tapped)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN quiet delivery WHEN a notice is turned on THEN saves it and asks for the full prompt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.PROVISIONAL)
            val toggled = pushOn(NotificationType.PEOPLE_AND_GROUPS)

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(
                listOf(Triple(NotificationType.PEOPLE_AND_GROUPS, NotificationChannel.PUSH, true)),
                preferences.updates,
            )
            assertEquals(listOf<NotificationSettingsUiAction>(NotificationSettingsUiAction.RequestPermission), actions)
            assertEquals(listOf("notification_toggled"), trackedEvents.map { (name, _) -> name })
        }

    @Test
    fun `GIVEN quiet delivery WHEN a notice is turned off THEN saves it and asks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(permission = NotificationPermission.PROVISIONAL)
        val toggled = NotificationSettingsUiEvent.OnToggled(
            NotificationType.BILLS,
            NotificationChannel.PUSH,
            isEnabled = false,
        )

        // When
        viewModel.onEvent(toggled)

        // Then
        assertEquals(listOf(Triple(NotificationType.BILLS, NotificationChannel.PUSH, false)), preferences.updates)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN quiet delivery WHEN an In app notice is turned on THEN saves it and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                permission = NotificationPermission.PROVISIONAL,
                savedInApp = emptySet(),
            )
            val toggled = NotificationSettingsUiEvent.OnToggled(
                NotificationType.COMMENTS,
                NotificationChannel.IN_APP,
                isEnabled = true,
            )

            // When
            viewModel.onEvent(toggled)

            // Then
            assertEquals(
                listOf(Triple(NotificationType.COMMENTS, NotificationChannel.IN_APP, true)),
                preferences.updates,
            )
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN a notice turned on while never asked WHEN the system answers granted THEN sends the answer granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.NOT_DETERMINED)
            viewModel.onEvent(pushOn(NotificationType.PEOPLE_AND_GROUPS))
            val answer = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.GRANTED)

            // When
            viewModel.onEvent(answer)

            // Then
            assertEquals(listOf(answered(isGranted = true)), answeredEvents())
        }

    @Test
    fun `GIVEN a notice turned on while never asked WHEN the system answers denied THEN sends not granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.NOT_DETERMINED)
            viewModel.onEvent(pushOn(NotificationType.PEOPLE_AND_GROUPS))
            val answer = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.DENIED)

            // When
            viewModel.onEvent(answer)

            // Then
            assertEquals(listOf(answered(isGranted = false)), answeredEvents())
        }

    @Test
    fun `GIVEN a notice turned on while never asked WHEN the dialog is dismissed THEN sends the answer not granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.NOT_DETERMINED)
            viewModel.onEvent(pushOn(NotificationType.PEOPLE_AND_GROUPS))
            val answer = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.NOT_DETERMINED)

            // When
            viewModel.onEvent(answer)

            // Then
            assertEquals(listOf(answered(isGranted = false)), answeredEvents())
        }

    @Test
    fun `GIVEN turn on tapped after a first refusal WHEN the system answers granted THEN sends the answer granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.REFUSED_ONCE)
            viewModel.onEvent(NotificationSettingsUiEvent.OnAskAgainClicked)
            val answer = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.GRANTED)

            // When
            viewModel.onEvent(answer)

            // Then
            assertEquals(listOf(answered(isGranted = true)), answeredEvents())
        }

    @Test
    fun `GIVEN turn on tapped after a first refusal WHEN the system answers denied THEN sends not granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.REFUSED_ONCE)
            viewModel.onEvent(NotificationSettingsUiEvent.OnAskAgainClicked)
            val answer = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.DENIED)

            // When
            viewModel.onEvent(answer)

            // Then
            assertEquals(listOf(answered(isGranted = false)), answeredEvents())
        }

    @Test
    fun `GIVEN turn on tapped after a first refusal WHEN it is refused once again THEN sends the answer not granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.REFUSED_ONCE)
            viewModel.onEvent(NotificationSettingsUiEvent.OnAskAgainClicked)
            val answer = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.REFUSED_ONCE)

            // When
            viewModel.onEvent(answer)

            // Then
            assertEquals(listOf(answered(isGranted = false)), answeredEvents())
        }

    @Test
    fun `GIVEN quiet turn on tapped WHEN Apple answers granted THEN sends the answer granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.PROVISIONAL)
            viewModel.onEvent(NotificationSettingsUiEvent.OnQuietTurnOnClicked)
            val answer = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.GRANTED)

            // When
            viewModel.onEvent(answer)

            // Then
            assertEquals(listOf(answered(isGranted = true)), answeredEvents())
        }

    @Test
    fun `GIVEN quiet turn on tapped WHEN Apple keeps delivering quietly THEN sends the answer not granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.PROVISIONAL)
            viewModel.onEvent(NotificationSettingsUiEvent.OnQuietTurnOnClicked)
            val answer = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.PROVISIONAL)

            // When
            viewModel.onEvent(answer)

            // Then
            assertEquals(listOf(answered(isGranted = false)), answeredEvents())
        }

    @Test
    fun `GIVEN a notice turned on during quiet delivery WHEN Apple answers granted THEN sends the answer granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.PROVISIONAL)
            viewModel.onEvent(pushOn(NotificationType.PEOPLE_AND_GROUPS))
            val answer = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.GRANTED)

            // When
            viewModel.onEvent(answer)

            // Then
            assertEquals(listOf(answered(isGranted = true)), answeredEvents())
        }

    @Test
    fun `GIVEN the answer already sent WHEN the permission is checked again THEN sends nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.NOT_DETERMINED)
            viewModel.onEvent(pushOn(NotificationType.PEOPLE_AND_GROUPS))
            viewModel.onEvent(NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.GRANTED))
            val checkedAgain = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.GRANTED)

            // When
            viewModel.onEvent(checkedAgain)

            // Then
            assertEquals(listOf(answered(isGranted = true)), answeredEvents())
        }

    @Test
    fun `GIVEN two notices turned on before the answer WHEN the system answers THEN sends the answer once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.NOT_DETERMINED)
            viewModel.onEvent(pushOn(NotificationType.PEOPLE_AND_GROUPS))
            viewModel.onEvent(pushOn(NotificationType.PEOPLE_AND_GROUPS))
            val answer = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.DENIED)

            // When
            viewModel.onEvent(answer)

            // Then
            assertEquals(listOf(answered(isGranted = false)), answeredEvents())
        }

    @Test
    fun `GIVEN open settings tapped while denied WHEN the permission comes back granted THEN sends no answer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.DENIED)
            viewModel.onEvent(NotificationSettingsUiEvent.OnOpenSettingsClicked)
            val checked = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.GRANTED)

            // When
            viewModel.onEvent(checked)

            // Then
            assertTrue(answeredEvents().isEmpty())
        }

    @Test
    fun `GIVEN a notice turned on while granted WHEN the permission is checked THEN sends no answer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = NotificationPermission.GRANTED)
            viewModel.onEvent(pushOn(NotificationType.PEOPLE_AND_GROUPS))
            val checked = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.GRANTED)

            // When
            viewModel.onEvent(checked)

            // Then
            assertTrue(answeredEvents().isEmpty())
        }

    @Test
    fun `GIVEN the screen opening WHEN the first permission check arrives THEN sends no answer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(permission = null)
            val checked = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.GRANTED)

            // When
            viewModel.onEvent(checked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a first ask answered WHEN a second ask is answered THEN sends both answers`() = runTest(testDispatcher) {
        // Given
        prepareScenario(permission = NotificationPermission.NOT_DETERMINED)
        viewModel.onEvent(pushOn(NotificationType.PEOPLE_AND_GROUPS))
        viewModel.onEvent(NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.NOT_DETERMINED))
        viewModel.onEvent(pushOn(NotificationType.PEOPLE_AND_GROUPS))
        val answer = NotificationSettingsUiEvent.OnPermissionChecked(NotificationPermission.GRANTED)

        // When
        viewModel.onEvent(answer)

        // Then
        assertEquals(listOf(answered(isGranted = false), answered(isGranted = true)), answeredEvents())
    }

    private fun pushOn(type: NotificationType): NotificationSettingsUiEvent.OnToggled =
        NotificationSettingsUiEvent.OnToggled(type, NotificationChannel.PUSH, isEnabled = true)

    private fun answered(isGranted: Boolean): Pair<String, Map<String, Any>> =
        "notification_permission_answered" to mapOf("source" to "notification_settings", "is_granted" to isGranted)

    private fun answeredEvents(): List<Pair<String, Map<String, Any>>> =
        trackedEvents.filter { (name, _) -> name == "notification_permission_answered" }

    private fun TestScope.prepareScenario(
        saved: Set<NotificationType> = NotificationPreferences.DEFAULT.push,
        savedInApp: Set<NotificationType> = NotificationPreferences.DEFAULT.inApp,
        permission: NotificationPermission? = NotificationPermission.GRANTED,
        insideProfile: Boolean = false,
        firstDownload: FirstDownloadProgress? = null,
        systemBlocked: Set<NotificationType> = emptySet(),
    ) {
        this@NotificationSettingsViewModelTest.firstDownload = MutableStateFlow(firstDownload)
        preferences =
            FakeNotificationPreferencesRepository(
                initialPreferences = NotificationPreferences(
                    push = saved,
                    inApp = savedInApp,
                ),
            )
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.Profile)
            if (!insideProfile) it.navigate(Route.NotificationSettings)
        }
        viewModel = NotificationSettingsViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            navigator = navigator,
            updateNotificationPreference = UpdateNotificationPreferenceUseCase(preferences),
            markActivityTypeRead = MarkActivityTypeRead { type ->
                markedReadTypes += type
                Result.success(Unit)
            },
            observeNotificationPreferences = ObserveNotificationPreferencesUseCase(preferences),
            observeFirstDownload = ObserveFirstDownload { this@NotificationSettingsViewModelTest.firstDownload },
        )
        permission?.let { viewModel.onEvent(NotificationSettingsUiEvent.OnPermissionChecked(it)) }
        // Why: only Android reports its silenced channels; iOS and the web never send it.
        if (systemBlocked.isNotEmpty()) {
            viewModel.onEvent(NotificationSettingsUiEvent.OnSystemBlockedTypesChecked(systemBlocked))
        }
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
