package com.quare.blitzsplit.feature.watchpairing

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.watchpairing.presentation.viewmodel.WatchRootViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatchRootViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var profiles: MutableSharedFlow<UserProfile?>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: WatchRootViewModel
    private var syncStarts = 0
    private var screenViewStarts = 0

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        profiles = MutableSharedFlow(replay = 1)
        navigator = BackStackNavigator(startRoute = Route.Welcome)
        viewModel = WatchRootViewModel(
            navigator = navigator,
            observeProfile = { profiles },
            startScreenViews = { screenViewStarts++ },
            startSync = { syncStarts++ },
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the profile not read yet WHEN the watch app starts THEN starts the sync and is not ready`() =
        runTest(testDispatcher) {
            // When
            val isReady = viewModel.isReady.value

            // Then
            assertFalse(isReady)
            assertEquals(1, syncStarts)
            assertEquals(0, screenViewStarts)
            assertEquals(listOf<Route>(Route.Welcome), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a paired watch WHEN its profile is read THEN opens the balances and gets ready`() =
        runTest(testDispatcher) {
            // When
            profiles.emit(pierre)

            // Then
            assertEquals(listOf<Route>(Route.WatchBalances), navigator.backStack.value)
            assertTrue(viewModel.isReady.value)
            assertEquals(1, screenViewStarts)
            assertEquals(1, syncStarts)
        }

    @Test
    fun `GIVEN a watch never paired WHEN its profile is read THEN opens the connect screen and gets ready`() =
        runTest(testDispatcher) {
            // When
            profiles.emit(null)

            // Then
            assertEquals(listOf<Route>(Route.WatchConnect), navigator.backStack.value)
            assertTrue(viewModel.isReady.value)
            assertEquals(1, screenViewStarts)
        }

    @Test
    fun `GIVEN a paired watch WHEN a sign out erases its profile THEN opens the connect screen alone`() =
        runTest(testDispatcher) {
            // Given
            profiles.emit(pierre)
            navigator.navigate(
                Route.WatchGroup(
                    groupId = "trip",
                    currency = GroupCurrency.BRL,
                ),
            )

            // When
            profiles.emit(null)

            // Then
            assertEquals(listOf<Route>(Route.WatchConnect), navigator.backStack.value)
            assertEquals(1, screenViewStarts)
            assertEquals(1, syncStarts)
        }

    @Test
    fun `GIVEN the connect screen WHEN the profile arrives after pairing THEN does not navigate by itself`() =
        runTest(testDispatcher) {
            // Given
            profiles.emit(null)

            // When
            profiles.emit(pierre)

            // Then
            assertEquals(listOf<Route>(Route.WatchConnect), navigator.backStack.value)
            assertEquals(1, screenViewStarts)
        }

    @Test
    fun `GIVEN a watch paired again WHEN a second sign out erases its profile THEN opens the connect screen`() =
        runTest(testDispatcher) {
            // Given
            profiles.emit(pierre)
            profiles.emit(null)
            profiles.emit(pierre)
            navigator.replaceAll(Route.WatchBalances)

            // When
            profiles.emit(null)

            // Then
            assertEquals(listOf<Route>(Route.WatchConnect), navigator.backStack.value)
        }

    private val pierre = UserProfile(
        displayName = "Pierre",
        avatarColor = AvatarColor.PEACH,
        photo = null,
    )
}
