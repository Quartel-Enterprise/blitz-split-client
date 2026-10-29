package com.quare.blitzsplit.feature.creategroup

import com.quare.blitzsplit.core.groups.domain.model.NewGroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CheckMemberNameUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CreateGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateGroupViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var profiles: FakeProfileRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: CreateGroupViewModel

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
            assertNull(state.takenName)
            assertTrue(state.nicknameSuggestions.isEmpty())
            assertFalse(state.isCreating)
            assertFalse(state.canCreate)
            assertFalse(state.canAddMember)
            assertTrue(state.showsNoAppNotice)
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
    fun `GIVEN Ana in the group WHEN typing a padded aNA THEN warns about Ana and suggests two nicknames`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            addMember("Ana")

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("  aNA  "))

            // Then
            assertEquals("Ana", viewModel.uiState.value.takenName)
            assertEquals(listOf("Ana P.", "Ana 2"), viewModel.uiState.value.nicknameSuggestions)
        }

    @Test
    fun `GIVEN a named group with Ana WHEN typing Ana again THEN can neither add nor create nor show the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
            addMember("Ana")

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Ana"))

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.canAddMember)
            assertFalse(state.canCreate)
            assertFalse(state.showsNoAppNotice)
        }

    @Test
    fun `GIVEN the creator Pierre WHEN typing pierre THEN warns about Pierre`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profile = pierre)
        val draft = "pierre"

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged(draft))

        // Then
        assertEquals("Pierre", viewModel.uiState.value.takenName)
        assertEquals(listOf("Pierre P.", "Pierre 2"), viewModel.uiState.value.nicknameSuggestions)
    }

    @Test
    fun `GIVEN a draft repeating Ana WHEN the draft becomes a free name THEN clears the warning`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            addMember("Ana")
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Ana"))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Ana Paula"))

            // Then
            val state = viewModel.uiState.value
            assertNull(state.takenName)
            assertTrue(state.nicknameSuggestions.isEmpty())
            assertTrue(state.canAddMember)
            assertTrue(state.showsNoAppNotice)
        }

    @Test
    fun `GIVEN a draft repeating Ana WHEN the draft is cleared THEN clears the warning and can add nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            addMember("Ana")
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Ana"))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("  "))

            // Then
            val state = viewModel.uiState.value
            assertNull(state.takenName)
            assertTrue(state.nicknameSuggestions.isEmpty())
            assertFalse(state.canAddMember)
            assertTrue(state.showsNoAppNotice)
        }

    @Test
    fun `GIVEN the creator name typed early WHEN the profile is read THEN warns about the creator`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = null)
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Pierre"))

            // When
            profiles.profile.value = pierre

            // Then
            assertEquals("Pierre", viewModel.uiState.value.takenName)
            assertFalse(viewModel.uiState.value.canAddMember)
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
    fun `GIVEN a draft repeating Ana WHEN adding it THEN adds nobody and keeps the warning`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            addMember("Ana")
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("ana"))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnAddMemberClicked)

            // Then
            assertEquals(
                listOf("Pierre", "Ana"),
                viewModel.uiState.value.members
                    .map { it.name },
            )
            assertEquals("Ana", viewModel.uiState.value.takenName)
        }

    @Test
    fun `GIVEN a draft repeating Ana WHEN picking a nickname THEN adds it and clears the draft and the warning`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            addMember("Ana")
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Ana"))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnSuggestionClicked("Ana P."))

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf("Pierre", "Ana", "Ana P."), state.members.map { it.name })
            assertFalse(state.members.last().isMe)
            assertEquals("", state.draft)
            assertNull(state.takenName)
            assertTrue(state.nicknameSuggestions.isEmpty())
        }

    @Test
    fun `GIVEN a draft repeating Ana WHEN picking the numbered nickname THEN adds that nickname`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            addMember("Ana")
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Ana"))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnSuggestionClicked("Ana 2"))

            // Then
            assertEquals(
                listOf("Pierre", "Ana", "Ana 2"),
                viewModel.uiState.value.members
                    .map { it.name },
            )
        }

    @Test
    fun `GIVEN a draft repeating Ana WHEN picking a name that was not suggested THEN adds nobody`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            addMember("Ana")
            viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Ana"))

            // When
            viewModel.onEvent(CreateGroupUiEvent.OnSuggestionClicked("Ana Z."))

            // Then
            assertEquals(
                listOf("Pierre", "Ana"),
                viewModel.uiState.value.members
                    .map { it.name },
            )
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
    fun `GIVEN a draft repeating Ana WHEN removing Ana THEN the draft is free again`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        addMember("Ana")
        viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("Ana"))
        val anaId = memberIdOf("Ana")

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnRemoveMemberClicked(anaId))

        // Then
        val state = viewModel.uiState.value
        assertNull(state.takenName)
        assertTrue(state.nicknameSuggestions.isEmpty())
        assertTrue(state.canAddMember)
        assertTrue(state.showsNoAppNotice)
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
        assertTrue(state.showsNoAppNotice)
    }

    @Test
    fun `GIVEN a filled group WHEN creating THEN creates it once with the trimmed name and its members in order`() =
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
        assertEquals(listOf(Route.Groups, Route.Invite(groupId)), navigator.backStack.value)
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
    fun `GIVEN a draft repeating Ana WHEN creating THEN creates nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged("Viagem Floripa"))
        addMember("Ana")
        viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged("ANA"))

        // When
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)

        // Then
        assertTrue(groups.createdGroups.isEmpty())
        assertEquals(listOf(Route.Groups, Route.CreateGroup), navigator.backStack.value)
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

    private val pierre = UserProfile("Pierre", AvatarColor.BLUE, null)

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
    ) {
        profiles = FakeProfileRepository(profile)
        groups = FakeGroupRepository()
        navigator = BackStackNavigator(startRoute = Route.Groups).also { it.navigate(Route.CreateGroup) }
        viewModel = CreateGroupViewModel(
            navigator = navigator,
            createGroup = CreateGroupUseCase(groups) { 1_000L },
            checkMemberName = CheckMemberNameUseCase(),
            observeProfile = ObserveProfileUseCase(profiles),
            deviceRegion = { region },
        )
    }
}
