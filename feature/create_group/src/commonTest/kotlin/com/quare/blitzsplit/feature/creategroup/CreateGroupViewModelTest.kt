package com.quare.blitzsplit.feature.creategroup

import com.quare.blitzsplit.core.groups.domain.model.NewGroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CheckMemberNameUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CreateGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiEvent
import com.quare.blitzsplit.feature.creategroup.presentation.viewmodel.CreateGroupViewModel
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateGroupViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val backStacksWhenTracked = mutableListOf<List<Route>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var profiles: FakeProfileRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: CreateGroupViewModel
    private lateinit var loadGate: CompletableDeferred<Unit>
    private lateinit var prepareGate: CompletableDeferred<Unit>
    private var loadError: Throwable? = null
    private var prepareError: Throwable? = null
    private val loadedSources = mutableListOf<ByteArray>()
    private val preparedCrops = mutableListOf<PhotoCrop>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a profile WHEN the screen opens THEN the creator is the only member`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profile = pierre)

        // When
        val members = viewModel.uiState.value.members

        // Then
        assertEquals(1, members.size)
        val me = members.single()
        assertEquals("Pierre", me.name)
        assertEquals("P", me.initials)
        assertEquals(AvatarColor.BLUE, me.avatarColor)
        assertTrue(me.isMe)
    }

    @Test
    fun `GIVEN a padded lower case name WHEN the screen opens THEN the creator is trimmed with a capital initial`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = pierre.copy(displayName = "  pierre vieira  "))

            // When
            val me = viewModel.uiState.value.members
                .single()

            // Then
            assertEquals("pierre vieira", me.name)
            assertEquals("P", me.initials)
        }

    @Test
    fun `GIVEN no profile yet WHEN the screen opens THEN lists no member`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profile = null)

        // When
        val members = viewModel.uiState.value.members

        // Then
        assertTrue(members.isEmpty())
    }

    @Test
    fun `GIVEN no profile when the screen opened WHEN the profile is read THEN the creator becomes the first member`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = null)

            // When
            profiles.profile.value = pierre.copy(avatarColor = AvatarColor.GREEN)

            // Then
            val me = viewModel.uiState.value.members
                .single()
            assertEquals("Pierre", me.name)
            assertEquals(AvatarColor.GREEN, me.avatarColor)
            assertTrue(me.isMe)
            assertEquals(AvatarColor.BLUE, viewModel.uiState.value.color)
        }

    @Test
    fun `GIVEN the creator already read WHEN the profile changes THEN the members stay as they were`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = pierre)
            val membersBefore = viewModel.uiState.value.members

            // When
            profiles.profile.value = UserProfile("Joana", AvatarColor.SAND, null)

            // Then
            assertEquals(membersBefore, viewModel.uiState.value.members)
            assertEquals(AvatarColor.PEACH, viewModel.uiState.value.color)
        }

    @Test
    fun `GIVEN a creator in blue WHEN the screen opens THEN the group starts in peach`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profile = pierre.copy(avatarColor = AvatarColor.BLUE))

        // When
        val color = viewModel.uiState.value.color

        // Then
        assertEquals(AvatarColor.PEACH, color)
    }

    @Test
    fun `GIVEN a creator in green WHEN the screen opens THEN the group starts in blue`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profile = pierre.copy(avatarColor = AvatarColor.GREEN))

        // When
        val color = viewModel.uiState.value.color

        // Then
        assertEquals(AvatarColor.BLUE, color)
    }

    @Test
    fun `GIVEN a profile WHEN the screen opens THEN starts as an unnamed trip with nothing typed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("", state.name)
            assertEquals("", state.initials)
            assertEquals(GroupType.TRIP, state.type)
            assertEquals("", state.draft)
            assertFalse(state.isCreating)
            assertFalse(state.canCreate)
            assertFalse(state.canAddMember)
        }

    @Test
    fun `GIVEN a device in Brazil WHEN the screen opens THEN the group splits in reais`() = runTest(testDispatcher) {
        // Given
        prepareScenario(region = "BR")

        // When
        val currency = viewModel.uiState.value.currency

        // Then
        assertEquals(GroupCurrency.BRL, currency)
    }

    @Test
    fun `GIVEN a device in the United States WHEN the screen opens THEN the group splits in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(region = "US")

            // When
            val currency = viewModel.uiState.value.currency

            // Then
            assertEquals(GroupCurrency.USD, currency)
        }

    @Test
    fun `GIVEN a region with no listed currency WHEN the screen opens THEN the group splits in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(region = "IN")

            // When
            val currency = viewModel.uiState.value.currency

            // Then
            assertEquals(GroupCurrency.USD, currency)
        }

    @Test
    fun `GIVEN a device with no region WHEN the screen opens THEN the group splits in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(region = null)

            // When
            val currency = viewModel.uiState.value.currency

            // Then
            assertEquals(GroupCurrency.USD, currency)
        }

    @Test
    fun `GIVEN the screen open WHEN typing a name THEN shows the name and its initials`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val name = "Viagem Floripa"

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged(name))

        // Then
        assertEquals("Viagem Floripa", viewModel.uiState.value.name)
        assertEquals("VF", viewModel.uiState.value.initials)
    }

    @Test
    fun `GIVEN a typed name WHEN the name is cleared to blank THEN shows no initials`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("   "))

        // Then
        assertEquals("", viewModel.uiState.value.initials)
        assertFalse(viewModel.uiState.value.canCreate)
    }

    @Test
    fun `GIVEN the screen open WHEN typing a name longer than 40 characters THEN keeps the first 40`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val longName = "A".repeat(45)

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged(longName))

            // Then
            assertEquals("A".repeat(40), viewModel.uiState.value.name)
        }

    @Test
    fun `GIVEN the group in peach WHEN tapping the avatar THEN the group turns green`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profile = pierre.copy(avatarColor = AvatarColor.BLUE))

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnColorClicked)

        // Then
        assertEquals(AvatarColor.GREEN, viewModel.uiState.value.color)
    }

    @Test
    fun `GIVEN the group in the last color WHEN tapping the avatar THEN wraps around to the first color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = pierre.copy(avatarColor = AvatarColor.BLUE))
            repeat(3) { viewModel.onEvent(CreateGroupUiEvent.OnColorClicked) }

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnColorClicked)

            // Then
            assertEquals(AvatarColor.BLUE, viewModel.uiState.value.color)
        }

    @Test
    fun `GIVEN a trip WHEN picking home THEN the group is a home`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val type = GroupType.HOME

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnTypeSelected(type))

        // Then
        assertEquals(GroupType.HOME, viewModel.uiState.value.type)
    }

    @Test
    fun `GIVEN reais WHEN picking euros THEN the group splits in euros`() = runTest(testDispatcher) {
        // Given
        prepareScenario(region = "BR")
        val currency = GroupCurrency.EUR

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnCurrencySelected(currency))

        // Then
        assertEquals(GroupCurrency.EUR, viewModel.uiState.value.currency)
    }

    @Test
    fun `GIVEN the screen open WHEN typing a member name longer than 40 characters THEN keeps the first 40`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val longDraft = "B".repeat(45)

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged(longDraft))

            // Then
            assertEquals("B".repeat(40), viewModel.uiState.value.draft)
        }

    @Test
    fun `GIVEN a named group with Ana WHEN typing Ana again THEN can add and create`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
        addMember("Ana")

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Ana"))

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.canAddMember)
        assertTrue(state.canCreate)
    }

    @Test
    fun `GIVEN Ana added WHEN typing a single letter THEN can neither add it nor create the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Floripa"))
            addMember("Ana")

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("J"))

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.canAddMember)
            assertFalse(state.canCreate)
        }

    @Test
    fun `GIVEN a padded draft WHEN adding it THEN adds the trimmed name as a member and clears the draft`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("  Ana Paula  "))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnAddMemberClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf("Pierre", "Ana Paula"), state.members.map { it.name })
            val ana = state.members.last()
            assertEquals("AP", ana.initials)
            assertFalse(ana.isMe)
            assertEquals("", state.draft)
            assertFalse(state.canAddMember)
        }

    @Test
    fun `GIVEN a creator in blue WHEN adding the first member THEN the member takes the first color nobody has`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = pierre.copy(avatarColor = AvatarColor.BLUE))
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Ana"))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnAddMemberClicked)

            // Then
            assertEquals(
                AvatarColor.PEACH,
                viewModel.uiState.value.members
                    .last()
                    .avatarColor,
            )
        }

    @Test
    fun `GIVEN every color taken and the only green member removed WHEN adding a member THEN the member is green`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = pierre.copy(avatarColor = AvatarColor.BLUE))
            listOf("Ana", "Bia", "Caio", "Duda", "Edu", "Fer").forEach(::addMember)
            viewModel.onEvent(CreateGroupUiEvent.OnRemoveMemberClicked(memberIdOf("Bia")))
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Gabi"))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnAddMemberClicked)

            // Then
            assertEquals(
                AvatarColor.GREEN,
                viewModel.uiState.value.members
                    .last()
                    .avatarColor,
            )
        }

    @Test
    fun `GIVEN a blank draft WHEN adding it THEN adds nobody`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("   "))

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnAddMemberClicked)

        // Then
        assertEquals(
            listOf("Pierre"),
            viewModel.uiState.value.members
                .map { it.name },
        )
    }

    @Test
    fun `GIVEN a draft repeating Ana WHEN adding it THEN adds a second Ana as typed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        addMember("Ana")
        viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("ana"))

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnAddMemberClicked)

        // Then
        assertEquals(
            listOf("Pierre", "Ana", "ana"),
            viewModel.uiState.value.members
                .map { it.name },
        )
        assertEquals("", viewModel.uiState.value.draft)
    }

    @Test
    fun `GIVEN Ana in the group WHEN removing Ana THEN only the creator is left`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        addMember("Ana")
        val anaId = memberIdOf("Ana")

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnRemoveMemberClicked(anaId))

        // Then
        assertEquals(
            listOf("Pierre"),
            viewModel.uiState.value.members
                .map { it.name },
        )
    }

    @Test
    fun `GIVEN the creator in the group WHEN removing the creator THEN the creator stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        addMember("Ana")
        val myId = memberIdOf("Pierre")

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnRemoveMemberClicked(myId))

        // Then
        assertEquals(
            listOf("Pierre", "Ana"),
            viewModel.uiState.value.members
                .map { it.name },
        )
        assertTrue(
            viewModel.uiState.value.members
                .first()
                .isMe,
        )
    }

    @Test
    fun `GIVEN a name and a draft not added yet WHEN reading the state THEN can create`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
        viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Ana"))

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.canCreate)
        assertTrue(state.canAddMember)
    }

    @Test
    fun `GIVEN a filled group WHEN creating THEN creates one square with the trimmed name and members in order`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                profile = pierre.copy(avatarColor = AvatarColor.BLUE),
                region = "BR",
            )
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("  Viagem Floripa  "))
            viewModel.onEvent(CreateGroupUiEvent.OnColorClicked)
            viewModel.onEvent(CreateGroupUiEvent.OnTypeSelected(GroupType.HOME))
            viewModel.onEvent(CreateGroupUiEvent.OnCurrencySelected(GroupCurrency.EUR))
            addMember("Ana")
            addMember("Bia")

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

            // Then
            val group = groups.createdGroups.single()
            assertEquals("Viagem Floripa", group.name)
            assertEquals(GroupType.HOME, group.type)
            assertEquals(AvatarColor.GREEN, group.color)
            assertEquals(GroupShape.SQUARE, group.shape)
            assertNull(group.photo)
            assertEquals(GroupCurrency.EUR, group.currency)
            assertEquals(
                listOf(
                    NewGroupMember("Pierre", AvatarColor.BLUE, isMe = true),
                    NewGroupMember("Ana", AvatarColor.PEACH, isMe = false),
                    NewGroupMember("Bia", AvatarColor.GREEN, isMe = false),
                ),
                group.members.map { NewGroupMember(it.name, it.avatarColor, it.isMe) },
            )
        }

    @Test
    fun `GIVEN a draft typed and not added WHEN creating THEN the draft goes in as the last member`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = pierre.copy(avatarColor = AvatarColor.BLUE))
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
            addMember("Ana")
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("  Bia  "))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

            // Then
            val members = groups.createdGroups.single().members
            assertEquals(listOf("Pierre", "Ana", "Bia"), members.map { it.name })
            assertEquals(AvatarColor.GREEN, members.last().avatarColor)
            assertFalse(members.last().isMe)
        }

    @Test
    fun `GIVEN a filled group WHEN tapping create twice THEN creates a single group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

        // Then
        assertEquals(1, groups.createdGroups.size)
        assertTrue(viewModel.uiState.value.isCreating)
        assertFalse(viewModel.uiState.value.canCreate)
    }

    @Test
    fun `GIVEN a filled group WHEN creating THEN opens its invite in place of the form`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

        // Then
        val groupId = groups.createdGroups.single().id
        assertEquals(listOf(Route.Groups, Route.Invite(groupId, isNewGroup = true)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the device fails to save the group WHEN creating THEN stays on the screen and can try again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.createError = IllegalStateException("disk full")
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

            // Then
            assertFalse(viewModel.uiState.value.isCreating)
            assertTrue(viewModel.uiState.value.canCreate)
            assertTrue(viewModel.uiState.value.showsCreateError)
            assertEquals(listOf(Route.Groups, Route.CreateGroup), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a failed save WHEN creating again THEN hides the error while it saves`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        groups.createError = IllegalStateException("disk full")
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)
        groups.createError = null

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

        // Then
        assertFalse(viewModel.uiState.value.showsCreateError)
        assertEquals(1, groups.createdGroups.size)
    }

    @Test
    fun `GIVEN a save in flight WHEN closing THEN stays on the screen`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        groups.createGate = CompletableDeferred()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnCloseClicked)

        // Then
        assertTrue(viewModel.uiState.value.isCreating)
        assertEquals(listOf(Route.Groups, Route.CreateGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the screen closed by a native back WHEN creating THEN creates the group and keeps the back stack`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
            navigator.navigateBack()
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

            // Then
            assertEquals(1, groups.createdGroups.size)
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no group name WHEN creating THEN creates nothing and stays on the screen`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("   "))

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

        // Then
        assertTrue(groups.createdGroups.isEmpty())
        assertFalse(viewModel.uiState.value.isCreating)
        assertEquals(listOf(Route.Groups, Route.CreateGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a draft repeating Ana WHEN creating THEN creates the group with both`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
        addMember("Ana")
        viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("ANA"))

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

        // Then
        assertEquals(
            listOf("Pierre", "Ana", "ANA"),
            groups.createdGroups
                .single()
                .members
                .map { it.name },
        )
    }

    @Test
    fun `GIVEN no profile read yet WHEN creating a named group THEN creates nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profile = null)
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

        // Then
        assertTrue(groups.createdGroups.isEmpty())
        assertFalse(viewModel.uiState.value.canCreate)
    }

    @Test
    fun `GIVEN the screen on top WHEN closing THEN returns to the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val close = CreateGroupUiEvent.OnCloseClicked

        // When
        viewModel.onEvent(close)

        // Then
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
        assertTrue(groups.createdGroups.isEmpty())
    }

    @Test
    fun `GIVEN the screen already closed by a native back WHEN closing THEN leaves the back stack as it is`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnCloseClicked)

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no photo WHEN picking a photo THEN opens Ajustar foto with the photo as picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

            // Then
            val state = viewModel.uiState.value
            val editor = assertNotNull(state.photoCrop)
            assertContentEquals(firstPhoto, editor.photo.jpeg)
            assertEquals(PhotoCrop.of(1200, 900), editor.crop)
            assertFalse(state.isPreparingPhoto)
            assertFalse(state.showsPhotoError)
            assertNull(state.photo)
            assertFalse(state.hasPhoto)
        }

    @Test
    fun `GIVEN a picked photo still loading WHEN reading the state THEN shows it preparing and can not create`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isLoadHeld = true)
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isPreparingPhoto)
            assertNull(state.photoCrop)
            assertFalse(state.canCreate)
            assertFalse(state.canChangeColor)
        }

    @Test
    fun `GIVEN a named group with Ajustar foto open WHEN reading the state THEN can not create`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

            // When
            val state = viewModel.uiState.value

            // Then
            assertNotNull(state.photoCrop)
            assertFalse(state.canCreate)
        }

    @Test
    fun `GIVEN Ajustar foto open WHEN dragging the photo THEN moves the crop`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

        // When
        viewModel.onEvent(
            CreateGroupUiEvent.OnPhotoCrop(
                PhotoCropEvent.OnDragged(
                    dx = 0.1f,
                    dy = 0f,
                ),
            ),
        )

        // Then
        assertEquals(
            PhotoCrop.of(1200, 900).toDragged(0.1f, 0f),
            viewModel.uiState.value.photoCrop
                ?.crop,
        )
    }

    @Test
    fun `GIVEN Ajustar foto open WHEN pinching the photo THEN zooms the crop by the pinch`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

        // When
        viewModel.onEvent(
            CreateGroupUiEvent.OnPhotoCrop(
                PhotoCropEvent.OnPinched(
                    zoomChange = 1.5f,
                    focusX = 0f,
                    focusY = 0f,
                ),
            ),
        )

        // Then
        assertEquals(
            1.5f,
            viewModel.uiState.value.photoCrop
                ?.crop
                ?.zoom,
        )
    }

    @Test
    fun `GIVEN Ajustar foto open WHEN moving the zoom slider THEN zooms the crop to it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnZoomChanged(zoom = 2f)))

        // Then
        assertEquals(
            2f,
            viewModel.uiState.value.photoCrop
                ?.crop
                ?.zoom,
        )
    }

    @Test
    fun `GIVEN Ajustar foto open WHEN turning the photo THEN the crop takes a quarter turn`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnTurnClicked))

            // Then
            assertEquals(
                1,
                viewModel.uiState.value.photoCrop
                    ?.crop
                    ?.quarterTurns,
            )
        }

    @Test
    fun `GIVEN Ajustar foto open WHEN mirroring the photo horizontally THEN the crop is mirrored`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnMirrorHorizontallyClicked))

            // Then
            assertEquals(
                PhotoCrop.of(1200, 900).toMirroredHorizontally(),
                viewModel.uiState.value.photoCrop
                    ?.crop,
            )
        }

    @Test
    fun `GIVEN Ajustar foto open WHEN mirroring the photo vertically THEN the crop is mirrored upside down`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnMirrorVerticallyClicked))

            // Then
            assertEquals(
                PhotoCrop.of(1200, 900).toMirroredVertically(),
                viewModel.uiState.value.photoCrop
                    ?.crop,
            )
        }

    @Test
    fun `GIVEN Ajustar foto open on the first photo WHEN dismissing it THEN the group has no photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnDismissed))

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photoCrop)
            assertNull(state.photo)
            assertFalse(state.hasPhoto)
            assertFalse(state.isPreparingPhoto)
            assertTrue(state.canChangeColor)
            assertTrue(preparedCrops.isEmpty())
        }

    @Test
    fun `GIVEN a group photo and Ajustar foto open on another WHEN dismissing it THEN keeps the group photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            pickAndConfirm(firstPhoto)
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(secondPhoto))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnDismissed))

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photoCrop)
            assertEquals(ProfilePhoto.Pending(firstPhoto), state.photo)
            assertFalse(state.isPreparingPhoto)
        }

    @Test
    fun `GIVEN a turned photo in Ajustar foto WHEN confirming THEN shows the square prepared from that crop`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnTurnClicked))
            val framed = viewModel.uiState.value.photoCrop
                ?.crop

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            val state = viewModel.uiState.value
            val expected = ProfilePhoto.Pending(firstPhoto)
            assertNull(state.photoCrop)
            assertEquals(expected, state.photo)
            assertEquals(expected.dataUri, state.photoUrl)
            assertTrue(state.hasPhoto)
            assertFalse(state.isPreparingPhoto)
            assertFalse(state.showsPhotoError)
            assertEquals(listOf(framed), preparedCrops)
        }

    @Test
    fun `GIVEN a named group WHEN a confirmed photo is being prepared THEN shows it preparing and can not create`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isPrepareHeld = true)
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photoCrop)
            assertTrue(state.isPreparingPhoto)
            assertNull(state.photo)
            assertFalse(state.canCreate)
            assertFalse(state.canChangeColor)
        }

    @Test
    fun `GIVEN a named group WHEN its photo is prepared THEN can create`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

        // Then
        assertTrue(viewModel.uiState.value.canCreate)
    }

    @Test
    fun `GIVEN a group photo WHEN the next picked photo fails to load THEN keeps the photo and shows the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            pickAndConfirm(firstPhoto)
            loadError = IllegalStateException("not an image")

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(secondPhoto))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertFalse(state.isPreparingPhoto)
            assertNull(state.photoCrop)
            assertEquals(ProfilePhoto.Pending(firstPhoto), state.photo)
        }

    @Test
    fun `GIVEN a group photo WHEN the next photo fails to prepare THEN keeps the photo and shows the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            pickAndConfirm(firstPhoto)
            prepareError = IllegalStateException("encoder failed")
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(secondPhoto))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertFalse(state.isPreparingPhoto)
            assertNull(state.photoCrop)
            assertEquals(ProfilePhoto.Pending(firstPhoto), state.photo)
        }

    @Test
    fun `GIVEN a group photo WHEN the picked file is unreadable THEN keeps the photo and shows the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            pickAndConfirm(firstPhoto)

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoUnreadable)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertFalse(state.isPreparingPhoto)
            assertEquals(ProfilePhoto.Pending(firstPhoto), state.photo)
        }

    @Test
    fun `GIVEN a photo error WHEN picking another photo THEN hides the error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoUnreadable)

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

        // Then
        assertFalse(viewModel.uiState.value.showsPhotoError)
        assertNotNull(viewModel.uiState.value.photoCrop)
    }

    @Test
    fun `GIVEN a first photo still loading WHEN picking a second photo THEN only the second opens in Ajustar foto`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isLoadHeld = true)
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(secondPhoto))

            // When
            loadGate.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            val editor = assertNotNull(state.photoCrop)
            assertContentEquals(secondPhoto, editor.photo.jpeg)
            assertEquals(listOf(firstPhoto, secondPhoto).map(ByteArray::toList), loadedSources.map(ByteArray::toList))
            assertFalse(state.isPreparingPhoto)
        }

    @Test
    fun `GIVEN a group photo and a photo error WHEN removing the photo THEN shows no photo and no error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            pickAndConfirm(firstPhoto)
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoUnreadable)

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoRemoved)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photo)
            assertNull(state.photoUrl)
            assertFalse(state.hasPhoto)
            assertFalse(state.isPreparingPhoto)
            assertFalse(state.showsPhotoError)
        }

    @Test
    fun `GIVEN Ajustar foto open WHEN removing the photo THEN closes Ajustar foto`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoRemoved)

        // Then
        assertNull(viewModel.uiState.value.photoCrop)
        assertNull(viewModel.uiState.value.photo)
    }

    @Test
    fun `GIVEN a picked photo still loading WHEN removing the photo THEN the late photo never opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isLoadHeld = true)
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoRemoved)

            // When
            loadGate.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photoCrop)
            assertNull(state.photo)
            assertFalse(state.isPreparingPhoto)
            assertFalse(state.showsPhotoError)
        }

    @Test
    fun `GIVEN a confirmed photo still being prepared WHEN removing the photo THEN the late photo never shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isPrepareHeld = true)
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoRemoved)

            // When
            prepareGate.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photo)
            assertFalse(state.isPreparingPhoto)
            assertTrue(state.canChangeColor)
        }

    @Test
    fun `GIVEN a group photo WHEN tapping the avatar THEN the color stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        pickAndConfirm(firstPhoto)
        val color = viewModel.uiState.value.color

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnColorClicked)

        // Then
        assertFalse(viewModel.uiState.value.canChangeColor)
        assertEquals(color, viewModel.uiState.value.color)
    }

    @Test
    fun `GIVEN a confirmed photo still being prepared WHEN tapping the avatar THEN the color stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isPrepareHeld = true)
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))
            val color = viewModel.uiState.value.color

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnColorClicked)

            // Then
            assertEquals(color, viewModel.uiState.value.color)
        }

    @Test
    fun `GIVEN the group photo removed WHEN tapping the avatar THEN the group turns green`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        pickAndConfirm(firstPhoto)
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoRemoved)

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnColorClicked)

        // Then
        assertTrue(viewModel.uiState.value.canChangeColor)
        assertEquals(AvatarColor.GREEN, viewModel.uiState.value.color)
    }

    @Test
    fun `GIVEN a named group with a photo WHEN creating THEN creates it with that photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
        pickAndConfirm(firstPhoto)

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

        // Then
        assertEquals(ProfilePhoto.Pending(firstPhoto), groups.createdGroups.single().photo)
    }

    @Test
    fun `GIVEN a group photo removed WHEN creating THEN creates it with no photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
        pickAndConfirm(firstPhoto)
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoRemoved)

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

        // Then
        assertNull(groups.createdGroups.single().photo)
    }

    @Test
    fun `GIVEN a save in flight WHEN picking a photo THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        groups.createGate = CompletableDeferred()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isCreating)
        assertNull(state.photoCrop)
        assertFalse(state.isPreparingPhoto)
        assertTrue(loadedSources.isEmpty())
    }

    @Test
    fun `GIVEN a save in flight WHEN confirming Ajustar foto THEN prepares nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        groups.createGate = CompletableDeferred()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

        // Then
        assertTrue(preparedCrops.isEmpty())
        assertFalse(viewModel.uiState.value.isPreparingPhoto)
        assertNull(viewModel.uiState.value.photo)
    }

    @Test
    fun `GIVEN a save in flight with a photo WHEN removing the photo THEN keeps the photo that is being saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
            pickAndConfirm(firstPhoto)
            groups.createGate = CompletableDeferred()
            viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnPhotoRemoved)

            // Then
            assertTrue(viewModel.uiState.value.isCreating)
            assertEquals(ProfilePhoto.Pending(firstPhoto), viewModel.uiState.value.photo)
        }

    @Test
    fun `GIVEN the screen on top WHEN closing THEN sends no event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val close = CreateGroupUiEvent.OnCloseClicked

        // When
        viewModel.onEvent(close)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the screen open WHEN typing the group name THEN sends no event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val typed = CreateGroupUiEvent.OnNameChanged("Viagem Floripa")

        // When
        viewModel.onEvent(typed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the screen open WHEN typing a member name THEN sends no event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val typed = CreateGroupUiEvent.OnDraftChanged("Ana")

        // When
        viewModel.onEvent(typed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the screen open WHEN the picked file is unreadable THEN sends no event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val unreadable = CreateGroupUiEvent.OnPhotoUnreadable

        // When
        viewModel.onEvent(unreadable)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Ajustar foto open WHEN dragging the photo THEN sends no event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))
        trackedEvents.clear()
        val drag = CreateGroupUiEvent.OnPhotoCrop(
            PhotoCropEvent.OnDragged(
                dx = 0.1f,
                dy = 0f,
            ),
        )

        // When
        viewModel.onEvent(drag)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Ajustar foto open WHEN pinching the photo THEN sends no event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))
        trackedEvents.clear()
        val pinch = CreateGroupUiEvent.OnPhotoCrop(
            PhotoCropEvent.OnPinched(
                zoomChange = 1.5f,
                focusX = 0.5f,
                focusY = 0.5f,
            ),
        )

        // When
        viewModel.onEvent(pinch)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Ajustar foto open WHEN moving the zoom slider THEN sends no event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))
        trackedEvents.clear()
        val zoom = CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnZoomChanged(2f))

        // When
        viewModel.onEvent(zoom)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the screen open WHEN tapping the avatar THEN sends the color event from the form`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val tap = CreateGroupUiEvent.OnColorClicked

            // When
            viewModel.onEvent(tap)

            // Then
            assertEquals(listOf("group_color_clicked" to mapOf<String, Any>("source" to "create_group")), trackedEvents)
        }

    @Test
    fun `GIVEN a group photo WHEN tapping the avatar THEN still sends the color event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        pickAndConfirm(firstPhoto)
        trackedEvents.clear()
        val tap = CreateGroupUiEvent.OnColorClicked

        // When
        viewModel.onEvent(tap)

        // Then
        assertEquals(listOf("group_color_clicked" to mapOf<String, Any>("source" to "create_group")), trackedEvents)
    }

    @Test
    fun `GIVEN the screen open WHEN a photo is picked THEN sends the photo picked event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val picked = CreateGroupUiEvent.OnPhotoPicked(firstPhoto)

        // When
        viewModel.onEvent(picked)

        // Then
        assertEquals(listOf("photo_picked" to mapOf<String, Any>("source" to "create_group")), trackedEvents)
    }

    @Test
    fun `GIVEN Ajustar foto open WHEN confirming THEN sends the crop confirmed event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(firstPhoto))
        trackedEvents.clear()
        val confirm = CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked)

        // When
        viewModel.onEvent(confirm)

        // Then
        assertEquals(listOf("photo_crop_confirmed" to mapOf<String, Any>("source" to "create_group")), trackedEvents)
    }

    @Test
    fun `GIVEN a group photo WHEN removing it THEN sends the photo removed event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        pickAndConfirm(firstPhoto)
        trackedEvents.clear()
        val remove = CreateGroupUiEvent.OnPhotoRemoved

        // When
        viewModel.onEvent(remove)

        // Then
        assertEquals(listOf("photo_removed" to mapOf<String, Any>("source" to "create_group")), trackedEvents)
    }

    @Test
    fun `GIVEN a trip WHEN picking home THEN sends the type event with home`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val pick = CreateGroupUiEvent.OnTypeSelected(GroupType.HOME)

        // When
        viewModel.onEvent(pick)

        // Then
        assertEquals(
            listOf("group_type_selected" to mapOf<String, Any>("type" to GroupType.HOME, "source" to "create_group")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN reais WHEN picking euros THEN sends the currency event with euros`() = runTest(testDispatcher) {
        // Given
        prepareScenario(region = "BR")
        val pick = CreateGroupUiEvent.OnCurrencySelected(GroupCurrency.EUR)

        // When
        viewModel.onEvent(pick)

        // Then
        assertEquals(
            listOf(
                "group_currency_selected" to mapOf<String, Any>(
                    "currency" to GroupCurrency.EUR,
                    "source" to "create_group",
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a member name typed WHEN adding it THEN sends the add member event with no params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Ana"))
            val add = CreateGroupUiEvent.OnAddMemberClicked

            // When
            viewModel.onEvent(add)

            // Then
            assertEquals(listOf("create_group_add_member_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN a blank draft WHEN adding it THEN still sends the add member event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val add = CreateGroupUiEvent.OnAddMemberClicked

        // When
        viewModel.onEvent(add)

        // Then
        assertEquals(listOf("create_group_add_member_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN Ana in the group WHEN removing Ana THEN sends the remove member event without the id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            addMember("Ana")
            trackedEvents.clear()
            val remove = CreateGroupUiEvent.OnRemoveMemberClicked(memberIdOf("Ana"))

            // When
            viewModel.onEvent(remove)

            // Then
            assertEquals(listOf("create_group_remove_member_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN no group name WHEN creating THEN sends only the create tap`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val create = CreateGroupUiEvent.OnCreateClicked

        // When
        viewModel.onEvent(create)

        // Then
        assertEquals(listOf("create_group_create_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN a filled group WHEN creating THEN sends the tap and then the group created with its kind`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(region = "BR")
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
            viewModel.onEvent(CreateGroupUiEvent.OnTypeSelected(GroupType.HOME))
            viewModel.onEvent(CreateGroupUiEvent.OnCurrencySelected(GroupCurrency.EUR))
            addMember("Ana")
            trackedEvents.clear()
            val create = CreateGroupUiEvent.OnCreateClicked

            // When
            viewModel.onEvent(create)

            // Then
            assertEquals(
                listOf(
                    "create_group_create_clicked" to emptyMap(),
                    "group_created" to mapOf<String, Any>(
                        "type" to GroupType.HOME,
                        "currency" to GroupCurrency.EUR,
                        "members" to 2,
                        "has_photo" to false,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN only the creator WHEN creating THEN the group created counts one member`() = runTest(testDispatcher) {
        // Given
        prepareScenario(region = "BR")
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
        val create = CreateGroupUiEvent.OnCreateClicked

        // When
        viewModel.onEvent(create)

        // Then
        assertEquals(
            "group_created" to mapOf<String, Any>(
                "type" to GroupType.TRIP,
                "currency" to GroupCurrency.BRL,
                "members" to 1,
                "has_photo" to false,
            ),
            trackedEvents.last(),
        )
    }

    @Test
    fun `GIVEN a draft typed and not added WHEN creating THEN the group created counts the draft as a member`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
            addMember("Ana")
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("  Bia  "))
            val create = CreateGroupUiEvent.OnCreateClicked

            // When
            viewModel.onEvent(create)

            // Then
            val (name, params) = trackedEvents.last()
            assertEquals("group_created", name)
            assertEquals(3, params["members"])
        }

    @Test
    fun `GIVEN a group photo WHEN creating THEN the group created says it has a photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
        pickAndConfirm(firstPhoto)
        val create = CreateGroupUiEvent.OnCreateClicked

        // When
        viewModel.onEvent(create)

        // Then
        val (name, params) = trackedEvents.last()
        assertEquals("group_created", name)
        assertEquals(true, params["has_photo"])
    }

    @Test
    fun `GIVEN a filled group WHEN creating THEN sends the group created before the invite opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
            backStacksWhenTracked.clear()
            val create = CreateGroupUiEvent.OnCreateClicked

            // When
            viewModel.onEvent(create)

            // Then
            assertEquals(listOf(Route.Groups, Route.CreateGroup), backStacksWhenTracked.last())
            assertEquals(
                Route.Invite(groups.createdGroups.single().id, isNewGroup = true),
                navigator.backStack.value.last(),
            )
        }

    @Test
    fun `GIVEN a group with names WHEN creating THEN no event carries a group or member name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
            addMember("Ana")
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Bia"))
            trackedEvents.clear()
            val create = CreateGroupUiEvent.OnCreateClicked

            // When
            viewModel.onEvent(create)

            // Then
            val values = trackedEvents.flatMap { it.second.values }.map { it.toString() }
            assertTrue(values.none { it.contains("Viagem") || it == "Pierre" || it == "Ana" || it == "Bia" })
        }

    @Test
    fun `GIVEN the device fails to save the group WHEN creating THEN sends only the create tap`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.createError = IllegalStateException("disk full")
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
            val create = CreateGroupUiEvent.OnCreateClicked

            // When
            viewModel.onEvent(create)

            // Then
            assertEquals(listOf("create_group_create_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN a save in flight WHEN tapping create again THEN sends a second tap and no group created yet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.createGate = CompletableDeferred()
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
            viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)
            val create = CreateGroupUiEvent.OnCreateClicked

            // When
            viewModel.onEvent(create)

            // Then
            assertEquals(
                listOf(
                    "create_group_create_clicked" to emptyMap<String, Any>(),
                    "create_group_create_clicked" to emptyMap(),
                ),
                trackedEvents,
            )
        }

    private val pierre = UserProfile("Pierre", AvatarColor.BLUE, null)

    private val firstPhoto = byteArrayOf(1, 2, 3)

    private val secondPhoto = byteArrayOf(4, 5, 6)

    private fun pickAndConfirm(source: ByteArray) {
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(source))
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))
    }

    private fun addMember(name: String) {
        viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged(name))
        viewModel.onEvent(CreateGroupUiEvent.OnAddMemberClicked)
    }

    private fun memberIdOf(name: String): String = viewModel.uiState.value.members
        .first { it.name == name }
        .id

    private fun prepareScenario(
        profile: UserProfile? = pierre,
        region: String? = "BR",
        isLoadHeld: Boolean = false,
        isPrepareHeld: Boolean = false,
    ) {
        loadGate = CompletableDeferred<Unit>().also { if (!isLoadHeld) it.complete(Unit) }
        prepareGate = CompletableDeferred<Unit>().also { if (!isPrepareHeld) it.complete(Unit) }
        profiles = FakeProfileRepository(profile)
        groups = FakeGroupRepository()
        navigator = BackStackNavigator(startRoute = Route.Groups).also { it.navigate(Route.CreateGroup) }
        viewModel = CreateGroupViewModel(
            trackEvent = { name, params ->
                trackedEvents += name to params
                backStacksWhenTracked += navigator.backStack.value
            },
            navigator = navigator,
            createGroup = CreateGroupUseCase(groups) { 1_000L },
            checkMemberName = CheckMemberNameUseCase(),
            observeProfile = ObserveProfileUseCase(profiles),
            deviceRegion = { region },
            loadCropPhoto = { source ->
                loadedSources += source
                loadGate.await()
                loadError?.let { Result.failure(it) } ?: Result.success(
                    CropPhoto(
                        jpeg = source,
                        width = 1200,
                        height = 900,
                    ),
                )
            },
            prepareProfilePhoto = { photo, crop ->
                preparedCrops += crop
                prepareGate.await()
                prepareError?.let { Result.failure(it) } ?: Result.success(ProfilePhoto.Pending(photo.jpeg))
            },
        )
    }
}
