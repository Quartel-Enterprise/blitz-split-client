package com.quare.blitzsplit.feature.editprofile

import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.account.domain.usecase.GetSessionUser
import com.quare.blitzsplit.core.account.domain.usecase.impl.GetSessionUserUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.model.DisplayNameRules
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.PrepareProfilePhotoUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ValidateDisplayNameUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.provider.image.AvatarImageEncoder
import com.quare.blitzsplit.core.provider.image.UnsupportedImageException
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileNameError
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.EditProfileViewModel
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

class EditProfileViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var profiles: FakeProfileRepository
    private lateinit var account: FakeAccountRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: EditProfileViewModel
    private lateinit var heldPhotos: MutableList<CompletableDeferred<Unit>>
    private lateinit var heldSave: CompletableDeferred<Unit>
    private lateinit var heldSession: CompletableDeferred<Unit>
    private var sessionReads = 0

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a saved profile and a Google account WHEN the editor opens THEN fills the form and names Google`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Connected("u", AccountProvider.GOOGLE))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("Pierre", state.name)
            assertEquals(AvatarColor.SAND, state.avatarColor)
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
            assertTrue(state.showsAccountNotice)
            assertTrue(state.isNameValid)
            assertNull(state.nameError)
            assertFalse(state.hasChanges)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN an Apple account WHEN the editor opens THEN names Apple`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Connected("u", AccountProvider.APPLE))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(AccountProvider.APPLE, state.accountProvider)
        assertTrue(state.showsAccountNotice)
    }

    @Test
    fun `GIVEN a local only profile WHEN the editor opens THEN names no account`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.accountProvider)
        assertFalse(state.showsAccountNotice)
        assertEquals("Pierre", state.name)
    }

    @Test
    fun `GIVEN an anonymous account WHEN the editor opens THEN names no account`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID))

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.accountProvider)
        assertFalse(state.showsAccountNotice)
    }

    @Test
    fun `GIVEN an anonymous account WHEN Google gets connected THEN names Google`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID))
        val connected = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)

        // When
        account.status = connected

        // Then
        assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.accountProvider)
    }

    @Test
    fun `GIVEN no saved profile WHEN the editor opens THEN the form is empty and cannot be saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedProfile = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("", state.name)
            assertNull(state.photo)
            assertNull(state.nameError)
            assertFalse(state.isNameValid)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN the editor WHEN typing a name too long THEN keeps it whole with the too long error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val tooLong = "a".repeat(DisplayNameRules.MAX_LENGTH + 1)

            // When
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged(tooLong))

            // Then
            val state = viewModel.uiState.value
            assertEquals(tooLong, state.name)
            assertEquals(EditProfileNameError.TOO_LONG, state.nameError)
            assertFalse(state.isNameValid)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN the editor WHEN typing one letter THEN shows the too short error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged("P"))

        // Then
        val state = viewModel.uiState.value
        assertEquals(EditProfileNameError.TOO_SHORT, state.nameError)
        assertFalse(state.isNameValid)
        assertFalse(state.canSave)
    }

    @Test
    fun `GIVEN the editor WHEN clearing the name THEN shows no error and cannot be saved`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged(""))

        // Then
        val state = viewModel.uiState.value
        assertNull(state.nameError)
        assertFalse(state.isNameValid)
        assertFalse(state.canSave)
    }

    @Test
    fun `GIVEN the editor WHEN typing only spaces THEN shows no error and cannot be saved`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged("   "))

        // Then
        val state = viewModel.uiState.value
        assertEquals("   ", state.name)
        assertNull(state.nameError)
        assertFalse(state.isNameValid)
        assertFalse(state.canSave)
    }

    @Test
    fun `GIVEN the editor WHEN typing another valid name THEN it has changes and can be saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))

            // Then
            val state = viewModel.uiState.value
            assertNull(state.nameError)
            assertTrue(state.isNameValid)
            assertTrue(state.hasChanges)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN the editor WHEN typing the saved name between spaces THEN it has no changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged("  Pierre  "))

            // Then
            assertFalse(viewModel.uiState.value.hasChanges)
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN an edited name WHEN typing the saved name back THEN it has no changes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))

        // When
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre"))

        // Then
        assertFalse(viewModel.uiState.value.hasChanges)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN the editor WHEN choosing another color THEN it has changes and can be saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditProfileUiEvent.OnAvatarColorSelected(AvatarColor.PEACH))

            // Then
            val state = viewModel.uiState.value
            assertEquals(AvatarColor.PEACH, state.avatarColor)
            assertTrue(state.hasChanges)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN another color chosen WHEN choosing the saved color again THEN it has no changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditProfileUiEvent.OnAvatarColorSelected(AvatarColor.PEACH))

            // When
            viewModel.onEvent(EditProfileUiEvent.OnAvatarColorSelected(AvatarColor.SAND))

            // Then
            assertFalse(viewModel.uiState.value.hasChanges)
        }

    @Test
    fun `GIVEN the editor WHEN picking a photo THEN shows the prepared JPEG as a change`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))

        // Then
        val state = viewModel.uiState.value
        assertEquals(ProfilePhoto.Pending(jpegOf(photoSource)), state.photo)
        assertEquals("data:image/jpeg;base64,/9j/AQID", state.photoUrl)
        assertTrue(state.hasPhoto)
        assertFalse(state.isPreparingPhoto)
        assertFalse(state.showsPhotoError)
        assertTrue(state.hasChanges)
        assertTrue(state.canSave)
    }

    @Test
    fun `GIVEN an edited name WHEN a pick is still being prepared THEN keeps the saved photo and cannot be saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsPhotos = true)
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))

            // When
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isPreparingPhoto)
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
            assertTrue(state.hasChanges)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN two picks being prepared WHEN the older one finishes after the newer THEN keeps showing the newer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsPhotos = true)
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(otherPhotoSource))
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
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(otherPhotoSource))

            // When
            heldPhotos[0].complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
            assertTrue(state.isPreparingPhoto)
        }

    @Test
    fun `GIVEN a saved photo WHEN picking a file that is not an image THEN shows the error and keeps it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(notAnImage))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertFalse(state.isPreparingPhoto)
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
            assertFalse(state.hasChanges)
        }

    @Test
    fun `GIVEN an earlier pick WHEN picking a photo too large THEN shows the error and keeps the earlier one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))
            val tooLarge = ByteArray(30 * 1024 * 1024 + 1)

            // When
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(tooLarge))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertFalse(state.isPreparingPhoto)
            assertEquals(ProfilePhoto.Pending(jpegOf(photoSource)), state.photo)
        }

    @Test
    fun `GIVEN a saved photo WHEN the picked photo is unreadable THEN shows the error and keeps it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditProfileUiEvent.OnPhotoUnreadable)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
        }

    @Test
    fun `GIVEN the photo error WHEN picking another photo THEN hides the error while preparing it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsPhotos = true)
            viewModel.onEvent(EditProfileUiEvent.OnPhotoUnreadable)

            // When
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))

            // Then
            assertFalse(viewModel.uiState.value.showsPhotoError)
            assertTrue(viewModel.uiState.value.isPreparingPhoto)
        }

    @Test
    fun `GIVEN a save in progress WHEN picking a photo THEN ignores the pick`() = runTest(testDispatcher) {
        // Given
        prepareScenario(holdsSave = true)
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))
        viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)

        // When
        viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))

        // Then
        val state = viewModel.uiState.value
        assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
        assertFalse(state.isPreparingPhoto)
    }

    @Test
    fun `GIVEN a pick ignored during a save WHEN the save finishes THEN saves the photo shown before`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsSave = true)
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))
            viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))

            // When
            heldSave.complete(Unit)

            // Then
            val expected = UserProfile("Pierre Vieira", AvatarColor.SAND, ProfilePhoto.Remote(GOOGLE_PHOTO_URL))
            assertEquals(listOf(expected), profiles.savedProfiles)
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), viewModel.uiState.value.photo)
        }

    @Test
    fun `GIVEN a connected Google account WHEN the editor opens THEN keeps its picture to offer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = pierreWithUpload,
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GOOGLE_PHOTO_URL, state.accountPhotoUrl)
            assertTrue(state.showsUseAccountPhoto)
        }

    @Test
    fun `GIVEN a local only account WHEN the editor opens THEN offers no account photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = AccountStatus.LocalOnly,
            sessionUser = googleUser,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.accountPhotoUrl)
        assertFalse(state.showsUseAccountPhoto)
    }

    @Test
    fun `GIVEN a Google account without a picture WHEN the editor opens THEN offers no account photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser.copy(photoUrl = null),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.accountPhotoUrl)
            assertFalse(state.showsUseAccountPhoto)
        }

    @Test
    fun `GIVEN an uploaded photo WHEN choosing the account photo THEN shows it as a change and hides the offer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = pierreWithUpload,
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
            )

            // When
            viewModel.onEvent(EditProfileUiEvent.OnAccountPhotoSelected)

            // Then
            val state = viewModel.uiState.value
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
            assertEquals(GOOGLE_PHOTO_URL, state.photoUrl)
            assertTrue(state.hasChanges)
            assertTrue(state.canSave)
            assertFalse(state.showsUseAccountPhoto)
        }

    @Test
    fun `GIVEN a removed photo WHEN choosing the account photo THEN brings it back as the saved one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
            )
            viewModel.onEvent(EditProfileUiEvent.OnPhotoRemoved)

            // When
            viewModel.onEvent(EditProfileUiEvent.OnAccountPhotoSelected)

            // Then
            val state = viewModel.uiState.value
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
            assertFalse(state.hasChanges)
        }

    @Test
    fun `GIVEN a pick being prepared WHEN choosing the account photo THEN the account photo replaces it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = pierreWithUpload,
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
                holdsPhotos = true,
            )
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(EditProfileUiEvent.OnAccountPhotoSelected)
            heldPhotos.forEach { it.complete(Unit) }

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isPreparingPhoto)
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
        }

    @Test
    fun `GIVEN no account picture WHEN choosing the account photo THEN keeps the photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        viewModel.onEvent(EditProfileUiEvent.OnAccountPhotoSelected)

        // Then
        val state = viewModel.uiState.value
        assertEquals(pierre.photo, state.photo)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `GIVEN a save in progress WHEN choosing the account photo THEN ignores it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedProfile = pierreWithUpload,
            status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
            sessionUser = googleUser,
            holdsSave = true,
        )
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))
        viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)

        // When
        viewModel.onEvent(EditProfileUiEvent.OnAccountPhotoSelected)

        // Then
        assertEquals(ProfilePhoto.Remote(UPLOADED_PHOTO_URL), viewModel.uiState.value.photo)
        heldSave.complete(Unit)
    }

    @Test
    fun `GIVEN a saved photo WHEN removing it THEN clears it as a change`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditProfileUiEvent.OnPhotoRemoved)

        // Then
        val state = viewModel.uiState.value
        assertNull(state.photo)
        assertNull(state.photoUrl)
        assertFalse(state.hasPhoto)
        assertTrue(state.hasChanges)
        assertTrue(state.canSave)
    }

    @Test
    fun `GIVEN no saved photo and a picked one WHEN removing it THEN it has no changes`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedProfile = pierreWithoutPhoto)
        viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))

        // When
        viewModel.onEvent(EditProfileUiEvent.OnPhotoRemoved)

        // Then
        val state = viewModel.uiState.value
        assertNull(state.photo)
        assertFalse(state.hasChanges)
        assertFalse(state.canSave)
    }

    @Test
    fun `GIVEN a pick being prepared WHEN removing the photo THEN stops preparing and clears it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsPhotos = true)
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(EditProfileUiEvent.OnPhotoRemoved)

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isPreparingPhoto)
            assertNull(state.photo)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a pick removed while being prepared WHEN the pick finishes THEN the photo stays removed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsPhotos = true)
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(EditProfileUiEvent.OnPhotoRemoved)

            // When
            heldPhotos[0].complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photo)
            assertFalse(state.isPreparingPhoto)
        }

    @Test
    fun `GIVEN the photo error WHEN removing the photo THEN hides the error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditProfileUiEvent.OnPhotoUnreadable)

        // When
        viewModel.onEvent(EditProfileUiEvent.OnPhotoRemoved)

        // Then
        assertFalse(viewModel.uiState.value.showsPhotoError)
    }

    @Test
    fun `GIVEN a save in progress WHEN removing the photo THEN keeps it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(holdsSave = true)
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))
        viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)

        // When
        viewModel.onEvent(EditProfileUiEvent.OnPhotoRemoved)

        // Then
        assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), viewModel.uiState.value.photo)
    }

    @Test
    fun `GIVEN a new name color and photo WHEN saving THEN saves the trimmed profile once`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged("  Pierre Vieira  "))
        viewModel.onEvent(EditProfileUiEvent.OnAvatarColorSelected(AvatarColor.PEACH))
        viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))

        // When
        viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)

        // Then
        val expected = UserProfile("Pierre Vieira", AvatarColor.PEACH, ProfilePhoto.Pending(jpegOf(photoSource)))
        assertEquals(listOf(expected), profiles.savedProfiles)
    }

    @Test
    fun `GIVEN the editor pushed on a phone WHEN saving THEN goes back to the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario(pushed = true)
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))

        // When
        viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the editor inline in the profile WHEN saving THEN stays with the saved values`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(pushed = false)
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged("  Pierre Vieira  "))
            viewModel.onEvent(EditProfileUiEvent.OnAvatarColorSelected(AvatarColor.PEACH))

            // When
            viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
            assertEquals("Pierre Vieira", state.name)
            assertEquals(AvatarColor.PEACH, state.avatarColor)
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
            assertFalse(state.hasChanges)
            assertFalse(state.isSaving)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN the editor inline and saved once WHEN editing again THEN compares with the new saved values`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(pushed = false)
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))
            viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre"))

            // When
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))

            // Then
            assertFalse(viewModel.uiState.value.hasChanges)
        }

    @Test
    fun `GIVEN no changes WHEN saving THEN saves nothing and stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario(pushed = true)

        // When
        viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)

        // Then
        assertTrue(profiles.savedProfiles.isEmpty())
        assertEquals(listOf(Route.Groups, Route.Profile, Route.EditProfile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN an invalid name WHEN saving THEN saves nothing and stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario(pushed = true)
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged("P"))

        // When
        viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)

        // Then
        assertTrue(profiles.savedProfiles.isEmpty())
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(listOf(Route.Groups, Route.Profile, Route.EditProfile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a pick being prepared WHEN saving THEN saves nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(holdsPhotos = true)
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))
        viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))

        // When
        viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)

        // Then
        assertTrue(profiles.savedProfiles.isEmpty())
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `GIVEN a slow save WHEN saving THEN shows it saving and blocks another save`() = runTest(testDispatcher) {
        // Given
        prepareScenario(holdsSave = true)
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))

        // When
        viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)

        // Then
        assertTrue(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN save tapped twice during a slow save WHEN the save finishes THEN saves the profile once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsSave = true)
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))
            viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)
            viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)

            // When
            heldSave.complete(Unit)

            // Then
            assertEquals(1, profiles.savedProfiles.size)
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN edits to every field WHEN discarding THEN puts back the saved values`() = runTest(testDispatcher) {
        // Given
        prepareScenario(pushed = false)
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged("P"))
        viewModel.onEvent(EditProfileUiEvent.OnAvatarColorSelected(AvatarColor.PEACH))
        viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))

        // When
        viewModel.onEvent(EditProfileUiEvent.OnDiscardClicked)

        // Then
        val state = viewModel.uiState.value
        assertEquals("Pierre", state.name)
        assertEquals(AvatarColor.SAND, state.avatarColor)
        assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
        assertNull(state.nameError)
        assertTrue(state.isNameValid)
        assertFalse(state.hasChanges)
        assertTrue(profiles.savedProfiles.isEmpty())
    }

    @Test
    fun `GIVEN the photo error WHEN discarding THEN hides the error`() = runTest(testDispatcher) {
        // Given
        prepareScenario(pushed = false)
        viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(notAnImage))

        // When
        viewModel.onEvent(EditProfileUiEvent.OnDiscardClicked)

        // Then
        assertFalse(viewModel.uiState.value.showsPhotoError)
    }

    @Test
    fun `GIVEN a pick being prepared WHEN discarding THEN stops preparing it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            pushed = false,
            holdsPhotos = true,
        )
        viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))

        // When
        viewModel.onEvent(EditProfileUiEvent.OnDiscardClicked)

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isPreparingPhoto)
        assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
    }

    @Test
    fun `GIVEN a pick discarded while being prepared WHEN the pick finishes THEN keeps the saved photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                pushed = false,
                holdsPhotos = true,
            )
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(EditProfileUiEvent.OnDiscardClicked)

            // When
            heldPhotos[0].complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
            assertFalse(state.hasChanges)
        }

    @Test
    fun `GIVEN the editor pushed on a phone WHEN going back THEN returns to the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario(pushed = true)

        // When
        viewModel.onEvent(EditProfileUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN an untouched form with a pending photo WHEN sync uploads it THEN shows the uploaded photo`() =
        runTest(testDispatcher) {
            // Given
            val pending = ProfilePhoto.Pending(jpegOf(photoSource))
            prepareScenario(storedProfile = pierre.copy(photo = pending))
            val uploaded = pierre.copy(photo = ProfilePhoto.Remote(UPLOADED_PHOTO_URL))

            // When
            profiles.profile.value = uploaded

            // Then
            val state = viewModel.uiState.value
            assertEquals(ProfilePhoto.Remote(UPLOADED_PHOTO_URL), state.photo)
            assertFalse(state.hasChanges)
        }

    @Test
    fun `GIVEN an untouched form WHEN the Google name arrives THEN shows that name`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        profiles.profile.value = pierre.copy(displayName = "Pierre Vieira")

        // Then
        assertEquals("Pierre Vieira", viewModel.uiState.value.name)
        assertFalse(viewModel.uiState.value.hasChanges)
    }

    @Test
    fun `GIVEN no saved profile yet WHEN the profile arrives THEN fills the form`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedProfile = null)

        // When
        profiles.profile.value = pierre

        // Then
        val state = viewModel.uiState.value
        assertEquals("Pierre", state.name)
        assertEquals(AvatarColor.SAND, state.avatarColor)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `GIVEN an edited name WHEN the saved color changes THEN keeps the edited name with changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))

            // When
            profiles.profile.value = pierre.copy(avatarColor = AvatarColor.GREEN)

            // Then
            val state = viewModel.uiState.value
            assertEquals("Pierre Vieira", state.name)
            assertTrue(state.hasChanges)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN an edited name WHEN the saved profile gets that same name THEN it has no changes left`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))

            // When
            profiles.profile.value = pierre.copy(displayName = "Pierre Vieira")

            // Then
            val state = viewModel.uiState.value
            assertEquals("Pierre Vieira", state.name)
            assertFalse(state.hasChanges)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN a saved profile WHEN the profile becomes empty THEN keeps the form`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        profiles.profile.value = null

        // Then
        val state = viewModel.uiState.value
        assertEquals("Pierre", state.name)
        assertEquals(AvatarColor.SAND, state.avatarColor)
        assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `GIVEN an edited name and a pending photo WHEN sync uploads the photo THEN saves the uploaded photo`() =
        runTest(testDispatcher) {
            // Given
            val pending = ProfilePhoto.Pending(jpegOf(photoSource))
            prepareScenario(
                storedProfile = pierre.copy(photo = pending),
                pushed = false,
            )
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))
            profiles.profile.value = pierre.copy(photo = ProfilePhoto.Remote(UPLOADED_PHOTO_URL))

            // When
            viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)

            // Then
            val expected = pierre.copy(
                displayName = "Pierre Vieira",
                photo = ProfilePhoto.Remote(UPLOADED_PHOTO_URL),
            )
            assertEquals(listOf(expected), profiles.savedProfiles)
        }

    @Test
    fun `GIVEN a save under way inline WHEN typing another name THEN keeps it as a change once saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                pushed = false,
                holdsSave = true,
            )
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre Vieira"))
            viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)
            viewModel.onEvent(EditProfileUiEvent.OnNameChanged("Pierre V."))

            // When
            heldSave.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertEquals("Pierre V.", state.name)
            assertTrue(state.hasChanges)
        }

    @Test
    fun `GIVEN a pick being prepared WHEN the next pick is unreadable THEN stops preparing and keeps the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsPhotos = true)
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(EditProfileUiEvent.OnPhotoUnreadable)

            // When
            heldPhotos.forEach { it.complete(Unit) }

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isPreparingPhoto)
            assertTrue(state.showsPhotoError)
            assertEquals(pierre.photo, state.photo)
        }

    @Test
    fun `GIVEN a slow session read WHEN the editor opens THEN names Google before the picture arrives`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
                holdsSession = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
            assertTrue(state.showsAccountNotice)
            assertNull(state.accountPhotoUrl)
            assertFalse(state.showsUseAccountPhoto)
            heldSession.complete(Unit)
        }

    @Test
    fun `GIVEN a slow session read WHEN the session is read THEN offers the picture`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedProfile = pierreWithUpload,
            status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
            sessionUser = googleUser,
            holdsSession = true,
        )

        // When
        heldSession.complete(Unit)

        // Then
        val state = viewModel.uiState.value
        assertEquals(AccountProvider.GOOGLE, state.accountProvider)
        assertEquals(GOOGLE_PHOTO_URL, state.accountPhotoUrl)
        assertTrue(state.showsUseAccountPhoto)
    }

    @Test
    fun `GIVEN a connected account with a picture WHEN it gets disconnected THEN forgets both`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = pierreWithUpload,
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
            )

            // When
            account.status = AccountStatus.LocalOnly

            // Then
            val state = viewModel.uiState.value
            assertNull(state.accountProvider)
            assertFalse(state.showsAccountNotice)
            assertNull(state.accountPhotoUrl)
            assertFalse(state.showsUseAccountPhoto)
        }

    @Test
    fun `GIVEN a connected account with a picture WHEN it turns anonymous THEN forgets both`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = pierreWithUpload,
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
            )

            // When
            account.status = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.accountProvider)
            assertNull(state.accountPhotoUrl)
        }

    @Test
    fun `GIVEN a local only editor open WHEN Google gets connected THEN names Google and offers its picture`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = pierreWithUpload,
                status = AccountStatus.LocalOnly,
                sessionUser = googleUser,
            )

            // When
            account.status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)

            // Then
            val state = viewModel.uiState.value
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
            assertEquals(GOOGLE_PHOTO_URL, state.accountPhotoUrl)
            assertTrue(state.showsUseAccountPhoto)
        }

    @Test
    fun `GIVEN a connected account with a picture WHEN only its email changes THEN keeps the picture and reads once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = pierreWithUpload,
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
            )

            // When
            account.email = "pierre@gmail.com"

            // Then
            val state = viewModel.uiState.value
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
            assertEquals(GOOGLE_PHOTO_URL, state.accountPhotoUrl)
            assertEquals(1, sessionReads)
        }

    @Test
    fun `GIVEN a connected account with a picture WHEN only the recovery prompt changes THEN keeps the picture`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = pierreWithUpload,
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
            )

            // When
            account.recoveryCodePrompt = RecoveryCodePrompt.DUE

            // Then
            assertEquals(GOOGLE_PHOTO_URL, viewModel.uiState.value.accountPhotoUrl)
            assertEquals(1, sessionReads)
        }

    @Test
    fun `GIVEN a Google account with a picture WHEN Apple replaces it THEN names Apple with no picture`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
            )
            account.sessionUser = appleUser

            // When
            account.status = AccountStatus.Connected("u", AccountProvider.APPLE)

            // Then
            val state = viewModel.uiState.value
            assertEquals(AccountProvider.APPLE, state.accountProvider)
            assertNull(state.accountPhotoUrl)
            assertFalse(state.showsUseAccountPhoto)
        }

    @Test
    fun `GIVEN a slow session read WHEN the account disconnects meanwhile THEN keeps neither provider nor picture`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
                holdsSession = true,
            )
            account.status = AccountStatus.LocalOnly

            // When
            heldSession.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.accountProvider)
            assertNull(state.accountPhotoUrl)
        }

    @Test
    fun `GIVEN a slow session read WHEN the account disconnects THEN drops the notice without waiting for the read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
                holdsSession = true,
            )

            // When
            account.status = AccountStatus.LocalOnly

            // Then
            val state = viewModel.uiState.value
            assertNull(state.accountProvider)
            assertFalse(state.showsAccountNotice)
            heldSession.complete(Unit)
            assertNull(viewModel.uiState.value.accountPhotoUrl)
        }

    @Test
    fun `GIVEN a Google account with a picture WHEN another Google user replaces it THEN offers that user's picture`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = pierreWithUpload,
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
            )
            account.sessionUser = googleUser.copy(
                userId = "v",
                photoUrl = OTHER_GOOGLE_PHOTO_URL,
            )

            // When
            account.status = AccountStatus.Connected("v", AccountProvider.GOOGLE)

            // Then
            val state = viewModel.uiState.value
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
            assertEquals(OTHER_GOOGLE_PHOTO_URL, state.accountPhotoUrl)
        }

    @Test
    fun `GIVEN a connected account without a session user WHEN the editor opens THEN offers no account photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = null,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
            assertNull(state.accountPhotoUrl)
            assertFalse(state.showsUseAccountPhoto)
        }

    @Test
    fun `GIVEN the account picture is the saved photo WHEN the editor opens THEN does not offer it again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GOOGLE_PHOTO_URL, state.accountPhotoUrl)
            assertFalse(state.showsUseAccountPhoto)
        }

    @Test
    fun `GIVEN the photo error WHEN choosing the account photo THEN hides the error`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedProfile = pierreWithUpload,
            status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
            sessionUser = googleUser,
        )
        viewModel.onEvent(EditProfileUiEvent.OnPhotoUnreadable)

        // When
        viewModel.onEvent(EditProfileUiEvent.OnAccountPhotoSelected)

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showsPhotoError)
        assertEquals(ProfilePhoto.Remote(GOOGLE_PHOTO_URL), state.photo)
    }

    @Test
    fun `GIVEN the account photo chosen WHEN saving THEN saves its public url without preparing or uploading it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = pierreWithUpload,
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
                holdsPhotos = true,
            )
            viewModel.onEvent(EditProfileUiEvent.OnAccountPhotoSelected)

            // When
            viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)

            // Then
            val expected = pierreWithUpload.copy(photo = ProfilePhoto.Remote(GOOGLE_PHOTO_URL))
            assertEquals(listOf(expected), profiles.savedProfiles)
            assertTrue(heldPhotos.isEmpty())
            assertTrue(profiles.uploadedUserIds.isEmpty())
        }

    @Test
    fun `GIVEN the account photo chosen WHEN discarding THEN puts back the saved photo and offers the account one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = pierreWithUpload,
                status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
                sessionUser = googleUser,
            )
            viewModel.onEvent(EditProfileUiEvent.OnAccountPhotoSelected)

            // When
            viewModel.onEvent(EditProfileUiEvent.OnDiscardClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(ProfilePhoto.Remote(UPLOADED_PHOTO_URL), state.photo)
            assertFalse(state.hasChanges)
            assertTrue(state.showsUseAccountPhoto)
        }

    @Test
    fun `GIVEN a saved photo WHEN the editor opens THEN hides the avatar colors`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedProfile = pierreWithUpload)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showsAvatarColors)
    }

    @Test
    fun `GIVEN a saved photo WHEN removing it THEN brings the avatar colors back`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedProfile = pierreWithUpload)

        // When
        viewModel.onEvent(EditProfileUiEvent.OnPhotoRemoved)

        // Then
        assertTrue(viewModel.uiState.value.showsAvatarColors)
    }

    @Test
    fun `GIVEN no photo WHEN picking one THEN hides the avatar colors while it is prepared`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = pierreWithoutPhoto,
                holdsPhotos = true,
            )

            // When
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(photoSource))

            // Then
            assertFalse(viewModel.uiState.value.showsAvatarColors)
        }

    @Test
    fun `GIVEN no photo WHEN the picked file is not an image THEN shows the avatar colors again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedProfile = pierreWithoutPhoto)

            // When
            viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(notAnImage))

            // Then
            assertTrue(viewModel.uiState.value.showsAvatarColors)
        }

    @Test
    fun `GIVEN no photo WHEN the account photo is chosen THEN hides the avatar colors`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedProfile = pierreWithoutPhoto,
            status = AccountStatus.Connected("u", AccountProvider.GOOGLE),
            sessionUser = googleUser,
        )

        // When
        viewModel.onEvent(EditProfileUiEvent.OnAccountPhotoSelected)

        // Then
        assertFalse(viewModel.uiState.value.showsAvatarColors)
    }

    private val googleUser = SessionUser(
        userId = "u",
        socialProvider = AccountProvider.GOOGLE,
        displayName = "Pierre Vieira",
        photoUrl = GOOGLE_PHOTO_URL,
        email = null,
    )
    private val appleUser = googleUser.copy(
        socialProvider = AccountProvider.APPLE,
        photoUrl = null,
    )
    private val pierre = UserProfile("Pierre", AvatarColor.SAND, ProfilePhoto.Remote(GOOGLE_PHOTO_URL))
    private val pierreWithoutPhoto = pierre.copy(photo = null)
    private val pierreWithUpload = pierre.copy(photo = ProfilePhoto.Remote(UPLOADED_PHOTO_URL))
    private val photoSource = byteArrayOf(1, 2, 3)
    private val otherPhotoSource = byteArrayOf(4, 5, 6)
    private val notAnImage = "not an image".encodeToByteArray()

    // Why: a JPEG header in front of the source, so the state shows the encoder's output, not what was picked.
    private fun jpegOf(source: ByteArray): ByteArray = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + source

    private fun prepareScenario(
        storedProfile: UserProfile? = pierre,
        status: AccountStatus = AccountStatus.LocalOnly,
        pushed: Boolean = true,
        holdsPhotos: Boolean = false,
        holdsSave: Boolean = false,
        holdsSession: Boolean = false,
        sessionUser: SessionUser? = null,
    ) {
        profiles = FakeProfileRepository(initialProfile = storedProfile)
        account = FakeAccountRepository(initialStatus = status).also { it.sessionUser = sessionUser }
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.Profile)
            if (pushed) it.navigate(Route.EditProfile)
        }
        heldPhotos = mutableListOf()
        heldSave = CompletableDeferred()
        if (!holdsSave) heldSave.complete(Unit)
        heldSession = CompletableDeferred()
        if (!holdsSession) heldSession.complete(Unit)
        sessionReads = 0
        viewModel = EditProfileViewModel(
            validateDisplayName = ValidateDisplayNameUseCase(),
            prepareProfilePhoto = PrepareProfilePhotoUseCase(
                encodeAvatar = AvatarImageEncoder { source ->
                    if (holdsPhotos) CompletableDeferred<Unit>().also(heldPhotos::add).await()
                    if (source.contentEquals(notAnImage)) throw UnsupportedImageException()
                    jpegOf(source)
                },
            ),
            // Why: straight to the repository, so savedProfiles holds exactly what the editor handed over.
            saveProfile = { profile ->
                heldSave.await()
                profiles.saveProfile(profile)
            },
            navigator = navigator,
            observeProfile = ObserveProfileUseCase(profiles),
            getSessionUser = GetSessionUser {
                sessionReads++
                heldSession.await()
                GetSessionUserUseCase(account)()
            },
            observeAccount = ObserveAccountUseCase(account),
        )
    }

    private companion object {
        const val GOOGLE_PHOTO_URL = "https://lh3.googleusercontent.com/a/pierre"
        const val OTHER_GOOGLE_PHOTO_URL = "https://lh3.googleusercontent.com/a/ana"
        const val UPLOADED_PHOTO_URL = "https://supabase/storage/avatars/pierre.jpg"
    }
}
