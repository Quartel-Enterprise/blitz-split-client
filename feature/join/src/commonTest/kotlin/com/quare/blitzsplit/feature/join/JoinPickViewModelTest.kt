package com.quare.blitzsplit.feature.join

import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteMember
import com.quare.blitzsplit.core.groups.domain.model.JoinChoice
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CheckMemberNameUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.GetInviteUseCase
import com.quare.blitzsplit.core.groups.testing.FakeInviteRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.usecase.JoinGroup
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.NewMemberDraft
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinPickViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
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

class JoinPickViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var invites: FakeInviteRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: JoinPickViewModel
    private val actions = mutableListOf<JoinPickUiAction>()
    private var joinCalls = 0

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the invite on its way WHEN the screen opens THEN nothing is loaded`() = runTest(testDispatcher) {
        // Given
        prepareScenario(fetchGate = CompletableDeferred())

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertTrue(state.members.isEmpty())
    }

    @Test
    fun `GIVEN a valid invite WHEN the screen opens THEN splits who can be picked from who joined`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals(listOf("Bruno", "Carla"), state.notJoined.map { it.name })
            assertEquals(listOf("Pierre", "Ana"), state.joined.map { it.name })
            assertEquals("Pierre", state.creatorName)
            assertNull(state.selectedMemberId)
            assertFalse(state.canJoin)
        }

    @Test
    fun `GIVEN a link that was changed or a deleted group WHEN the screen opens THEN goes back to the invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = null)

            // When
            val backStack = navigator.backStack.value

            // Then
            assertEquals(listOf(Route.Groups, Route.JoinInvite(CODE)), backStack)
        }

    @Test
    fun `GIVEN no internet WHEN the screen opens THEN goes back to the invite`() = runTest(testDispatcher) {
        // Given
        prepareScenario(fetchError = IllegalStateException("offline"))

        // When
        val backStack = navigator.backStack.value

        // Then
        assertEquals(listOf(Route.Groups, Route.JoinInvite(CODE)), backStack)
    }

    @Test
    fun `GIVEN the screen was already closed WHEN the invite fails to load THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(
                fetchError = IllegalStateException("offline"),
                fetchGate = gate,
            )
            navigator.navigateBack()

            // When
            gate.complete(Unit)

            // Then
            assertEquals(listOf(Route.Groups, Route.JoinInvite(CODE)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the list WHEN picking Bruno THEN Bruno is selected and can join`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))

        // Then
        assertEquals(BRUNO_ID, viewModel.uiState.value.selectedMemberId)
        assertEquals("Bruno", viewModel.uiState.value.selectedName)
        assertTrue(viewModel.uiState.value.canJoin)
    }

    @Test
    fun `GIVEN Ana already joined WHEN picking Ana THEN nobody is selected`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(ANA_ID))

        // Then
        assertNull(viewModel.uiState.value.selectedMemberId)
        assertFalse(viewModel.uiState.value.canJoin)
    }

    @Test
    fun `GIVEN Bruno picked WHEN joining THEN joins as Bruno and opens the group over the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinOutcome = JoinOutcome.Joined(GROUP_ID))
            viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))

            // When
            viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

            // Then
            assertEquals(listOf(JoinChoice.AsMember(BRUNO_ID)), invites.joins.map { it.choice })
            assertEquals(listOf(CODE), invites.joins.map { it.code })
            assertEquals(
                listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = "Bruno")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the link opened over the welcome WHEN joining as Bruno THEN the welcome is gone under the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                joinOutcome = JoinOutcome.Joined(GROUP_ID),
                root = Route.Welcome,
            )
            viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))

            // When
            viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = "Bruno")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the join went through on an earlier try WHEN joining as Bruno again THEN opens the group as Bruno`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinOutcome = JoinOutcome.AlreadyMember(GROUP_ID))
            viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))

            // When
            viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = "Bruno")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Bruno taken from another device WHEN joining as Bruno THEN says so and lists him as joined`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinOutcome = JoinOutcome.MemberTaken)
            viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))
            invites.invite = trip.copy(members = listOf(pierre, ana, bruno.copy(hasJoined = true), carla))

            // When
            viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals("Bruno", state.takenName)
            assertNull(state.selectedMemberId)
            assertEquals(listOf("Carla"), state.notJoined.map { it.name })
            assertEquals(listOf("Pierre", "Ana", "Bruno"), state.joined.map { it.name })
            assertFalse(state.isJoining)
            assertEquals(2, invites.fetchedCodes.size)
            assertEquals(listOf(Route.Groups, Route.JoinInvite(CODE), Route.JoinPick(CODE)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Bruno was taken WHEN picking Carla THEN the taken notice goes away`() = runTest(testDispatcher) {
        // Given
        prepareScenario(joinOutcome = JoinOutcome.MemberTaken)
        viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))
        invites.invite = trip.copy(members = listOf(pierre, ana, bruno.copy(hasJoined = true), carla))
        viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

        // When
        viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(CARLA_ID))

        // Then
        assertNull(viewModel.uiState.value.takenName)
        assertEquals(CARLA_ID, viewModel.uiState.value.selectedMemberId)
    }

    @Test
    fun `GIVEN Bruno was removed from the group WHEN joining as Bruno THEN lists the group without him`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinOutcome = JoinOutcome.MemberGone)
            viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))
            invites.invite = trip.copy(members = listOf(pierre, ana, carla))

            // When
            viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf("Carla"), state.notJoined.map { it.name })
            assertNull(state.selectedMemberId)
            assertFalse(state.isJoining)
            assertEquals(2, invites.fetchedCodes.size)
            assertEquals(listOf(Route.Groups, Route.JoinInvite(CODE), Route.JoinPick(CODE)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the link stopped being valid WHEN joining as Bruno THEN goes back to the invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinOutcome = JoinOutcome.InvalidLink)
            viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))

            // When
            viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

            // Then
            assertEquals(listOf(Route.Groups, Route.JoinInvite(CODE)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no internet WHEN joining as Bruno THEN shows the join error and stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario(joinError = IllegalStateException("offline"))
        viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))

        // When
        viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

        // Then
        assertEquals(listOf<JoinPickUiAction>(JoinPickUiAction.ShowJoinError), actions)
        assertFalse(viewModel.uiState.value.isJoining)
        assertEquals(BRUNO_ID, viewModel.uiState.value.selectedMemberId)
        assertTrue(viewModel.uiState.value.canJoin)
        assertEquals(listOf(Route.Groups, Route.JoinInvite(CODE), Route.JoinPick(CODE)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN nobody picked WHEN joining THEN nothing is sent`() = runTest(testDispatcher) {
        // Given
        prepareScenario(joinOutcome = JoinOutcome.Joined(GROUP_ID))

        // When
        viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

        // Then
        assertTrue(invites.joins.isEmpty())
        assertFalse(viewModel.uiState.value.isJoining)
    }

    @Test
    fun `GIVEN Bruno picked WHEN the join is on its way THEN no button can join again`() = runTest(testDispatcher) {
        // Given
        prepareScenario(joinGate = CompletableDeferred())
        viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))

        // When
        viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

        // Then
        assertTrue(viewModel.uiState.value.isJoining)
        assertFalse(viewModel.uiState.value.canJoin)
    }

    @Test
    fun `GIVEN a join on its way WHEN joining again THEN joins only once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(joinGate = CompletableDeferred())
        viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))
        viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

        // When
        viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

        // Then
        assertEquals(1, joinCalls)
    }

    @Test
    fun `GIVEN a join as Bruno on its way WHEN picking Carla THEN Bruno stays selected`() = runTest(testDispatcher) {
        // Given
        prepareScenario(joinGate = CompletableDeferred())
        viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))
        viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

        // When
        viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(CARLA_ID))

        // Then
        assertEquals(BRUNO_ID, viewModel.uiState.value.selectedMemberId)
    }

    @Test
    fun `GIVEN the screen was closed while joining WHEN the join goes through THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(
                joinOutcome = JoinOutcome.Joined(GROUP_ID),
                joinGate = gate,
            )
            viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))
            viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)
            navigator.navigateBack()

            // When
            gate.complete(Unit)

            // Then
            assertEquals(listOf(Route.Groups, Route.JoinInvite(CODE)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the list WHEN tapping not in the list THEN opens an empty new member draft`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

        // Then
        assertEquals(
            NewMemberDraft(
                name = "",
                isNameTaken = false,
            ),
            viewModel.uiState.value.newMember,
        )
        assertFalse(viewModel.uiState.value.canJoinAsNewMember)
    }

    @Test
    fun `GIVEN the new member draft WHEN typing a name nobody has THEN can join with it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

        // When
        viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Daniel"))

        // Then
        assertEquals(
            NewMemberDraft(
                name = "Daniel",
                isNameTaken = false,
            ),
            viewModel.uiState.value.newMember,
        )
        assertTrue(viewModel.uiState.value.canJoinAsNewMember)
    }

    @Test
    fun `GIVEN the new member draft WHEN typing Ana in other case and spacing THEN the name is taken`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("  aNA "))

            // Then
            assertTrue(
                viewModel.uiState.value.newMember
                    ?.isNameTaken == true,
            )
            assertFalse(viewModel.uiState.value.canJoinAsNewMember)
        }

    @Test
    fun `GIVEN the new member draft WHEN typing only spaces THEN cannot join`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

        // When
        viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("   "))

        // Then
        assertFalse(
            viewModel.uiState.value.newMember
                ?.isNameTaken == true,
        )
        assertFalse(viewModel.uiState.value.canJoinAsNewMember)
    }

    @Test
    fun `GIVEN the new member draft WHEN typing past the name limit THEN keeps the first 40 letters`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("D".repeat(45)))

            // Then
            assertEquals(
                "D".repeat(40),
                viewModel.uiState.value.newMember
                    ?.name,
            )
        }

    @Test
    fun `GIVEN a new name with spaces around WHEN confirming THEN joins as the trimmed name and opens the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinOutcome = JoinOutcome.Joined(GROUP_ID))
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
            viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("  Daniel  "))

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)

            // Then
            assertEquals(listOf(JoinChoice.AsNewMember("Daniel")), invites.joins.map { it.choice })
            assertEquals(
                listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = "Daniel")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a name already in the group WHEN confirming THEN nothing is sent`() = runTest(testDispatcher) {
        // Given
        prepareScenario(joinOutcome = JoinOutcome.Joined(GROUP_ID))
        viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
        viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Ana"))

        // When
        viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)

        // Then
        assertTrue(invites.joins.isEmpty())
    }

    @Test
    fun `GIVEN someone joined as Daniel meanwhile WHEN confirming Daniel THEN flags the name as taken and reloads`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinOutcome = JoinOutcome.NameTaken)
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
            viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Daniel"))
            val daniel = InviteMember("member-5", "Daniel", AvatarColor.SAND, hasJoined = true)
            invites.invite = trip.copy(members = trip.members + daniel)

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)

            // Then
            assertEquals(
                NewMemberDraft(
                    name = "Daniel",
                    isNameTaken = true,
                ),
                viewModel.uiState.value.newMember,
            )
            assertFalse(viewModel.uiState.value.canJoinAsNewMember)
            assertEquals(2, invites.fetchedCodes.size)
            assertEquals(
                listOf("Pierre", "Ana", "Daniel"),
                viewModel.uiState.value.joined
                    .map { it.name },
            )
        }

    @Test
    fun `GIVEN the new member draft WHEN dismissing it THEN closes the draft`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
        viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Daniel"))

        // When
        viewModel.onEvent(JoinPickUiEvent.OnNewMemberDismissed)

        // Then
        assertNull(viewModel.uiState.value.newMember)
    }

    @Test
    fun `GIVEN the list WHEN tapping back THEN goes back to the invite`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(JoinPickUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.JoinInvite(CODE)), navigator.backStack.value)
    }

    private val pierre = InviteMember("member-1", "Pierre", AvatarColor.BLUE, hasJoined = true)
    private val ana = InviteMember(ANA_ID, "Ana", AvatarColor.PEACH, hasJoined = true)
    private val bruno = InviteMember(BRUNO_ID, "Bruno", AvatarColor.GREEN, hasJoined = false)
    private val carla = InviteMember(CARLA_ID, "Carla", AvatarColor.LILAC, hasJoined = false)

    private val trip = Invite(
        code = CODE,
        groupId = GROUP_ID,
        groupName = "Viagem Floripa",
        groupType = GroupType.TRIP,
        groupColor = AvatarColor.GREEN,
        currency = GroupCurrency.BRL,
        creatorName = "Pierre",
        isMember = false,
        members = listOf(pierre, ana, bruno, carla),
    )

    private fun TestScope.prepareScenario(
        invite: Invite? = trip,
        fetchError: Throwable? = null,
        fetchGate: CompletableDeferred<Unit>? = null,
        joinOutcome: JoinOutcome = JoinOutcome.Joined(GROUP_ID),
        joinError: Throwable? = null,
        joinGate: CompletableDeferred<Unit>? = null,
        root: Route = Route.Groups,
    ) {
        invites = FakeInviteRepository(
            invite = invite,
            joinOutcome = joinOutcome,
        ).also {
            it.fetchError = fetchError
            it.fetchGate = fetchGate
            it.joinError = joinError
            it.joinGate = joinGate
        }
        navigator = BackStackNavigator(startRoute = root).also {
            it.navigate(Route.JoinInvite(CODE))
            it.navigate(Route.JoinPick(CODE))
        }
        viewModel = JoinPickViewModel(
            route = Route.JoinPick(CODE),
            navigator = navigator,
            getInvite = GetInviteUseCase(invites),
            joinGroup = JoinGroup { joined, choice ->
                joinCalls++
                invites.joinGroup(joined.code, choice, AvatarColor.default)
            },
            checkMemberName = CheckMemberNameUseCase(),
        )
        actions.clear()
        joinCalls = 0
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val CODE = "7Hk2pQxZ"
        const val GROUP_ID = "group-1"
        const val ANA_ID = "member-2"
        const val BRUNO_ID = "member-3"
        const val CARLA_ID = "member-4"
    }
}
