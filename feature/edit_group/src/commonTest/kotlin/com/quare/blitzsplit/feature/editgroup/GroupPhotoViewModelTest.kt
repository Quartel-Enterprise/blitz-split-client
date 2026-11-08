package com.quare.blitzsplit.feature.editgroup

import androidx.lifecycle.ViewModelStore
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.UpdateGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.editgroup.presentation.model.GroupPhotoUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.GroupPhotoViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupPhotoViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: GroupPhotoViewModel
    private lateinit var unreadRoom: MutableSharedFlow<Group?>
    private lateinit var heldLoad: CompletableDeferred<Unit>
    private lateinit var heldPrepare: CompletableDeferred<Unit>
    private var preparations = 0

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a group with a photo WHEN the viewer opens THEN shows its name initials color and photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithPhoto)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals("Viagem Floripa", state.name)
            assertEquals("VF", state.initials)
            assertEquals(AvatarColor.GREEN, state.color)
            assertEquals(uploadedPhoto, state.photo)
            assertEquals(UPLOADED_PHOTO_URL, state.photoUrl)
            assertTrue(state.hasPhoto)
            assertNull(state.photoCrop)
            assertFalse(state.isPreparingPhoto)
            assertFalse(state.showsPhotoError)
        }

    @Test
    fun `GIVEN a group without a photo WHEN the viewer opens THEN shows the initials with nothing to remove`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertNull(state.photo)
            assertFalse(state.hasPhoto)
            assertEquals("VF", state.initials)
        }

    @Test
    fun `GIVEN the group not read yet WHEN the viewer opens THEN is not loaded`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertEquals(listOf(Route.Groups, groupDetail, groupPhoto), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the group not read yet WHEN it arrives THEN shows it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        unreadRoom.tryEmit(tripWithPhoto)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isLoaded)
        assertEquals("Viagem Floripa", state.name)
        assertEquals(uploadedPhoto, state.photo)
    }

    @Test
    fun `GIVEN the group not read yet WHEN removing the photo THEN saves and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isRoomRead = false)

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoRemoved)

            // Then
            assertTrue(groups.updatedGroupIds.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the group not read yet WHEN picking a photo THEN opens no editor and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isRoomRead = false)

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photoCrop)
            assertFalse(state.isPreparingPhoto)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the group not read yet WHEN the picked photo is unreadable THEN shows no error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isRoomRead = false)

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoUnreadable)

            // Then
            assertFalse(viewModel.uiState.value.showsPhotoError)
        }

    @Test
    fun `GIVEN the group not read yet WHEN dismissing THEN goes back to the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        viewModel.onEvent(GroupPhotoUiEvent.OnDismissed)

        // Then
        assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the viewer WHEN dismissing THEN goes back to the group and saves nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = tripWithPhoto)

        // When
        viewModel.onEvent(GroupPhotoUiEvent.OnDismissed)

        // Then
        assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        assertTrue(groups.updatedGroupIds.isEmpty())
    }

    @Test
    fun `GIVEN the viewer WHEN the backdrop and the close button both dismiss it THEN only the viewer closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnDismissed)
            viewModel.onEvent(GroupPhotoUiEvent.OnDismissed)

            // Then
            assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the viewer WHEN the group disappears THEN closes the viewer`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        groups.groups.value = emptyList()

        // Then
        assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the viewer WHEN another device renames the group THEN shows the new name initials and color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.value = listOf(
                trip.copy(
                    name = "Churrasco",
                    color = AvatarColor.LILAC,
                ),
            )

            // Then
            val state = viewModel.uiState.value
            assertEquals("Churrasco", state.name)
            assertEquals("C", state.initials)
            assertEquals(AvatarColor.LILAC, state.color)
        }

    @Test
    fun `GIVEN a picked photo WHEN reading it THEN opens Ajustar foto with it and saves nothing yet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

            // Then
            val state = viewModel.uiState.value
            assertContentEquals(photoSource, state.photoCrop?.photo?.jpeg)
            assertFalse(state.isPreparingPhoto)
            assertTrue(groups.updatedGroupIds.isEmpty())
        }

    @Test
    fun `GIVEN a photo being read WHEN it is not read yet THEN shows the spinner`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isLoadHeld = true)

        // When
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isPreparingPhoto)
        assertNull(state.photoCrop)
        heldLoad.complete(Unit)
    }

    @Test
    fun `GIVEN Ajustar foto open WHEN turning the photo THEN the editor shows the turn`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

        // When
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnTurnClicked))

        // Then
        assertEquals(
            1,
            viewModel.uiState.value.photoCrop
                ?.crop
                ?.quarterTurns,
        )
    }

    @Test
    fun `GIVEN Ajustar foto open WHEN dismissing it THEN closes the editor and saves nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnDismissed))

            // Then
            assertNull(viewModel.uiState.value.photoCrop)
            assertTrue(groups.updatedGroupIds.isEmpty())
            assertEquals(listOf(Route.Groups, groupDetail, groupPhoto), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Ajustar foto open WHEN confirming it THEN saves the group with the prepared photo at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
            assertEquals(
                trip.copy(
                    photo = preparedPhoto,
                    isPending = true,
                ),
                groups.groups.value.single(),
            )
        }

    @Test
    fun `GIVEN Ajustar foto open WHEN confirming it THEN the viewer stays open showing the new photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            val state = viewModel.uiState.value
            assertEquals(preparedPhoto, state.photo)
            assertNull(state.photoCrop)
            assertFalse(state.isPreparingPhoto)
            assertFalse(state.showsPhotoError)
            assertEquals(listOf(Route.Groups, groupDetail, groupPhoto), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a photo still being prepared WHEN confirming it THEN shows the spinner and saves nothing yet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isPrepareHeld = true)
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isPreparingPhoto)
            assertNull(state.photoCrop)
            assertTrue(groups.updatedGroupIds.isEmpty())
            heldPrepare.complete(Unit)
        }

    @Test
    fun `GIVEN a group renamed elsewhere WHEN confirming a framed photo THEN keeps its new name and color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.groups.value = listOf(
                trip.copy(
                    name = "Churrasco",
                    color = AvatarColor.LILAC,
                ),
            )
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            val expected = trip.copy(
                name = "Churrasco",
                color = AvatarColor.LILAC,
                photo = preparedPhoto,
                isPending = true,
            )
            assertEquals(expected, groups.groups.value.single())
        }

    @Test
    fun `GIVEN a slow save WHEN confirming a photo THEN shows it at once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isSaveHeld = true)
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

        // When
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

        // Then
        assertEquals(preparedPhoto, viewModel.uiState.value.photo)
        assertTrue(groups.updatedGroupIds.isEmpty())
        groups.updateGroupGate?.complete(Unit)
    }

    @Test
    fun `GIVEN a photo WHEN removing it THEN saves the group without a photo keeping everything else`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithPhoto)

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoRemoved)

            // Then
            assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
            assertEquals(trip.copy(isPending = true), groups.groups.value.single())
        }

    @Test
    fun `GIVEN a photo WHEN removing it THEN the viewer stays open showing the initials`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = tripWithPhoto)

        // When
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoRemoved)

        // Then
        val state = viewModel.uiState.value
        assertNull(state.photo)
        assertFalse(state.hasPhoto)
        assertEquals(listOf(Route.Groups, groupDetail, groupPhoto), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group without a photo WHEN removing the photo THEN saves and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoRemoved)

            // Then
            assertTrue(groups.updatedGroupIds.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Ajustar foto open WHEN removing the photo THEN closes the editor and saves no photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithPhoto)
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoRemoved)

            // Then
            assertNull(viewModel.uiState.value.photoCrop)
            assertNull(
                groups.groups.value
                    .single()
                    .photo,
            )
            assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
        }

    @Test
    fun `GIVEN a pick still being read WHEN removing the photo THEN the removal wins once the pick is read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = tripWithPhoto,
                isLoadHeld = true,
            )
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoRemoved)

            // When
            heldLoad.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photoCrop)
            assertFalse(state.isPreparingPhoto)
            assertNull(state.photo)
            assertNull(
                groups.groups.value
                    .single()
                    .photo,
            )
            assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
        }

    @Test
    fun `GIVEN a save in progress WHEN removing the photo again THEN ignores it and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = tripWithPhoto,
                isSaveHeld = true,
            )
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoRemoved)
            trackedEvents.clear()

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoRemoved)
            groups.updateGroupGate?.complete(Unit)

            // Then
            assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a save in progress WHEN picking and confirming another photo THEN saves nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = tripWithPhoto,
                isSaveHeld = true,
            )
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoRemoved)

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))
            groups.updateGroupGate?.complete(Unit)

            // Then
            assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
            assertEquals(0, preparations)
            assertNull(viewModel.uiState.value.photoCrop)
            assertNull(viewModel.uiState.value.photo)
            assertNull(
                groups.groups.value
                    .single()
                    .photo,
            )
        }

    @Test
    fun `GIVEN a save that finished WHEN removing the photo THEN saves again`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = tripWithPhoto)
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

        // When
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoRemoved)

        // Then
        assertEquals(listOf(GROUP_ID, GROUP_ID), groups.updatedGroupIds)
        assertNull(
            groups.groups.value
                .single()
                .photo,
        )
        assertNull(viewModel.uiState.value.photo)
    }

    @Test
    fun `GIVEN a confirmed photo still being saved WHEN the viewer closes THEN the photo is still saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isSaveHeld = true)
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnDismissed)
            ViewModelStore().apply { put("viewer", viewModel) }.clear()
            groups.updateGroupGate?.complete(Unit)

            // Then
            assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
            assertEquals(
                preparedPhoto,
                groups.groups.value
                    .single()
                    .photo,
            )
            assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the viewer WHEN the picked photo is unreadable THEN shows the error and saves nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithPhoto)

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoUnreadable)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertFalse(state.isPreparingPhoto)
            assertEquals(uploadedPhoto, state.photo)
            assertTrue(groups.updatedGroupIds.isEmpty())
        }

    @Test
    fun `GIVEN a photo the app cannot read WHEN picking it THEN shows the error and saves nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(loadError = IllegalArgumentException("not an image"))

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertFalse(state.isPreparingPhoto)
            assertNull(state.photoCrop)
            assertTrue(groups.updatedGroupIds.isEmpty())
        }

    @Test
    fun `GIVEN a framed photo that cannot be prepared WHEN confirming it THEN shows the error and saves nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(prepareError = IllegalStateException("cannot encode"))
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertFalse(state.isPreparingPhoto)
            assertNull(state.photo)
            assertTrue(groups.updatedGroupIds.isEmpty())
        }

    @Test
    fun `GIVEN an error shown WHEN picking a new photo THEN clears the error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoUnreadable)

        // When
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

        // Then
        assertFalse(viewModel.uiState.value.showsPhotoError)
        assertContentEquals(
            photoSource,
            viewModel.uiState.value.photoCrop
                ?.photo
                ?.jpeg,
        )
    }

    @Test
    fun `GIVEN a photo save that failed WHEN picking another photo THEN does not stay stuck saving`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateError = IllegalStateException("room failed"))
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))
            groups.updateGroupError = null

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            assertNotNull(
                groups.groups.value
                    .single()
                    .photo,
            )
        }

    @Test
    fun `GIVEN a save that fails WHEN removing the photo THEN the viewer still shows the saved photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = tripWithPhoto,
                updateError = IllegalStateException("room failed"),
            )

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoRemoved)

            // Then
            assertEquals(
                uploadedPhoto,
                groups.groups.value
                    .single()
                    .photo,
            )
            assertEquals(uploadedPhoto, viewModel.uiState.value.photo)
        }

    @Test
    fun `GIVEN the viewer WHEN a photo is picked THEN sends the photo picked from the group photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(photoSource))

            // Then
            assertEquals(listOf("photo_picked" to mapOf<String, Any>("source" to SOURCE)), trackedEvents)
        }

    @Test
    fun `GIVEN a photo WHEN removing it THEN sends the removal from the group photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = tripWithPhoto)

        // When
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoRemoved)

        // Then
        assertEquals(listOf("photo_removed" to mapOf<String, Any>("source" to SOURCE)), trackedEvents)
    }

    @Test
    fun `GIVEN the viewer WHEN dismissing or finding the photo unreadable THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupPhotoUiEvent.OnPhotoUnreadable)
            viewModel.onEvent(GroupPhotoUiEvent.OnDismissed)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    private val groupDetail = Route.GroupDetail(
        GROUP_ID,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
    )
    private val groupPhoto = Route.GroupPhoto(GROUP_ID)
    private val uploadedPhoto = ProfilePhoto.Remote(UPLOADED_PHOTO_URL)
    private val preparedPhoto = ProfilePhoto.Pending(byteArrayOf(9, 8, 7))
    private val photoSource = byteArrayOf(1, 2, 3)
    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = emptyList(),
        isPending = false,
        inviteCode = "AB12CD",
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )
    private val tripWithPhoto = trip.copy(photo = uploadedPhoto)

    private fun prepareScenario(
        storedGroup: Group = trip,
        isRoomRead: Boolean = true,
        isSaveHeld: Boolean = false,
        isLoadHeld: Boolean = false,
        isPrepareHeld: Boolean = false,
        updateError: Throwable? = null,
        loadError: Throwable? = null,
        prepareError: Throwable? = null,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup)).also {
            it.updateGroupError = updateError
            it.updateGroupGate = if (isSaveHeld) CompletableDeferred() else null
        }
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(groupDetail)
            it.navigate(groupPhoto)
        }
        heldLoad = CompletableDeferred<Unit>().also { if (!isLoadHeld) it.complete(Unit) }
        heldPrepare = CompletableDeferred<Unit>().also { if (!isPrepareHeld) it.complete(Unit) }
        preparations = 0
        unreadRoom = MutableSharedFlow(extraBufferCapacity = 1)
        viewModel = GroupPhotoViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = groupPhoto,
            navigator = navigator,
            updateGroup = UpdateGroupUseCase(groups) { 1_000L },
            observeGroup = if (isRoomRead) ObserveGroupUseCase(groups) else ObserveGroup { unreadRoom },
            loadCropPhoto = { source ->
                heldLoad.await()
                if (loadError != null) {
                    Result.failure(loadError)
                } else {
                    Result.success(
                        CropPhoto(
                            jpeg = source,
                            width = 1200,
                            height = 900,
                        ),
                    )
                }
            },
            prepareProfilePhoto = { _, _ ->
                preparations++
                heldPrepare.await()
                if (prepareError != null) Result.failure(prepareError) else Result.success(preparedPhoto)
            },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val UPLOADED_PHOTO_URL = "https://supabase/storage/groups/group-1.jpg"
        const val SOURCE = "group_photo"
    }
}
