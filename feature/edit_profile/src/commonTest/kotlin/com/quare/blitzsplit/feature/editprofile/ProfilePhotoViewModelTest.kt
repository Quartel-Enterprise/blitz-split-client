package com.quare.blitzsplit.feature.editprofile

import androidx.lifecycle.ViewModelStore
import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.account.domain.usecase.GetSessionUser
import com.quare.blitzsplit.core.account.domain.usecase.impl.GetSessionUserUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountPhotoUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.LoadCropPhotoUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.PrepareProfilePhotoUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.provider.image.AvatarImageEncoder
import com.quare.blitzsplit.core.provider.image.BillImage
import com.quare.blitzsplit.core.provider.image.BillImageEncoder
import com.quare.blitzsplit.core.provider.image.UnsupportedImageException
import com.quare.blitzsplit.feature.editprofile.presentation.model.ProfilePhotoUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.ProfilePhotoViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfilePhotoViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var profiles: FakeProfileRepository
    private lateinit var account: FakeAccountRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: ProfilePhotoViewModel
    private lateinit var heldLoad: CompletableDeferred<Unit>
    private lateinit var heldSave: CompletableDeferred<Unit>
    private lateinit var heldSession: CompletableDeferred<Unit>
    private var avatarEncodings = 0

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a saved profile WHEN the viewer opens THEN shows its name color and photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertEquals("Pierre", state.displayName)
        assertEquals(AvatarColor.SAND, state.avatarColor)
        assertEquals(ProfilePhoto.Remote(UPLOADED_PHOTO_URL), state.photo)
        assertTrue(state.hasPhoto)
        assertNull(state.photoCrop)
        assertFalse(state.isPreparingPhoto)
        assertFalse(state.showsPhotoError)
    }

    @Test
    fun `GIVEN a profile without a photo WHEN the viewer opens THEN shows the initial with nothing to remove`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedProfile = pierreWithoutPhoto)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertNull(state.photo)
            assertFalse(state.hasPhoto)
            assertEquals("P", state.avatarInitial)
            assertEquals(AvatarColor.SAND, state.avatarColor)
        }

    @Test
    fun `GIVEN the saved profile not read yet WHEN the viewer opens THEN is not loaded`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedProfile = null)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
    }

    @Test
    fun `GIVEN the saved profile not read yet WHEN it arrives THEN shows it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedProfile = null)

        // When
        profiles.profile.value = pierre

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isLoaded)
        assertEquals("Pierre", state.displayName)
        assertEquals(ProfilePhoto.Remote(UPLOADED_PHOTO_URL), state.photo)
    }

    @Test
    fun `GIVEN the saved profile not read yet WHEN removing the photo THEN saves nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedProfile = null)

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)

        // Then
        assertTrue(profiles.savedProfiles.isEmpty())
    }

    @Test
    fun `GIVEN the saved profile not read yet WHEN choosing the account photo THEN saves nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = null,
                status = googleStatus,
                sessionUser = googleUser,
            )

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)

            // Then
            assertTrue(profiles.savedProfiles.isEmpty())
            assertNull(viewModel.uiState.value.photo)
        }

    @Test
    fun `GIVEN the saved profile not read yet WHEN picking a photo THEN opens no editor`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedProfile = null)

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))

        // Then
        val state = viewModel.uiState.value
        assertNull(state.photoCrop)
        assertFalse(state.isPreparingPhoto)
    }

    @Test
    fun `GIVEN the saved profile not read yet WHEN the picked photo is unreadable THEN shows no error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedProfile = null)

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoUnreadable)

            // Then
            assertFalse(viewModel.uiState.value.showsPhotoError)
        }

    @Test
    fun `GIVEN the saved profile not read yet WHEN dismissing THEN goes back to the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedProfile = null)

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnDismissed)

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the viewer WHEN dismissing THEN goes back to the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnDismissed)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        assertTrue(profiles.savedProfiles.isEmpty())
    }

    @Test
    fun `GIVEN a connected Google account with another picture WHEN the viewer opens THEN offers the Google photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(AccountProvider.GOOGLE, state.accountPhotoProvider)
        }

    @Test
    fun `GIVEN a connected Apple account with a picture WHEN the viewer opens THEN offers the Apple photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("u", AccountProvider.APPLE),
                sessionUser = googleUser.copy(socialProvider = AccountProvider.APPLE),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(AccountProvider.APPLE, state.accountPhotoProvider)
        }

    @Test
    fun `GIVEN a local only profile WHEN the viewer opens THEN offers no account photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = AccountStatus.LocalOnly,
            sessionUser = googleUser,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.accountPhotoProvider)
    }

    @Test
    fun `GIVEN an anonymous account WHEN the viewer opens THEN offers no account photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID),
            sessionUser = googleUser,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.accountPhotoProvider)
    }

    @Test
    fun `GIVEN the Google picture is already the photo WHEN the viewer opens THEN does not offer it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = pierre.copy(photo = ProfilePhoto.Remote(GOOGLE_PHOTO_URL)),
                status = googleStatus,
                sessionUser = googleUser,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.accountPhotoProvider)
        }

    @Test
    fun `GIVEN a slow session read WHEN the viewer opens THEN offers the account photo only once it is read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
                holdsSession = true,
            )
            val before = viewModel.uiState.value.accountPhotoProvider

            // When
            heldSession.complete(Unit)

            // Then
            assertNull(before)
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.accountPhotoProvider)
        }

    @Test
    fun `GIVEN a Google account WHEN choosing its photo THEN saves its url keeping the name and color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
            )

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)

            // Then
            val expected = pierre.copy(photo = ProfilePhoto.Remote(GOOGLE_PHOTO_URL))
            assertEquals(listOf(expected), profiles.savedProfiles)
            assertEquals(0, avatarEncodings)
        }

    @Test
    fun `GIVEN a Google account WHEN choosing its photo THEN the viewer stays open showing it without offering it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
            )

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)

            // Then
            val state = viewModel.uiState.value
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
            assertEquals(GOOGLE_PHOTO_URL, state.photoUrl)
            assertNull(state.accountPhotoProvider)
            assertEquals(Route.ProfilePhoto, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN no account picture WHEN choosing the account photo THEN saves nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)

        // Then
        assertTrue(profiles.savedProfiles.isEmpty())
        assertEquals(ProfilePhoto.Remote(UPLOADED_PHOTO_URL), viewModel.uiState.value.photo)
    }

    @Test
    fun `GIVEN a slow save WHEN choosing the account photo THEN shows it at once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = googleStatus,
            sessionUser = googleUser,
            holdsSave = true,
        )

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)

        // Then
        assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), viewModel.uiState.value.photo)
        heldSave.complete(Unit)
    }

    @Test
    fun `GIVEN a photo WHEN removing it THEN saves the profile without a photo keeping the name and color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)

            // Then
            assertEquals(listOf(pierreWithoutPhoto), profiles.savedProfiles)
            val state = viewModel.uiState.value
            assertNull(state.photo)
            assertFalse(state.hasPhoto)
            assertEquals(Route.ProfilePhoto, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN the viewer WHEN picking a photo THEN frames it in Ajustar foto and saves nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))

            // Then
            val state = viewModel.uiState.value
            assertContentEquals(photoSource, state.photoCrop?.photo?.jpeg)
            assertFalse(state.isPreparingPhoto)
            assertEquals(ProfilePhoto.Remote(UPLOADED_PHOTO_URL), state.photo)
            assertTrue(profiles.savedProfiles.isEmpty())
        }

    @Test
    fun `GIVEN a slow photo read WHEN picking a photo THEN shows it preparing without the editor`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsLoad = true)

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isPreparingPhoto)
            assertNull(state.photoCrop)
            heldLoad.complete(Unit)
        }

    @Test
    fun `GIVEN Ajustar foto WHEN confirming it THEN saves the framed square keeping the name and color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            val expected = pierre.copy(photo = ProfilePhoto.Pending(jpegOf(photoSource)))
            assertEquals(listOf(expected), profiles.savedProfiles)
        }

    @Test
    fun `GIVEN a confirmed photo still being saved WHEN the viewer closes THEN the photo is still saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsSave = true)
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnDismissed)
            ViewModelStore().apply { put("viewer", viewModel) }.clear()
            heldSave.complete(Unit)

            // Then
            val expected = pierre.copy(photo = ProfilePhoto.Pending(jpegOf(photoSource)))
            assertEquals(listOf(expected), profiles.savedProfiles)
        }

    @Test
    fun `GIVEN Ajustar foto WHEN confirming it THEN the viewer stays open showing the new photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            val state = viewModel.uiState.value
            assertEquals(ProfilePhoto.Pending(jpegOf(photoSource)), state.photo)
            assertNull(state.photoCrop)
            assertFalse(state.isPreparingPhoto)
            assertFalse(state.showsPhotoError)
            assertEquals(Route.ProfilePhoto, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN Ajustar foto WHEN dismissing it THEN closes it keeping the photo and saving nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnDismissed))

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photoCrop)
            assertEquals(ProfilePhoto.Remote(UPLOADED_PHOTO_URL), state.photo)
            assertTrue(profiles.savedProfiles.isEmpty())
            assertEquals(Route.ProfilePhoto, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN the viewer WHEN the picked photo is unreadable THEN shows the error and saves nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoUnreadable)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertEquals(ProfilePhoto.Remote(UPLOADED_PHOTO_URL), state.photo)
            assertTrue(profiles.savedProfiles.isEmpty())
        }

    @Test
    fun `GIVEN the viewer WHEN picking a file that is not an image THEN shows the error and saves nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(notAnImage))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertFalse(state.isPreparingPhoto)
            assertNull(state.photoCrop)
            assertEquals(ProfilePhoto.Remote(UPLOADED_PHOTO_URL), state.photo)
            assertTrue(profiles.savedProfiles.isEmpty())
        }

    @Test
    fun `GIVEN preparing the square fails WHEN confirming Ajustar foto THEN shows the error and saves nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(preparingFails = true)
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertFalse(state.isPreparingPhoto)
            assertEquals(ProfilePhoto.Remote(UPLOADED_PHOTO_URL), state.photo)
            assertTrue(profiles.savedProfiles.isEmpty())
        }

    @Test
    fun `GIVEN the photo error WHEN removing the photo THEN hides the error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoUnreadable)

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)

        // Then
        assertFalse(viewModel.uiState.value.showsPhotoError)
    }

    @Test
    fun `GIVEN the photo error WHEN choosing the account photo THEN hides the error`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = googleStatus,
            sessionUser = googleUser,
        )
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoUnreadable)

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)

        // Then
        assertFalse(viewModel.uiState.value.showsPhotoError)
    }

    @Test
    fun `GIVEN the photo error WHEN a new photo is framed and confirmed THEN hides the error and saves it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoUnreadable)
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            assertFalse(viewModel.uiState.value.showsPhotoError)
            assertEquals(1, profiles.savedProfiles.size)
        }

    @Test
    fun `GIVEN a pick still being read WHEN choosing the account photo THEN the account photo wins`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
                holdsLoad = true,
            )
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)

            // When
            heldLoad.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photoCrop)
            assertFalse(state.isPreparingPhoto)
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
            assertEquals(listOf(pierre.copy(photo = ProfilePhoto.Remote(GOOGLE_PHOTO_URL))), profiles.savedProfiles)
        }

    @Test
    fun `GIVEN Ajustar foto open WHEN choosing the account photo THEN closes the editor and saves the account photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
            )
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)

            // Then
            assertNull(viewModel.uiState.value.photoCrop)
            assertEquals(listOf(pierre.copy(photo = ProfilePhoto.Remote(GOOGLE_PHOTO_URL))), profiles.savedProfiles)
        }

    @Test
    fun `GIVEN Ajustar foto open WHEN removing the photo THEN closes the editor and saves no photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)

            // Then
            assertNull(viewModel.uiState.value.photoCrop)
            assertEquals(listOf(pierreWithoutPhoto), profiles.savedProfiles)
        }

    @Test
    fun `GIVEN a pick still being read WHEN removing the photo THEN the removal wins once the pick is read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsLoad = true)
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)

            // When
            heldLoad.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photoCrop)
            assertFalse(state.isPreparingPhoto)
            assertNull(state.photo)
            assertEquals(listOf(pierreWithoutPhoto), profiles.savedProfiles)
        }

    @Test
    fun `GIVEN a save in progress WHEN removing the photo THEN ignores it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = googleStatus,
            sessionUser = googleUser,
            holdsSave = true,
        )
        viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)

        // When
        heldSave.complete(Unit)

        // Then
        assertEquals(listOf(pierre.copy(photo = ProfilePhoto.Remote(GOOGLE_PHOTO_URL))), profiles.savedProfiles)
        assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), viewModel.uiState.value.photo)
    }

    @Test
    fun `GIVEN a save in progress WHEN choosing the account photo THEN ignores it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = googleStatus,
            sessionUser = googleUser,
            holdsSave = true,
        )
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)
        viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)

        // When
        heldSave.complete(Unit)

        // Then
        assertEquals(listOf(pierreWithoutPhoto), profiles.savedProfiles)
        assertNull(viewModel.uiState.value.photo)
    }

    @Test
    fun `GIVEN a save in progress WHEN picking and confirming a photo THEN saves nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsSave = true)
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // When
            heldSave.complete(Unit)

            // Then
            assertEquals(listOf(pierreWithoutPhoto), profiles.savedProfiles)
            assertEquals(0, avatarEncodings)
            assertNull(viewModel.uiState.value.photo)
        }

    @Test
    fun `GIVEN a save that finished WHEN removing the photo THEN saves again`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = googleStatus,
            sessionUser = googleUser,
        )
        viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)

        // Then
        val expected = listOf(pierre.copy(photo = ProfilePhoto.Remote(GOOGLE_PHOTO_URL)), pierreWithoutPhoto)
        assertEquals(expected, profiles.savedProfiles)
    }

    @Test
    fun `GIVEN a pending photo WHEN sync replaces it with its upload THEN shows the uploaded photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))
            val uploaded = pierre.copy(photo = ProfilePhoto.Remote(OTHER_UPLOADED_PHOTO_URL))

            // When
            profiles.profile.value = uploaded

            // Then
            assertEquals(ProfilePhoto.Remote(OTHER_UPLOADED_PHOTO_URL), viewModel.uiState.value.photo)
        }

    @Test
    fun `GIVEN the viewer WHEN another device renames the profile THEN shows the new name and color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            profiles.profile.value = renamed

            // Then
            val state = viewModel.uiState.value
            assertEquals("Ana", state.displayName)
            assertEquals("A", state.avatarInitial)
            assertEquals(renamed.avatarColor, state.avatarColor)
        }

    @Test
    fun `GIVEN another device renamed the profile WHEN removing the photo THEN keeps the new name and color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            profiles.profile.value = renamed

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)

            // Then
            assertEquals(listOf(renamed.copy(photo = null)), profiles.savedProfiles)
        }

    @Test
    fun `GIVEN another device renamed the profile WHEN confirming a framed photo THEN keeps the new name and color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            profiles.profile.value = renamed
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            val expected = renamed.copy(photo = ProfilePhoto.Pending(jpegOf(photoSource)))
            assertEquals(listOf(expected), profiles.savedProfiles)
        }

    @Test
    fun `GIVEN a Google account WHEN another Google user replaces it THEN offers and saves the new picture`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
            )
            account.sessionUser = googleUser.copy(
                userId = "v",
                photoUrl = OTHER_GOOGLE_PHOTO_URL,
            )
            account.status = AccountStatus.Connected("v", AccountProvider.GOOGLE)

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)

            // Then
            assertEquals(
                listOf(pierre.copy(photo = ProfilePhoto.Remote(OTHER_GOOGLE_PHOTO_URL))),
                profiles.savedProfiles,
            )
        }

    @Test
    fun `GIVEN a Google account WHEN it gets disconnected THEN stops offering its photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = googleStatus,
            sessionUser = googleUser,
        )

        // When
        account.status = AccountStatus.LocalOnly

        // Then
        assertNull(viewModel.uiState.value.accountPhotoProvider)
    }

    @Test
    fun `GIVEN a slow session read WHEN the account disconnects meanwhile THEN never offers the old picture`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
                holdsSession = true,
            )
            account.status = AccountStatus.LocalOnly

            // When
            heldSession.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.accountPhotoProvider)
            assertNull(state.accountPhotoUrl)
        }

    @Test
    fun `GIVEN the saved profile not read yet WHEN removing the photo THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedProfile = null)

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the saved profile not read yet WHEN a photo is picked THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedProfile = null)

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the saved profile not read yet WHEN choosing the account photo THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = null,
                status = googleStatus,
                sessionUser = googleUser,
            )

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the saved profile not read yet WHEN tapping Girar THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedProfile = null)

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnTurnClicked))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the saved profile not read yet WHEN dismissing THEN goes back and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedProfile = null)

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnDismissed)

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the viewer WHEN a photo is picked THEN sends the photo picked from the viewer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))

            // Then
            assertEquals(listOf("photo_picked" to mapOf<String, Any>("source" to SOURCE)), trackedEvents)
        }

    @Test
    fun `GIVEN the viewer WHEN the picked photo is unreadable THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoUnreadable)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Ajustar foto over the viewer WHEN tapping Girar THEN sends the turn from the viewer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnTurnClicked))

            // Then
            assertEquals(listOf("photo_crop_turned" to mapOf<String, Any>("source" to SOURCE)), trackedEvents)
        }

    @Test
    fun `GIVEN Ajustar foto over the viewer WHEN tapping Horizontal THEN sends the horizontal mirror`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnMirrorHorizontallyClicked))

            // Then
            assertEquals(
                listOf("photo_crop_mirrored" to mapOf<String, Any>("source" to SOURCE, "axis" to "horizontal")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Ajustar foto over the viewer WHEN tapping Vertical THEN sends the vertical mirror`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnMirrorVerticallyClicked))

            // Then
            assertEquals(
                listOf("photo_crop_mirrored" to mapOf<String, Any>("source" to SOURCE, "axis" to "vertical")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Ajustar foto over the viewer WHEN tapping Pronto THEN sends the crop confirmed from the viewer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            assertEquals(listOf("photo_crop_confirmed" to mapOf<String, Any>("source" to SOURCE)), trackedEvents)
        }

    @Test
    fun `GIVEN Ajustar foto over the viewer WHEN closing it THEN sends the crop dismissed from the viewer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnDismissed))

            // Then
            assertEquals(listOf("photo_crop_dismissed" to mapOf<String, Any>("source" to SOURCE)), trackedEvents)
        }

    @Test
    fun `GIVEN Ajustar foto over the viewer WHEN zooming with the slider THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(photoSource))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnZoomChanged(2f)))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a photo WHEN removing it THEN sends the removal from the viewer`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)

        // Then
        assertEquals(listOf("photo_removed" to mapOf<String, Any>("source" to SOURCE)), trackedEvents)
    }

    @Test
    fun `GIVEN a save in progress WHEN removing the photo again THEN sends nothing as the viewer drops it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsSave = true)
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)

            // Then
            assertTrue(trackedEvents.isEmpty())
            heldSave.complete(Unit)
        }

    @Test
    fun `GIVEN a Google account with another picture WHEN choosing its photo THEN sends it from the viewer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
            )

            // When
            viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)

            // Then
            assertEquals(listOf("account_photo_selected" to mapOf<String, Any>("source" to SOURCE)), trackedEvents)
        }

    @Test
    fun `GIVEN no account picture WHEN choosing the account photo THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the viewer WHEN dismissing THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfilePhotoUiEvent.OnDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    private val googleStatus = AccountStatus.Connected("u", AccountProvider.GOOGLE)
    private val googleUser = SessionUser(
        userId = "u",
        socialProvider = AccountProvider.GOOGLE,
        displayName = "Pierre Vieira",
        photoUrl = GOOGLE_PHOTO_URL,
        email = null,
    )
    private val pierre = UserProfile("Pierre", AvatarColor.SAND, ProfilePhoto.Remote(UPLOADED_PHOTO_URL))
    private val pierreWithoutPhoto = pierre.copy(photo = null)
    private val renamed = pierre.copy(
        displayName = "Ana",
        avatarColor = AvatarColor.LILAC,
    )
    private val photoSource = byteArrayOf(1, 2, 3)
    private val notAnImage = "not an image".encodeToByteArray()

    // Why: a JPEG header in front of the source, so the saved photo is the encoder's output, not what was picked.
    private fun jpegOf(source: ByteArray): ByteArray = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + source

    private fun prepareScenario(
        storedProfile: UserProfile? = pierre,
        status: AccountStatus = AccountStatus.LocalOnly,
        sessionUser: SessionUser? = null,
        holdsLoad: Boolean = false,
        holdsSave: Boolean = false,
        holdsSession: Boolean = false,
        preparingFails: Boolean = false,
    ) {
        profiles = FakeProfileRepository(initialProfile = storedProfile)
        account = FakeAccountRepository(initialStatus = status).also { it.sessionUser = sessionUser }
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.Profile)
            it.navigate(Route.ProfilePhoto)
        }
        heldLoad = CompletableDeferred()
        if (!holdsLoad) heldLoad.complete(Unit)
        heldSave = CompletableDeferred()
        if (!holdsSave) heldSave.complete(Unit)
        heldSession = CompletableDeferred()
        if (!holdsSession) heldSession.complete(Unit)
        avatarEncodings = 0
        viewModel = ProfilePhotoViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            // Why: straight to the repository, so savedProfiles holds exactly what the viewer handed over.
            saveProfile = { profile ->
                heldSave.await()
                profiles.saveProfile(profile)
            },
            navigator = navigator,
            loadCropPhoto = LoadCropPhotoUseCase(
                encodeImage = BillImageEncoder { source ->
                    heldLoad.await()
                    if (source.contentEquals(notAnImage)) throw UnsupportedImageException()
                    BillImage(
                        jpeg = source,
                        width = 1200,
                        height = 900,
                    )
                },
            ),
            prepareProfilePhoto = PrepareProfilePhotoUseCase(
                encodeAvatar = AvatarImageEncoder { source, _ ->
                    avatarEncodings++
                    if (preparingFails) throw UnsupportedImageException()
                    jpegOf(source)
                },
            ),
            observeProfile = ObserveProfileUseCase(profiles),
            observeAccountPhoto = ObserveAccountPhotoUseCase(
                observeAccount = ObserveAccountUseCase(account),
                getSessionUser = GetSessionUser {
                    heldSession.await()
                    GetSessionUserUseCase(account)()
                },
            ),
        )
    }

    private companion object {
        const val GOOGLE_PHOTO_URL = "https://lh3.googleusercontent.com/a/pierre"
        const val OTHER_GOOGLE_PHOTO_URL = "https://lh3.googleusercontent.com/a/ana"
        const val UPLOADED_PHOTO_URL = "https://supabase/storage/avatars/pierre.jpg"
        const val OTHER_UPLOADED_PHOTO_URL = "https://supabase/storage/avatars/pierre.jpg?v=2"
        const val SOURCE = "profile_photo"
    }
}
