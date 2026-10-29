package com.quare.blitzsplit.feature.members

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.AddMemberUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CheckMemberNameUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.RemoveMemberUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.RenameMemberUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.members.presentation.model.MemberNameEditor
import com.quare.blitzsplit.feature.members.presentation.model.MembersMember
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiAction
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent
import com.quare.blitzsplit.feature.members.presentation.viewmodel.MembersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
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

class MembersViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: MembersViewModel
    private val actions = mutableListOf<MembersUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Room not read yet WHEN the screen opens THEN nothing is loaded`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertEquals("", state.groupName)
        assertNull(state.displayLink)
        assertTrue(state.members.isEmpty())
        assertNull(state.editor)
        assertNull(state.memberToRemove)
    }

    @Test
    fun `GIVEN a group on the device WHEN the screen opens THEN shows its name and members in order`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals("Viagem Floripa", state.groupName)
            assertEquals(
                listOf(
                    MembersMember(
                        "member-1",
                        "Pierre",
                        "P",
                        AvatarColor.BLUE,
                        isMe = true,
                        hasJoined = true,
                    ),
                    MembersMember(
                        "member-2",
                        "Ana",
                        "AN",
                        AvatarColor.PEACH,
                        isMe = false,
                        hasJoined = false,
                    ),
                    MembersMember(
                        "member-3",
                        "Bruno",
                        "BR",
                        AvatarColor.GREEN,
                        isMe = false,
                        hasJoined = true,
                    ),
                    MembersMember(
                        "member-4",
                        "Carla",
                        "CA",
                        AvatarColor.LILAC,
                        isMe = false,
                        hasJoined = false,
                    ),
                ),
                state.members,
            )
        }

    @Test
    fun `GIVEN a group with four members and two joined WHEN the screen opens THEN counts them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(4, state.memberCount)
            assertEquals(2, state.joinedCount)
        }

    @Test
    fun `GIVEN a group with an invite code WHEN the screen opens THEN shows the link without the scheme`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(inviteCode = "7Hk2pQ"))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("borarachar.com/g/7Hk2pQ", state.displayLink)
        }

    @Test
    fun `GIVEN a group without an invite code yet WHEN the screen opens THEN shows no link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(inviteCode = null))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertNull(state.displayLink)
        }

    @Test
    fun `GIVEN the screen open WHEN a sync renames the group and adds someone THEN shows the change`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val synced = trip.copy(
                name = "Floripa 2026",
                members = trip.members + diego,
                inviteCode = "7Hk2pQ",
            )

            // When
            groups.groups.value = listOf(synced)

            // Then
            val state = viewModel.uiState.value
            assertEquals("Floripa 2026", state.groupName)
            assertEquals("borarachar.com/g/7Hk2pQ", state.displayLink)
            assertEquals(listOf("Pierre", "Ana", "Bruno", "Carla", "Diego"), state.members.map { it.name })
            assertEquals(5, state.memberCount)
        }

    @Test
    fun `GIVEN the screen on top WHEN the group disappears THEN goes back`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val noGroups = emptyList<Group>()

        // When
        groups.groups.value = noGroups

        // Then
        assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a native back already closed the screen WHEN the group disappears THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()
            navigator.navigate(Route.Profile)
            val noGroups = emptyList<Group>()

            // When
            groups.groups.value = noGroups

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Convidar opened on top WHEN the group disappears THEN leaves Convidar in place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnShareClicked)
            val noGroups = emptyList<Group>()

            // When
            groups.groups.value = noGroups

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Members("group-1"), Route.Invite("group-1")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the group gone while Convidar was on top WHEN Convidar closes THEN goes back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnShareClicked)
            groups.groups.value = emptyList()

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the screen on top WHEN tapping back THEN returns to the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = MembersUiEvent.OnBackClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen on top WHEN tapping back THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Invite("group-1"))

        // When
        viewModel.onEvent(MembersUiEvent.OnBackClicked)

        // Then
        assertEquals(
            listOf(Route.Groups, Route.Members("group-1"), Route.Invite("group-1")),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the screen on top WHEN tapping share THEN opens Convidar of the group on top`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val event = MembersUiEvent.OnShareClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Members("group-1"), Route.Invite("group-1")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN another screen on top WHEN tapping share THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Profile)

        // When
        viewModel.onEvent(MembersUiEvent.OnShareClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Members("group-1"), Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the screen open WHEN tapping another tab THEN says coming soon`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = MembersUiEvent.OnOtherTabClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf<MembersUiAction>(MembersUiAction.ShowComingSoon), actions)
    }

    @Test
    fun `GIVEN someone who has not joined WHEN tapping their personal invite THEN says coming soon`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val event = MembersUiEvent.OnPersonalInviteClicked("member-2")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf<MembersUiAction>(MembersUiAction.ShowComingSoon), actions)
            assertEquals(listOf(Route.Groups, Route.Members("group-1")), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the screen open WHEN tapping add THEN opens an empty editor that adds`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = MembersUiEvent.OnAddClicked

        // When
        viewModel.onEvent(event)

        // Then
        val editor = viewModel.uiState.value.editor
        assertEquals(
            MemberNameEditor(
                memberId = null,
                draft = "",
                takenName = null,
                nicknameSuggestions = emptyList(),
            ),
            editor,
        )
        assertTrue(editor!!.isAdding)
        assertFalse(editor.canSave)
    }

    @Test
    fun `GIVEN the screen open WHEN tapping rename on a member THEN opens the editor with their name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val event = MembersUiEvent.OnRenameClicked("member-2")

            // When
            viewModel.onEvent(event)

            // Then
            val editor = viewModel.uiState.value.editor
            assertEquals(
                MemberNameEditor(
                    memberId = "member-2",
                    draft = "Ana",
                    takenName = null,
                    nicknameSuggestions = emptyList(),
                ),
                editor,
            )
            assertFalse(editor!!.isAdding)
            assertTrue(editor.canSave)
        }

    @Test
    fun `GIVEN the screen open WHEN tapping rename on an unknown member THEN no editor opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val event = MembersUiEvent.OnRenameClicked("member-unknown")

            // When
            viewModel.onEvent(event)

            // Then
            assertNull(viewModel.uiState.value.editor)
        }

    @Test
    fun `GIVEN the add editor open WHEN typing more than 40 letters THEN keeps the first 40`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)

            // When
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("A".repeat(45)))

            // Then
            assertEquals(
                "A".repeat(40),
                viewModel.uiState.value.editor
                    ?.draft,
            )
        }

    @Test
    fun `GIVEN the add editor WHEN typing a member name in other case and spacing THEN offers nicknames`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)

            // When
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("  ana "))

            // Then
            val editor = viewModel.uiState.value.editor!!
            assertEquals("  ana ", editor.draft)
            assertEquals("Ana", editor.takenName)
            assertEquals(listOf("Ana P.", "Ana 2"), editor.nicknameSuggestions)
            assertFalse(editor.canSave)
        }

    @Test
    fun `GIVEN this device user not first WHEN typing a taken name THEN the nickname has the initial of this user`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(members = listOf(bruno, ana, pierre, carla)))
            viewModel.onEvent(MembersUiEvent.OnAddClicked)

            // When
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Ana"))

            // Then
            assertEquals(
                listOf("Ana P.", "Ana 2"),
                viewModel.uiState.value.editor
                    ?.nicknameSuggestions,
            )
        }

    @Test
    fun `GIVEN a taken name typed WHEN typing a free name THEN the name is no longer taken`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Ana"))

            // When
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Diego"))

            // Then
            val editor = viewModel.uiState.value.editor!!
            assertNull(editor.takenName)
            assertTrue(editor.nicknameSuggestions.isEmpty())
            assertTrue(editor.canSave)
        }

    @Test
    fun `GIVEN renaming a member WHEN typing their own name in other case THEN the name is not taken`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-2"))

            // When
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("ANA"))

            // Then
            val editor = viewModel.uiState.value.editor!!
            assertNull(editor.takenName)
            assertTrue(editor.canSave)
        }

    @Test
    fun `GIVEN renaming a member WHEN typing the name of another member THEN the name is taken`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-2"))

            // When
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("bruno"))

            // Then
            val editor = viewModel.uiState.value.editor!!
            assertEquals("Bruno", editor.takenName)
            assertEquals(listOf("Bruno P.", "Bruno 2"), editor.nicknameSuggestions)
            assertFalse(editor.canSave)
        }

    @Test
    fun `GIVEN a free name typed in the add editor WHEN saving THEN closes the editor and adds the name normalized`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("  Diego    Souza "))

            // When
            viewModel.onEvent(MembersUiEvent.OnSaveClicked)

            // Then
            assertNull(viewModel.uiState.value.editor)
            assertEquals(listOf("group-1"), groups.addedMembers.map { it.first })
            assertEquals(
                "Diego Souza",
                groups.addedMembers
                    .single()
                    .second.name,
            )
            assertEquals(
                "Diego Souza",
                viewModel.uiState.value.members
                    .last()
                    .name,
            )
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN a free name typed while renaming WHEN saving THEN closes the editor and renames the member`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-2"))
            viewModel.onEvent(MembersUiEvent.OnDraftChanged(" Aninha "))

            // When
            viewModel.onEvent(MembersUiEvent.OnSaveClicked)

            // Then
            assertNull(viewModel.uiState.value.editor)
            assertEquals(listOf("member-2" to "Aninha"), groups.renamedMembers)
            assertEquals(
                "Aninha",
                viewModel.uiState.value.members[1]
                    .name,
            )
            assertTrue(groups.addedMembers.isEmpty())
        }

    @Test
    fun `GIVEN this device user renaming themselves WHEN saving THEN renames them`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-1"))
        viewModel.onEvent(MembersUiEvent.OnDraftChanged("Pierre Vieira"))

        // When
        viewModel.onEvent(MembersUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf("member-1" to "Pierre Vieira"), groups.renamedMembers)
    }

    @Test
    fun `GIVEN the rename editor with the same name WHEN saving THEN closes the editor without a write`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-2"))
            viewModel.onEvent(MembersUiEvent.OnDraftChanged(" Ana "))

            // When
            viewModel.onEvent(MembersUiEvent.OnSaveClicked)

            // Then
            assertNull(viewModel.uiState.value.editor)
            assertTrue(groups.renamedMembers.isEmpty())
            assertTrue(groups.memberChangeTimes.isEmpty())
        }

    @Test
    fun `GIVEN a blank draft WHEN saving THEN keeps the editor open and writes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(MembersUiEvent.OnAddClicked)
        viewModel.onEvent(MembersUiEvent.OnDraftChanged("   "))

        // When
        viewModel.onEvent(MembersUiEvent.OnSaveClicked)

        // Then
        assertEquals(
            "   ",
            viewModel.uiState.value.editor
                ?.draft,
        )
        assertTrue(groups.addedMembers.isEmpty())
    }

    @Test
    fun `GIVEN a blank draft while renaming WHEN saving THEN keeps the editor open and writes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-2"))
            viewModel.onEvent(MembersUiEvent.OnDraftChanged(""))

            // When
            viewModel.onEvent(MembersUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                "member-2",
                viewModel.uiState.value.editor
                    ?.memberId,
            )
            assertTrue(groups.renamedMembers.isEmpty())
        }

    @Test
    fun `GIVEN a taken name typed WHEN saving THEN keeps the editor open and writes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Carla"))

            // When
            viewModel.onEvent(MembersUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                "Carla",
                viewModel.uiState.value.editor
                    ?.takenName,
            )
            assertTrue(groups.addedMembers.isEmpty())
        }

    @Test
    fun `GIVEN no editor open WHEN saving THEN writes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = MembersUiEvent.OnSaveClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertNull(viewModel.uiState.value.editor)
        assertTrue(groups.addedMembers.isEmpty())
        assertTrue(groups.renamedMembers.isEmpty())
    }

    @Test
    fun `GIVEN nicknames offered in the add editor WHEN tapping one THEN closes the editor and adds that nickname`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Ana"))

            // When
            viewModel.onEvent(MembersUiEvent.OnSuggestionClicked("Ana P."))

            // Then
            assertNull(viewModel.uiState.value.editor)
            assertEquals(listOf("Ana P."), groups.addedMembers.map { it.second.name })
        }

    @Test
    fun `GIVEN nicknames offered while renaming WHEN tapping one THEN closes the editor and renames to it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-4"))
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Ana"))

            // When
            viewModel.onEvent(MembersUiEvent.OnSuggestionClicked("Ana 2"))

            // Then
            assertNull(viewModel.uiState.value.editor)
            assertEquals(listOf("member-4" to "Ana 2"), groups.renamedMembers)
        }

    @Test
    fun `GIVEN nicknames offered WHEN tapping a name not offered THEN writes nothing and keeps the editor`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Ana"))

            // When
            viewModel.onEvent(MembersUiEvent.OnSuggestionClicked("Ana Z."))

            // Then
            assertEquals(
                "Ana",
                viewModel.uiState.value.editor
                    ?.draft,
            )
            assertTrue(groups.addedMembers.isEmpty())
        }

    @Test
    fun `GIVEN a nickname taken by a sync after it was offered WHEN tapping it THEN writes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Ana"))
            groups.groups.value = listOf(trip.copy(members = trip.members + anaP))

            // When
            viewModel.onEvent(MembersUiEvent.OnSuggestionClicked("Ana P."))

            // Then
            assertTrue(groups.addedMembers.isEmpty())
            assertEquals(
                listOf("Ana 2"),
                viewModel.uiState.value.editor
                    ?.nicknameSuggestions,
            )
        }

    @Test
    fun `GIVEN the editor open WHEN dismissing it THEN closes it without a write`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(MembersUiEvent.OnAddClicked)
        viewModel.onEvent(MembersUiEvent.OnDraftChanged("Diego"))

        // When
        viewModel.onEvent(MembersUiEvent.OnEditorDismissed)

        // Then
        assertNull(viewModel.uiState.value.editor)
        assertTrue(groups.addedMembers.isEmpty())
    }

    @Test
    fun `GIVEN a free name typed WHEN a sync adds someone with that name THEN the name becomes taken`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("diego"))

            // When
            groups.groups.value = listOf(trip.copy(members = trip.members + diego))

            // Then
            val editor = viewModel.uiState.value.editor!!
            assertEquals("diego", editor.draft)
            assertEquals("Diego", editor.takenName)
            assertEquals(listOf("Diego P.", "Diego 2"), editor.nicknameSuggestions)
        }

    @Test
    fun `GIVEN a taken name typed WHEN a sync removes the member with that name THEN the name becomes free`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Carla"))

            // When
            groups.groups.value = listOf(trip.copy(members = listOf(pierre, ana, bruno)))

            // Then
            val editor = viewModel.uiState.value.editor!!
            assertNull(editor.takenName)
            assertTrue(editor.nicknameSuggestions.isEmpty())
            assertTrue(editor.canSave)
        }

    @Test
    fun `GIVEN renaming a member WHEN a sync removes that member THEN closes the editor`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-2"))

        // When
        groups.groups.value = listOf(trip.copy(members = listOf(pierre, bruno, carla)))

        // Then
        assertNull(viewModel.uiState.value.editor)
    }

    @Test
    fun `GIVEN renaming a member WHEN a sync renames someone else THEN keeps the editor open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-2"))
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Aninha"))

            // When
            groups.groups.value = listOf(trip.copy(members = listOf(pierre, ana, bruno.copy(name = "Bruno L."), carla)))

            // Then
            assertEquals(
                MemberNameEditor(
                    memberId = "member-2",
                    draft = "Aninha",
                    takenName = null,
                    nicknameSuggestions = emptyList(),
                ),
                viewModel.uiState.value.editor,
            )
        }

    @Test
    fun `GIVEN someone who has not joined WHEN removing them THEN they leave the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-2"))

        // When
        viewModel.onEvent(MembersUiEvent.OnRemoveConfirmed)

        // Then
        assertEquals(listOf("member-2"), groups.removedMemberIds)
        assertNull(viewModel.uiState.value.memberToRemove)
        assertEquals(
            listOf("Pierre", "Bruno", "Carla"),
            viewModel.uiState.value.members
                .map { it.name },
        )
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN this device user WHEN removing them THEN nothing is removed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = MembersUiEvent.OnRemoveClicked("member-1")

        // When
        viewModel.onEvent(event)

        // Then
        assertNull(viewModel.uiState.value.memberToRemove)
        assertTrue(groups.removedMemberIds.isEmpty())
        assertEquals(4, viewModel.uiState.value.memberCount)
    }

    @Test
    fun `GIVEN someone who joined WHEN removing them THEN nothing is removed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = MembersUiEvent.OnRemoveClicked("member-3")

        // When
        viewModel.onEvent(event)

        // Then
        assertNull(viewModel.uiState.value.memberToRemove)
        assertTrue(groups.removedMemberIds.isEmpty())
        assertEquals(4, viewModel.uiState.value.memberCount)
    }

    @Test
    fun `GIVEN an unknown member WHEN removing them THEN nothing is removed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = MembersUiEvent.OnRemoveClicked("member-unknown")

        // When
        viewModel.onEvent(event)

        // Then
        assertNull(viewModel.uiState.value.memberToRemove)
        assertTrue(groups.removedMemberIds.isEmpty())
        assertTrue(groups.memberChangeTimes.isEmpty())
    }

    @Test
    fun `GIVEN someone who has not joined WHEN tapping remove THEN asks first without removing them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val event = MembersUiEvent.OnRemoveClicked("member-4")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(carlaOnScreen, viewModel.uiState.value.memberToRemove)
            assertTrue(groups.removedMemberIds.isEmpty())
            assertEquals(4, viewModel.uiState.value.memberCount)
        }

    @Test
    fun `GIVEN the removal of Carla asked WHEN dismissing it THEN closes it without removing anyone`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))

            // When
            viewModel.onEvent(MembersUiEvent.OnRemoveDismissed)

            // Then
            assertNull(viewModel.uiState.value.memberToRemove)
            assertTrue(groups.removedMemberIds.isEmpty())
            assertTrue(groups.memberChangeTimes.isEmpty())
            assertEquals(4, viewModel.uiState.value.memberCount)
        }

    @Test
    fun `GIVEN no removal asked WHEN confirming a removal THEN nothing is removed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = MembersUiEvent.OnRemoveConfirmed

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(groups.removedMemberIds.isEmpty())
        assertTrue(groups.memberChangeTimes.isEmpty())
        assertTrue(actions.isEmpty())
        assertEquals(4, viewModel.uiState.value.memberCount)
    }

    @Test
    fun `GIVEN no removal asked WHEN dismissing a removal THEN the screen stays the same`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val before = viewModel.uiState.value

        // When
        viewModel.onEvent(MembersUiEvent.OnRemoveDismissed)

        // Then
        assertEquals(before, viewModel.uiState.value)
        assertTrue(groups.removedMemberIds.isEmpty())
    }

    @Test
    fun `GIVEN the removal of Carla asked WHEN a sync removes her THEN closes the question`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))
            val withoutCarla = trip.copy(members = listOf(pierre, ana, bruno))

            // When
            groups.groups.value = listOf(withoutCarla)

            // Then
            assertNull(viewModel.uiState.value.memberToRemove)
        }

    @Test
    fun `GIVEN the removal of Carla asked and a sync removed her WHEN confirming THEN nothing is removed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))
            groups.groups.value = listOf(trip.copy(members = listOf(pierre, ana, bruno)))

            // When
            viewModel.onEvent(MembersUiEvent.OnRemoveConfirmed)

            // Then
            assertTrue(groups.removedMemberIds.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the removal of Carla asked WHEN a sync says she joined THEN closes the question`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))
            val carlaJoined = trip.copy(members = listOf(pierre, ana, bruno, carla.copy(hasJoined = true)))

            // When
            groups.groups.value = listOf(carlaJoined)

            // Then
            assertNull(viewModel.uiState.value.memberToRemove)
            assertEquals(4, viewModel.uiState.value.memberCount)
        }

    @Test
    fun `GIVEN the removal of Carla asked and a sync says she joined WHEN confirming THEN nothing is removed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))
            groups.groups.value = listOf(trip.copy(members = listOf(pierre, ana, bruno, carla.copy(hasJoined = true))))

            // When
            viewModel.onEvent(MembersUiEvent.OnRemoveConfirmed)

            // Then
            assertTrue(groups.removedMemberIds.isEmpty())
            assertEquals(4, viewModel.uiState.value.memberCount)
        }

    @Test
    fun `GIVEN the removal of Carla asked WHEN a sync renames her THEN still asks with her new name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))
            val carlaRenamed = trip.copy(members = listOf(pierre, ana, bruno, carla.copy(name = "Carla M.")))

            // When
            groups.groups.value = listOf(carlaRenamed)

            // Then
            assertEquals(
                carlaOnScreen.copy(
                    name = "Carla M.",
                    initials = "CM",
                ),
                viewModel.uiState.value.memberToRemove,
            )
        }

    @Test
    fun `GIVEN the removal of Carla asked and a sync renamed her WHEN confirming THEN removes her`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))
            groups.groups.value = listOf(trip.copy(members = listOf(pierre, ana, bruno, carla.copy(name = "Carla M."))))

            // When
            viewModel.onEvent(MembersUiEvent.OnRemoveConfirmed)

            // Then
            assertEquals(listOf("member-4"), groups.removedMemberIds)
            assertNull(viewModel.uiState.value.memberToRemove)
        }

    @Test
    fun `GIVEN the removal of Carla asked WHEN a sync adds someone else THEN still asks about Carla`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))
            val withDiego = trip.copy(members = trip.members + diego)

            // When
            groups.groups.value = listOf(withDiego)

            // Then
            assertEquals(carlaOnScreen, viewModel.uiState.value.memberToRemove)
        }

    @Test
    fun `GIVEN the device fails to save WHEN adding someone THEN says the change failed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(memberChangeError = IllegalStateException("disk full"))
        viewModel.onEvent(MembersUiEvent.OnAddClicked)
        viewModel.onEvent(MembersUiEvent.OnDraftChanged("Diego"))

        // When
        viewModel.onEvent(MembersUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf<MembersUiAction>(MembersUiAction.ShowChangeError), actions)
        assertNull(viewModel.uiState.value.editor)
        assertEquals(4, viewModel.uiState.value.memberCount)
    }

    @Test
    fun `GIVEN the device fails to save WHEN renaming someone THEN says the change failed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(memberChangeError = IllegalStateException("disk full"))
        viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-2"))
        viewModel.onEvent(MembersUiEvent.OnDraftChanged("Aninha"))

        // When
        viewModel.onEvent(MembersUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf<MembersUiAction>(MembersUiAction.ShowChangeError), actions)
        assertEquals(
            "Ana",
            viewModel.uiState.value.members[1]
                .name,
        )
    }

    @Test
    fun `GIVEN the device fails to save WHEN removing someone THEN says the change failed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(memberChangeError = IllegalStateException("disk full"))
        viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-2"))

        // When
        viewModel.onEvent(MembersUiEvent.OnRemoveConfirmed)

        // Then
        assertEquals(listOf<MembersUiAction>(MembersUiAction.ShowChangeError), actions)
        assertNull(viewModel.uiState.value.memberToRemove)
        assertEquals(4, viewModel.uiState.value.memberCount)
    }

    private val pierre = GroupMember(
        "member-1",
        "Pierre",
        AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
    )
    private val ana = GroupMember(
        "member-2",
        "Ana",
        AvatarColor.PEACH,
        isMe = false,
        hasJoined = false,
    )
    private val bruno = GroupMember(
        "member-3",
        "Bruno",
        AvatarColor.GREEN,
        isMe = false,
        hasJoined = true,
    )
    private val carla = GroupMember(
        "member-4",
        "Carla",
        AvatarColor.LILAC,
        isMe = false,
        hasJoined = false,
    )
    private val diego = GroupMember(
        "member-5",
        "Diego",
        AvatarColor.SAND,
        isMe = false,
        hasJoined = false,
    )
    private val anaP = GroupMember(
        "member-6",
        "Ana P.",
        AvatarColor.SAND,
        isMe = false,
        hasJoined = false,
    )

    private val carlaOnScreen = MembersMember(
        "member-4",
        "Carla",
        "CA",
        AvatarColor.LILAC,
        isMe = false,
        hasJoined = false,
    )

    private val trip = Group(
        id = "group-1",
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        members = listOf(pierre, ana, bruno, carla),
        isPending = false,
        inviteCode = null,
    )

    private fun TestScope.prepareScenario(
        storedGroup: Group = trip,
        isRoomRead: Boolean = true,
        memberChangeError: Throwable? = null,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup)).also {
            it.memberChangeError = memberChangeError
        }
        navigator = BackStackNavigator(startRoute = Route.Groups).also { it.navigate(Route.Members("group-1")) }
        val observeGroup = ObserveGroupUseCase(groups)
        val roomNeverAnswers = MutableSharedFlow<Group?>()
        viewModel = MembersViewModel(
            route = Route.Members("group-1"),
            navigator = navigator,
            checkMemberName = CheckMemberNameUseCase(),
            addMember = AddMemberUseCase(groups, observeGroup) { 1_000L },
            renameMember = RenameMemberUseCase(groups) { 1_000L },
            removeMember = RemoveMemberUseCase(groups) { 1_000L },
            observeGroup = if (isRoomRead) observeGroup else ObserveGroup { roomNeverAnswers },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
