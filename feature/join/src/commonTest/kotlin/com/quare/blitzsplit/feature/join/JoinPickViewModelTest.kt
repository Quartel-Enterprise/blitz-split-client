package com.quare.blitzsplit.feature.join

import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteMember
import com.quare.blitzsplit.core.groups.domain.model.InviteTarget
import com.quare.blitzsplit.core.groups.domain.model.JoinChoice
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CheckMemberNameUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.GetInviteUseCase
import com.quare.blitzsplit.core.groups.testing.FakeInviteRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.usecase.JoinGroup
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.NewMemberDraft
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinPickViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var invites: FakeInviteRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var accountStatuses: MutableStateFlow<AccountStatus>
    private lateinit var profiles: MutableStateFlow<UserProfile?>
    private lateinit var viewModel: JoinPickViewModel
    private val actions = mutableListOf<JoinPickUiAction>()
    private val selectedWhenTracked = mutableListOf<String?>()
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
            assertEquals("Viagem Floripa", state.groupName)
            assertNull(state.selectedMemberId)
            assertFalse(state.canJoin)
        }

    @Test
    fun `GIVEN the list WHEN the screen opens THEN reads the group invite with no member code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val targets = invites.fetchedTargets

            // Then
            assertEquals(
                listOf(
                    InviteTarget(
                        CODE,
                        memberCode = null,
                        inviterCode = null,
                    ),
                ),
                targets,
            )
        }

    @Test
    fun `GIVEN a link that was changed or a deleted group WHEN the screen opens THEN goes back to the invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = null)

            // When
            val backStack = navigator.backStack.value

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.JoinInvite(
                        CODE,
                        memberCode = null,
                        inviterCode = null,
                    ),
                ),
                backStack,
            )
        }

    @Test
    fun `GIVEN no internet WHEN the screen opens THEN goes back to the invite`() = runTest(testDispatcher) {
        // Given
        prepareScenario(fetchError = IllegalStateException("offline"))

        // When
        val backStack = navigator.backStack.value

        // Then
        assertEquals(
            listOf(
                Route.Home,
                Route.JoinInvite(
                    CODE,
                    memberCode = null,
                    inviterCode = null,
                ),
            ),
            backStack,
        )
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
            assertEquals(
                listOf(
                    Route.Home,
                    Route.JoinInvite(
                        CODE,
                        memberCode = null,
                        inviterCode = null,
                    ),
                ),
                navigator.backStack.value,
            )
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
                listOf(
                    Route.Home,
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = "Bruno",
                        tab = GroupTab.EXPENSES,
                    ),
                ),
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
                listOf(
                    Route.Home,
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = "Bruno",
                        tab = GroupTab.EXPENSES,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the join went through on an earlier try WHEN joining as Bruno again THEN opens the group as Bruno`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinOutcome = JoinOutcome.AlreadyMember(GROUP_ID, memberId = BRUNO_ID))
            viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))

            // When
            viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = "Bruno",
                        tab = GroupTab.EXPENSES,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the account joined as Bruno on an earlier try WHEN joining as Carla THEN opens the group as Bruno`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinOutcome = JoinOutcome.AlreadyMember(GROUP_ID, memberId = BRUNO_ID))
            viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(CARLA_ID))

            // When
            viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = "Bruno",
                        tab = GroupTab.EXPENSES,
                    ),
                ),
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
            assertEquals(
                listOf(
                    Route.Home,
                    Route.JoinInvite(
                        CODE,
                        memberCode = null,
                        inviterCode = null,
                    ),
                    Route.JoinPick(CODE),
                ),
                navigator.backStack.value,
            )
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
            assertEquals(
                listOf(
                    Route.Home,
                    Route.JoinInvite(
                        CODE,
                        memberCode = null,
                        inviterCode = null,
                    ),
                    Route.JoinPick(CODE),
                ),
                navigator.backStack.value,
            )
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
            assertEquals(
                listOf(
                    Route.Home,
                    Route.JoinInvite(
                        CODE,
                        memberCode = null,
                        inviterCode = null,
                    ),
                ),
                navigator.backStack.value,
            )
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
        assertEquals(
            listOf(
                Route.Home,
                Route.JoinInvite(
                    CODE,
                    memberCode = null,
                    inviterCode = null,
                ),
                Route.JoinPick(CODE),
            ),
            navigator.backStack.value,
        )
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
            assertEquals(
                listOf(
                    Route.Home,
                    Route.JoinInvite(
                        CODE,
                        memberCode = null,
                        inviterCode = null,
                    ),
                ),
                navigator.backStack.value,
            )
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
            ),
            viewModel.uiState.value.newMember,
        )
        assertTrue(viewModel.uiState.value.canJoinAsNewMember)
    }

    @Test
    fun `GIVEN the new member draft WHEN typing only spaces THEN cannot join`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

        // When
        viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("   "))

        // Then
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
                listOf(
                    Route.Home,
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = "Daniel",
                        tab = GroupTab.EXPENSES,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a name already in the group WHEN confirming THEN joins as a new member with that name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinOutcome = JoinOutcome.Joined(GROUP_ID))
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
            viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged(" aNA "))

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)

            // Then
            assertEquals(listOf(JoinChoice.AsNewMember("aNA")), invites.joins.map { it.choice })
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
        assertEquals(
            listOf(
                Route.Home,
                Route.JoinInvite(
                    CODE,
                    memberCode = null,
                    inviterCode = null,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the list WHEN tapping back THEN sends no analytics event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(JoinPickUiEvent.OnBackClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the list WHEN picking Bruno THEN sends the member selected event without his id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))

            // Then
            assertEquals(listOf("join_pick_member_selected" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the list WHEN picking Bruno THEN sends the event before he shows picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))

        // Then
        assertEquals(listOf<String?>(null), selectedWhenTracked)
        assertEquals(BRUNO_ID, viewModel.uiState.value.selectedMemberId)
    }

    @Test
    fun `GIVEN Ana already joined WHEN picking her THEN still sends the member selected event`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(ANA_ID))

            // Then
            assertEquals(listOf("join_pick_member_selected" to emptyMap<String, Any>()), trackedEvents)
            assertNull(viewModel.uiState.value.selectedMemberId)
        }

    @Test
    fun `GIVEN Bruno picked WHEN joining THEN sends the join event after the pick`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))

        // When
        viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

        // Then
        assertEquals(
            listOf(
                "join_pick_member_selected" to emptyMap<String, Any>(),
                "join_pick_join_clicked" to emptyMap(),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN nobody picked WHEN joining THEN still sends the join event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

        // Then
        assertEquals(listOf("join_pick_join_clicked" to emptyMap<String, Any>()), trackedEvents)
        assertEquals(0, joinCalls)
    }

    @Test
    fun `GIVEN the list WHEN tapping not in the list THEN sends the not in list event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

        // Then
        assertEquals(listOf("join_pick_not_in_list_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the new name sheet WHEN typing a name THEN sends no analytics event for it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

        // When
        viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Daniel"))

        // Then
        assertEquals(listOf("join_pick_not_in_list_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN Daniel typed WHEN confirming the new member THEN sends the confirmed event without the name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
            viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Daniel"))

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)

            // Then
            assertEquals(
                listOf(
                    "join_pick_not_in_list_clicked" to emptyMap<String, Any>(),
                    "join_pick_new_member_confirmed" to emptyMap(),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a name already in the group WHEN confirming the new member THEN sends the confirmed event and joins`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
            viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Ana"))

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)

            // Then
            assertEquals(
                listOf(
                    "join_pick_not_in_list_clicked" to emptyMap<String, Any>(),
                    "join_pick_new_member_confirmed" to emptyMap(),
                ),
                trackedEvents,
            )
            assertEquals(1, joinCalls)
        }

    @Test
    fun `GIVEN the new name sheet WHEN dismissing it THEN sends no analytics event for it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

        // When
        viewModel.onEvent(JoinPickUiEvent.OnNewMemberDismissed)

        // Then
        assertEquals(listOf("join_pick_not_in_list_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN members with photos WHEN the screen opens THEN each member keeps the photo of whoever joined as them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                invite = trip.copy(
                    members = listOf(
                        pierre.copy(photoUrl = PIERRE_PHOTO),
                        ana,
                        bruno,
                        carla,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(PIERRE_PHOTO, null, null, null), state.members.map { it.photoUrl })
        }

    @Test
    fun `GIVEN a Google account named Daniel WHEN tapping not in the list THEN joins as Daniel and opens the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = google,
                profile = profile("Daniel"),
            )

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            assertEquals(listOf(JoinChoice.AsNewMember("Daniel")), invites.joins.map { it.choice })
            assertEquals(listOf(CODE), invites.joins.map { it.code })
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = "Daniel",
                        tab = GroupTab.EXPENSES,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a Google profile still being read WHEN tapping not in the list twice THEN joins once when it comes`() =
        runTest(testDispatcher) {
            // Given
            val profileGate = CompletableDeferred<Unit>()
            prepareScenario(
                accountStatus = google,
                profile = profile("Daniel"),
                profileGate = profileGate,
            )
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // When
            profileGate.complete(Unit)

            // Then
            assertNull(viewModel.uiState.value.newMember)
            assertEquals(listOf(JoinChoice.AsNewMember("Daniel")), invites.joins.map { it.choice })
        }

    @Test
    fun `GIVEN a Google account named Daniel WHEN tapping not in the list THEN the new name sheet never opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = google,
                profile = profile("Daniel"),
                joinGate = CompletableDeferred(),
            )

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            assertNull(viewModel.uiState.value.newMember)
        }

    @Test
    fun `GIVEN an Apple account named Daniel WHEN tapping not in the list THEN joins as Daniel`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = apple,
                profile = profile("Daniel"),
            )

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            assertEquals(listOf(JoinChoice.AsNewMember("Daniel")), invites.joins.map { it.choice })
            assertNull(viewModel.uiState.value.newMember)
        }

    @Test
    fun `GIVEN a Google name with extra spaces WHEN tapping not in the list THEN joins trimmed with single spaces`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = google,
                profile = profile("  Daniel   Souza "),
            )

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            assertEquals(listOf(JoinChoice.AsNewMember("Daniel Souza")), invites.joins.map { it.choice })
        }

    @Test
    fun `GIVEN a Google name past the limit WHEN tapping not in the list THEN joins with the first 40 letters`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = google,
                profile = profile("D".repeat(45)),
            )

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            assertEquals(listOf(JoinChoice.AsNewMember("D".repeat(40))), invites.joins.map { it.choice })
        }

    @Test
    fun `GIVEN an anonymous account with a profile WHEN tapping not in the list THEN opens an empty draft`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = anonymous,
                profile = profile("Daniel"),
            )

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            assertEquals(NewMemberDraft(name = ""), viewModel.uiState.value.newMember)
            assertEquals(0, joinCalls)
        }

    @Test
    fun `GIVEN a local only account with a profile WHEN tapping not in the list THEN opens an empty draft`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = AccountStatus.LocalOnly,
                profile = profile("Daniel"),
            )

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            assertEquals(NewMemberDraft(name = ""), viewModel.uiState.value.newMember)
            assertEquals(0, joinCalls)
        }

    @Test
    fun `GIVEN a Google account with no profile WHEN tapping not in the list THEN opens an empty draft`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = google,
                profile = null,
            )

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            assertEquals(NewMemberDraft(name = ""), viewModel.uiState.value.newMember)
            assertEquals(0, joinCalls)
        }

    @Test
    fun `GIVEN a Google account named with a single letter WHEN tapping not in the list THEN opens an empty draft`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = google,
                profile = profile(" D "),
            )

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            assertEquals(NewMemberDraft(name = ""), viewModel.uiState.value.newMember)
            assertEquals(0, joinCalls)
        }

    @Test
    fun `GIVEN Google connected while the list is open WHEN tapping not in the list THEN joins as the profile name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = AccountStatus.LocalOnly,
                profile = null,
            )
            accountStatuses.value = google
            profiles.value = profile("Daniel")

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            assertEquals(listOf(JoinChoice.AsNewMember("Daniel")), invites.joins.map { it.choice })
            assertNull(viewModel.uiState.value.newMember)
        }

    @Test
    fun `GIVEN the profile renamed while the list is open WHEN tapping not in the list THEN joins as the new name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = google,
                profile = profile("Daniel"),
            )
            profiles.value = profile("Dani")

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            assertEquals(listOf(JoinChoice.AsNewMember("Dani")), invites.joins.map { it.choice })
        }

    @Test
    fun `GIVEN Google signed out while the list is open WHEN tapping not in the list THEN opens an empty draft`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = google,
                profile = profile("Daniel"),
            )
            accountStatuses.value = anonymous

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            assertEquals(NewMemberDraft(name = ""), viewModel.uiState.value.newMember)
            assertEquals(0, joinCalls)
        }

    @Test
    fun `GIVEN a Google account join on its way WHEN reading the screen THEN it shows a new member join`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = google,
                profile = profile("Daniel"),
                joinGate = CompletableDeferred(),
            )
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isJoining)
            assertTrue(state.isJoiningAsNewMember)
            assertFalse(state.canJoin)
        }

    @Test
    fun `GIVEN a Google account join on its way WHEN tapping not in the list again THEN joins only once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = google,
                profile = profile("Daniel"),
                joinGate = CompletableDeferred(),
            )
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            assertEquals(1, joinCalls)
            assertNull(viewModel.uiState.value.newMember)
        }

    @Test
    fun `GIVEN Google and no internet WHEN tapping not in the list THEN shows the join error and stays usable`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = google,
                profile = profile("Daniel"),
                joinError = IllegalStateException("offline"),
            )

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf<JoinPickUiAction>(JoinPickUiAction.ShowJoinError), actions)
            assertFalse(state.isJoining)
            assertFalse(state.isJoiningAsNewMember)
            assertNull(state.newMember)
            assertEquals(
                listOf(
                    Route.Home,
                    Route.JoinInvite(
                        CODE,
                        memberCode = null,
                        inviterCode = null,
                    ),
                    Route.JoinPick(CODE),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a Google account join failed WHEN tapping not in the list again THEN joins again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                accountStatus = google,
                profile = profile("Daniel"),
                joinError = IllegalStateException("offline"),
            )
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)

            // Then
            assertEquals(2, joinCalls)
        }

    @Test
    fun `GIVEN a join as Bruno on its way WHEN reading the screen THEN it is not a new member join`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinGate = CompletableDeferred())
            viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))
            viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isJoining)
            assertFalse(state.isJoiningAsNewMember)
        }

    @Test
    fun `GIVEN Daniel typed WHEN the new member join is on its way THEN it shows a new member join`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinGate = CompletableDeferred())
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
            viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Daniel"))

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isJoining)
            assertTrue(state.isJoiningAsNewMember)
            assertFalse(state.canJoinAsNewMember)
        }

    @Test
    fun `GIVEN a new member join on its way WHEN it ends without opening the group THEN nothing shows a join`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(
                joinOutcome = JoinOutcome.MemberGone,
                joinGate = gate,
            )
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
            viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Daniel"))
            viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)

            // When
            gate.complete(Unit)

            // Then
            assertFalse(viewModel.uiState.value.isJoining)
            assertFalse(viewModel.uiState.value.isJoiningAsNewMember)
        }

    @Test
    fun `GIVEN a join as Bruno on its way WHEN Bruno turns out taken THEN nothing shows a join`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(
                joinOutcome = JoinOutcome.MemberTaken,
                joinGate = gate,
            )
            viewModel.onEvent(JoinPickUiEvent.OnMemberSelected(BRUNO_ID))
            viewModel.onEvent(JoinPickUiEvent.OnJoinClicked)

            // When
            gate.complete(Unit)

            // Then
            assertFalse(viewModel.uiState.value.isJoining)
            assertFalse(viewModel.uiState.value.isJoiningAsNewMember)
        }

    @Test
    fun `GIVEN a new member join on its way WHEN dismissing the sheet THEN the sheet stays with Daniel`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinGate = CompletableDeferred())
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
            viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Daniel"))
            viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNewMemberDismissed)

            // Then
            assertEquals(NewMemberDraft(name = "Daniel"), viewModel.uiState.value.newMember)
        }

    @Test
    fun `GIVEN a new member join on its way WHEN typing another name THEN the name sent stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinGate = CompletableDeferred())
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
            viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Daniel"))
            viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Danilo"))

            // Then
            assertEquals(NewMemberDraft(name = "Daniel"), viewModel.uiState.value.newMember)
        }

    @Test
    fun `GIVEN a new member join on its way WHEN confirming again THEN joins only once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(joinGate = CompletableDeferred())
        viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
        viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Daniel"))
        viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)

        // When
        viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)

        // Then
        assertEquals(1, joinCalls)
    }

    @Test
    fun `GIVEN no internet WHEN confirming Daniel THEN shows the join error and keeps the sheet with Daniel`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinError = IllegalStateException("offline"))
            viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
            viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Daniel"))

            // When
            viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf<JoinPickUiAction>(JoinPickUiAction.ShowJoinError), actions)
            assertEquals(NewMemberDraft(name = "Daniel"), state.newMember)
            assertFalse(state.isJoining)
            assertFalse(state.isJoiningAsNewMember)
            assertTrue(state.canJoinAsNewMember)
        }

    @Test
    fun `GIVEN a new member join failed WHEN dismissing the sheet THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(joinError = IllegalStateException("offline"))
        viewModel.onEvent(JoinPickUiEvent.OnNotInListClicked)
        viewModel.onEvent(JoinPickUiEvent.OnNewNameChanged("Daniel"))
        viewModel.onEvent(JoinPickUiEvent.OnNewMemberConfirmed)

        // When
        viewModel.onEvent(JoinPickUiEvent.OnNewMemberDismissed)

        // Then
        assertNull(viewModel.uiState.value.newMember)
    }

    private val pierre = InviteMember(
        "member-1",
        "Pierre",
        AvatarColor.BLUE,
        hasJoined = true,
        photoUrl = null,
    )
    private val ana = InviteMember(
        ANA_ID,
        "Ana",
        AvatarColor.PEACH,
        hasJoined = true,
        photoUrl = null,
    )
    private val bruno = InviteMember(
        BRUNO_ID,
        "Bruno",
        AvatarColor.GREEN,
        hasJoined = false,
        photoUrl = null,
    )
    private val carla = InviteMember(
        CARLA_ID,
        "Carla",
        AvatarColor.LILAC,
        hasJoined = false,
        photoUrl = null,
    )

    private val anonymous = AccountStatus.Anonymous(userId = "user-1")
    private val google = AccountStatus.Connected(
        userId = "user-1",
        provider = AccountProvider.GOOGLE,
    )
    private val apple = AccountStatus.Connected(
        userId = "user-1",
        provider = AccountProvider.APPLE,
    )

    private fun profile(displayName: String): UserProfile = UserProfile(
        displayName = displayName,
        avatarColor = AvatarColor.BLUE,
        photo = null,
    )

    private val trip = Invite(
        code = CODE,
        groupId = GROUP_ID,
        groupName = "Viagem Floripa",
        groupType = GroupType.TRIP,
        groupColor = AvatarColor.GREEN,
        groupPhotoUrl = null,
        currency = GroupCurrency.BRL,
        creatorName = "Pierre",
        isMember = false,
        members = listOf(pierre, ana, bruno, carla),
        personalMemberId = null,
        inviterName = null,
    )

    private fun TestScope.prepareScenario(
        invite: Invite? = trip,
        fetchError: Throwable? = null,
        fetchGate: CompletableDeferred<Unit>? = null,
        joinOutcome: JoinOutcome = JoinOutcome.Joined(GROUP_ID),
        joinError: Throwable? = null,
        joinGate: CompletableDeferred<Unit>? = null,
        root: Route = Route.Home,
        accountStatus: AccountStatus = anonymous,
        profile: UserProfile? = null,
        profileGate: CompletableDeferred<Unit>? = null,
    ) {
        accountStatuses = MutableStateFlow(accountStatus)
        profiles = MutableStateFlow(profile)
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
            it.navigate(
                Route.JoinInvite(
                    CODE,
                    memberCode = null,
                    inviterCode = null,
                ),
            )
            it.navigate(Route.JoinPick(CODE))
        }
        viewModel = JoinPickViewModel(
            trackEvent = { name, params ->
                trackedEvents += name to params
                selectedWhenTracked += viewModel.uiState.value.selectedMemberId
            },
            route = Route.JoinPick(CODE),
            navigator = navigator,
            getInvite = GetInviteUseCase(invites),
            joinGroup = JoinGroup { joined, choice ->
                joinCalls++
                invites.joinGroup(joined.code, choice, AvatarColor.default)
            },
            checkMemberName = CheckMemberNameUseCase(),
            observeAccountStatus = { accountStatuses },
            observeProfile = {
                profileGate?.let { gate ->
                    flow {
                        gate.await()
                        emitAll(profiles)
                    }
                } ?: profiles
            },
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
        const val PIERRE_PHOTO = "https://cdn.blitzsplit.com/avatars/pierre.jpg"
    }
}
