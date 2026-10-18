package com.quare.blitzsplit.feature.profilesetup

import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.account.domain.usecase.impl.GetSessionUserUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.SaveProfileUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ValidateDisplayNameUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.feature.profilesetup.presentation.model.ProfileSetupUiEvent
import com.quare.blitzsplit.feature.profilesetup.presentation.viewmodel.ProfileSetupViewModel
import kotlinx.coroutines.Dispatchers
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

class ProfileSetupViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var profiles: FakeProfileRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: ProfileSetupViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a Google session WHEN the screen opens THEN prefills its name and photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sessionUser = SessionUser("u", AccountProvider.GOOGLE, "Pierre Vieira", "https://photo"))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals("Pierre Vieira", state.name)
        assertEquals("https://photo", state.photoUrl)
        assertTrue(state.canContinue)
    }

    @Test
    fun `GIVEN no account WHEN the screen opens THEN starts empty and blocked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals("", state.name)
        assertFalse(state.canContinue)
        assertFalse(state.showsNameTooShort)
    }

    @Test
    fun `GIVEN one letter WHEN typing THEN shows the too short error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged("P"))

        // Then
        assertTrue(viewModel.uiState.value.showsNameTooShort)
        assertFalse(viewModel.uiState.value.canContinue)
        assertEquals("P", viewModel.uiState.value.avatarInitial)
    }

    @Test
    fun `GIVEN a long paste WHEN typing THEN keeps the first 40 characters`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged("a".repeat(60)))

        // Then
        assertEquals(40, viewModel.uiState.value.name.length)
    }

    @Test
    fun `GIVEN a valid name and color WHEN continuing THEN saves the profile and opens the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged("Pierre"))
            viewModel.onEvent(ProfileSetupUiEvent.OnAvatarColorSelected(AvatarColor.SAND))

            // When
            viewModel.onEvent(ProfileSetupUiEvent.OnContinueClicked)

            // Then
            assertEquals(listOf(UserProfile("Pierre", AvatarColor.SAND, null)), profiles.savedProfiles)
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN an invalid name WHEN continuing THEN saves nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged("P"))

        // When
        viewModel.onEvent(ProfileSetupUiEvent.OnContinueClicked)

        // Then
        assertTrue(profiles.savedProfiles.isEmpty())
    }

    @Test
    fun `GIVEN the setup screen WHEN going back THEN pops it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileSetupUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Welcome), navigator.backStack.value)
    }

    private fun prepareScenario(sessionUser: SessionUser? = null) {
        profiles = FakeProfileRepository()
        navigator = BackStackNavigator(startRoute = Route.Welcome).also { it.navigate(Route.ProfileSetup) }
        val account = FakeAccountRepository().also { it.sessionUser = sessionUser }
        viewModel = ProfileSetupViewModel(
            observeProfile = ObserveProfileUseCase(profiles),
            getSessionUser = GetSessionUserUseCase(account),
            validateDisplayName = ValidateDisplayNameUseCase(),
            saveProfile = SaveProfileUseCase(profiles),
            navigator = navigator,
        )
    }
}
