package com.quare.blitzsplit.feature.profilesetup

import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.account.domain.usecase.impl.GetSessionUserUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.PrepareProfilePhotoUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.SaveProfileUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ValidateDisplayNameUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.provider.image.AvatarImageEncoder
import com.quare.blitzsplit.core.provider.image.UnsupportedImageException
import com.quare.blitzsplit.feature.profilesetup.presentation.model.NameError
import com.quare.blitzsplit.feature.profilesetup.presentation.model.ProfileSetupUiEvent
import com.quare.blitzsplit.feature.profilesetup.presentation.viewmodel.ProfileSetupViewModel
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileSetupViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var profiles: FakeProfileRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: ProfileSetupViewModel
    private lateinit var heldPhotos: MutableList<CompletableDeferred<Unit>>

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
        prepareScenario(sessionUser = SessionUser("u", AccountProvider.GOOGLE, "Pierre Vieira", "https://photo", null))

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
        assertNull(state.nameError)
    }

    @Test
    fun `GIVEN one letter WHEN typing THEN shows the too short error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged("P"))

        // Then
        assertEquals(NameError.TOO_SHORT, viewModel.uiState.value.nameError)
        assertFalse(viewModel.uiState.value.canContinue)
        assertEquals("P", viewModel.uiState.value.avatarInitial)
    }

    @Test
    fun `GIVEN a long paste WHEN typing THEN keeps it whole with the too long error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged("a".repeat(60)))

        // Then
        val state = viewModel.uiState.value
        assertEquals(60, state.name.length)
        assertEquals(NameError.TOO_LONG, state.nameError)
        assertFalse(state.canContinue)
    }

    @Test
    fun `GIVEN a name too long WHEN shortened to 40 characters THEN hides the error and allows continuing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged("a".repeat(41)))

            // When
            viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged("a".repeat(40)))

            // Then
            assertNull(viewModel.uiState.value.nameError)
            assertTrue(viewModel.uiState.value.canContinue)
        }

    @Test
    fun `GIVEN a Google name over 40 characters WHEN the screen opens THEN prefills it whole with the error`() =
        runTest(testDispatcher) {
            // Given
            val longName = "Pierre Vieira de Souza Albuquerque Cavalcanti"
            prepareScenario(sessionUser = SessionUser("u", AccountProvider.GOOGLE, longName, null, null))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(longName, state.name)
            assertEquals(NameError.TOO_LONG, state.nameError)
            assertFalse(state.canContinue)
        }

    @Test
    fun `GIVEN a one letter Google name WHEN the screen opens THEN prefills it with the too short error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sessionUser = SessionUser("u", AccountProvider.GOOGLE, "P", null, null))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("P", state.name)
            assertEquals(NameError.TOO_SHORT, state.nameError)
            assertFalse(state.canContinue)
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

    @Test
    fun `GIVEN an Apple session with a photo WHEN the screen opens THEN shows that photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sessionUser = appleUser)

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(ProfilePhoto.Remote(APPLE_PHOTO_URL), state.photo)
        assertEquals(APPLE_PHOTO_URL, state.photoUrl)
    }

    @Test
    fun `GIVEN a stored picked photo and a Google session WHEN the screen opens THEN the stored photo wins`() =
        runTest(testDispatcher) {
            // Given
            val stored = UserProfile("Pierre", AvatarColor.SAND, ProfilePhoto.Pending(jpegOf(photoSource)))
            prepareScenario(
                sessionUser = googleUser,
                storedProfile = stored,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(ProfilePhoto.Pending(jpegOf(photoSource)), state.photo)
            assertEquals("Pierre", state.name)
            assertEquals(AvatarColor.SAND, state.avatarColor)
        }

    @Test
    fun `GIVEN a stored uploaded photo and a Google session WHEN the screen opens THEN the uploaded photo wins`() =
        runTest(testDispatcher) {
            // Given
            val stored = UserProfile("Pierre", AvatarColor.SAND, ProfilePhoto.Remote(UPLOADED_PHOTO_URL))
            prepareScenario(
                sessionUser = googleUser,
                storedProfile = stored,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(ProfilePhoto.Remote(UPLOADED_PHOTO_URL), state.photo)
        }

    @Test
    fun `GIVEN a stored profile without photo and Google WHEN the screen opens THEN shows the Google photo`() =
        runTest(testDispatcher) {
            // Given
            val stored = UserProfile("Pierre", AvatarColor.SAND, photo = null)
            prepareScenario(
                sessionUser = googleUser,
                storedProfile = stored,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
        }

    @Test
    fun `GIVEN no account WHEN the screen opens THEN shows no photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.photo)
        assertNull(state.photoUrl)
        assertFalse(state.isPreparingPhoto)
        assertFalse(state.showsPhotoError)
    }

    @Test
    fun `GIVEN an anonymous session WHEN the screen opens THEN shows no photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            sessionUser = SessionUser(
                "anonymous",
                socialProvider = null,
                displayName = null,
                photoUrl = null,
                email = null,
            ),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.photo)
    }

    @Test
    fun `GIVEN a photo picked before the session arrives WHEN the session arrives THEN keeps the picked photo`() =
        runTest(testDispatcher) {
            // Given
            val sessionArrival = CompletableDeferred<Unit>()
            prepareScenario(
                sessionUser = googleUser,
                sessionArrival = sessionArrival,
            )
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(photoSource))

            // When
            sessionArrival.complete(Unit)

            // Then
            assertEquals(ProfilePhoto.Pending(jpegOf(photoSource)), viewModel.uiState.value.photo)
            assertEquals("Pierre Vieira", viewModel.uiState.value.name)
        }

    @Test
    fun `GIVEN the setup screen WHEN picking a photo THEN shows the prepared JPEG before any upload`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sessionUser = googleUser)

            // When
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(photoSource))

            // Then
            val state = viewModel.uiState.value
            assertEquals(ProfilePhoto.Pending(jpegOf(photoSource)), state.photo)
            assertEquals("data:image/jpeg;base64,/9j/AQID", state.photoUrl)
            assertFalse(state.isPreparingPhoto)
            assertFalse(state.showsPhotoError)
        }

    @Test
    fun `GIVEN a Google photo WHEN a pick is still being prepared THEN shows it preparing over the Google photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sessionUser = googleUser,
                holdsPhotos = true,
            )

            // When
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(photoSource))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isPreparingPhoto)
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
        }

    @Test
    fun `GIVEN a Google photo WHEN picking a file that is not an image THEN shows the error and keeps it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sessionUser = googleUser)

            // When
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(notAnImage))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertFalse(state.isPreparingPhoto)
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
            assertEquals(GOOGLE_PHOTO_URL, state.photoUrl)
        }

    @Test
    fun `GIVEN an earlier pick WHEN picking a photo too large THEN shows the error and keeps the earlier one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(photoSource))
            val tooLarge = ByteArray(30 * 1024 * 1024 + 1)

            // When
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(tooLarge))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertFalse(state.isPreparingPhoto)
            assertEquals(ProfilePhoto.Pending(jpegOf(photoSource)), state.photo)
        }

    @Test
    fun `GIVEN the photo error WHEN picking another photo THEN hides the error while preparing it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsPhotos = true)
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(notAnImage))
            heldPhotos[0].complete(Unit)

            // When
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(photoSource))

            // Then
            assertFalse(viewModel.uiState.value.showsPhotoError)
            assertTrue(viewModel.uiState.value.isPreparingPhoto)
        }

    @Test
    fun `GIVEN two picks being prepared WHEN the older one finishes after the newer THEN keeps showing the newer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsPhotos = true)
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(otherPhotoSource))
            heldPhotos[1].complete(Unit)

            // When
            heldPhotos[0].complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertEquals(ProfilePhoto.Pending(jpegOf(otherPhotoSource)), state.photo)
            assertFalse(state.isPreparingPhoto)
        }

    @Test
    fun `GIVEN two picks being prepared WHEN the older finishes first THEN keeps preparing the newer one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsPhotos = true)
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(otherPhotoSource))

            // When
            heldPhotos[0].complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photo)
            assertTrue(state.isPreparingPhoto)
        }

    @Test
    fun `GIVEN a pick still being prepared WHEN continuing THEN waits for it before saving`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsPhotos = true)
            viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged("Pierre"))
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(ProfileSetupUiEvent.OnContinueClicked)

            // Then
            assertTrue(profiles.savedProfiles.isEmpty())
            assertTrue(viewModel.uiState.value.isSaving)
            assertEquals(listOf(Route.Welcome, Route.ProfileSetup), navigator.backStack.value)
        }

    @Test
    fun `GIVEN continue tapped during a pick WHEN the photo is ready THEN saves it and opens the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsPhotos = true)
            viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged("Pierre"))
            viewModel.onEvent(ProfileSetupUiEvent.OnAvatarColorSelected(AvatarColor.SAND))
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(ProfileSetupUiEvent.OnContinueClicked)

            // When
            heldPhotos[0].complete(Unit)

            // Then
            val expected = UserProfile("Pierre", AvatarColor.SAND, ProfilePhoto.Pending(jpegOf(photoSource)))
            assertEquals(listOf(expected), profiles.savedProfiles)
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN continue tapped during a pick WHEN the pick fails THEN stays on the screen with the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sessionUser = googleUser,
                holdsPhotos = true,
            )
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(notAnImage))
            viewModel.onEvent(ProfileSetupUiEvent.OnContinueClicked)

            // When
            heldPhotos[0].complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertFalse(state.isSaving)
            assertTrue(profiles.savedProfiles.isEmpty())
            assertEquals(listOf(Route.Welcome, Route.ProfileSetup), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a Google photo and a failed pick WHEN continuing THEN saves the Google photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sessionUser = googleUser)
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(notAnImage))

            // When
            viewModel.onEvent(ProfileSetupUiEvent.OnContinueClicked)

            // Then
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), profiles.savedProfiles.single().photo)
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a Google photo WHEN the picked photo is unreadable THEN shows the error and keeps it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sessionUser = googleUser)

            // When
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoUnreadable)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
        }

    @Test
    fun `GIVEN a prepared photo WHEN continuing THEN saves the profile with it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged("Pierre"))
        viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(photoSource))

        // When
        viewModel.onEvent(ProfileSetupUiEvent.OnContinueClicked)

        // Then
        val expected = UserProfile("Pierre", AvatarColor.default, ProfilePhoto.Pending(jpegOf(photoSource)))
        assertEquals(listOf(expected), profiles.savedProfiles)
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the Google photo untouched WHEN continuing THEN saves the profile with the Google photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sessionUser = googleUser)

            // When
            viewModel.onEvent(ProfileSetupUiEvent.OnContinueClicked)

            // Then
            val expected = UserProfile("Pierre Vieira", AvatarColor.default, ProfilePhoto.Remote(GOOGLE_PHOTO_URL))
            assertEquals(listOf(expected), profiles.savedProfiles)
        }

    @Test
    fun `GIVEN an invalid name and a picked photo WHEN continuing THEN saves nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged("P"))
        viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(photoSource))

        // When
        viewModel.onEvent(ProfileSetupUiEvent.OnContinueClicked)

        // Then
        assertTrue(profiles.savedProfiles.isEmpty())
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(listOf(Route.Welcome, Route.ProfileSetup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN continue tapped twice during a pick WHEN the photo is ready THEN saves the profile once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsPhotos = true)
            viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged("Pierre"))
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(ProfileSetupUiEvent.OnContinueClicked)
            viewModel.onEvent(ProfileSetupUiEvent.OnContinueClicked)

            // When
            heldPhotos[0].complete(Unit)

            // Then
            assertEquals(1, profiles.savedProfiles.size)
        }

    @Test
    fun `GIVEN continue tapped during a pick WHEN picking another photo THEN ignores it and saves the first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsPhotos = true)
            viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged("Pierre"))
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(ProfileSetupUiEvent.OnContinueClicked)

            // When
            viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(otherPhotoSource))
            heldPhotos.forEach { it.complete(Unit) }

            // Then
            val expected = UserProfile("Pierre", AvatarColor.default, ProfilePhoto.Pending(jpegOf(photoSource)))
            assertEquals(listOf(expected), profiles.savedProfiles)
        }

    private val googleUser = SessionUser("u", AccountProvider.GOOGLE, "Pierre Vieira", GOOGLE_PHOTO_URL, null)
    private val appleUser = SessionUser("u", AccountProvider.APPLE, "Pierre Vieira", APPLE_PHOTO_URL, null)
    private val photoSource = byteArrayOf(1, 2, 3)
    private val otherPhotoSource = byteArrayOf(4, 5, 6)
    private val notAnImage = "not an image".encodeToByteArray()

    // Why: a JPEG header in front of the source, so the state shows the encoder's output, not what was picked.
    private fun jpegOf(source: ByteArray): ByteArray = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + source

    private fun prepareScenario(
        sessionUser: SessionUser? = null,
        storedProfile: UserProfile? = null,
        holdsPhotos: Boolean = false,
        sessionArrival: CompletableDeferred<Unit>? = null,
    ) {
        profiles = FakeProfileRepository(initialProfile = storedProfile)
        navigator = BackStackNavigator(startRoute = Route.Welcome).also { it.navigate(Route.ProfileSetup) }
        heldPhotos = mutableListOf()
        val account = FakeAccountRepository().also { it.sessionUser = sessionUser }
        val getSessionUser = GetSessionUserUseCase(account)
        viewModel = ProfileSetupViewModel(
            observeProfile = ObserveProfileUseCase(profiles),
            getSessionUser = {
                sessionArrival?.await()
                getSessionUser()
            },
            validateDisplayName = ValidateDisplayNameUseCase(),
            prepareProfilePhoto = PrepareProfilePhotoUseCase(
                encodeAvatar = AvatarImageEncoder { source ->
                    if (holdsPhotos) CompletableDeferred<Unit>().also(heldPhotos::add).await()
                    if (source.contentEquals(notAnImage)) throw UnsupportedImageException()
                    jpegOf(source)
                },
            ),
            saveProfile = SaveProfileUseCase(profiles),
            navigator = navigator,
        )
    }

    private companion object {
        const val GOOGLE_PHOTO_URL = "https://lh3.googleusercontent.com/a/pierre"
        const val APPLE_PHOTO_URL = "https://apple/pierre"
        const val UPLOADED_PHOTO_URL = "https://supabase/storage/avatars/pierre.jpg"
    }
}
