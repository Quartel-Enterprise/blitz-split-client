package com.quare.blitzsplit.feature.join

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteMember
import com.quare.blitzsplit.core.groups.domain.model.InviteTarget
import com.quare.blitzsplit.core.groups.domain.model.JoinChoice
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.groups.domain.usecase.impl.GetInviteUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.groups.testing.FakeInviteRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.usecase.JoinGroup
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteStatus
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinMember
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinInviteViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JoinInviteViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var invites: FakeInviteRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: JoinInviteViewModel
    private val actions = mutableListOf<JoinInviteUiAction>()
    private var syncCount = 0
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
    fun `GIVEN the invite on its way WHEN the screen opens THEN shows loading with the link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(fetchGate = CompletableDeferred())

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(JoinInviteStatus.LOADING, state.status)
            assertEquals("blitzsplit.com/g/$CODE", state.displayLink)
            assertEquals(listOf(CODE), invites.fetchedCodes)
        }

    @Test
    fun `GIVEN a valid invite WHEN the screen opens THEN shows the group its creator and members in order`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = trip)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(JoinInviteStatus.READY, state.status)
            assertEquals("Viagem Floripa", state.groupName)
            assertEquals("VF", state.groupInitials)
            assertEquals(AvatarColor.GREEN, state.groupColor)
            assertEquals("Pierre", state.creatorName)
            assertEquals(
                listOf(
                    JoinMember("member-1", "Pierre", "PI", AvatarColor.BLUE, hasJoined = true),
                    JoinMember("member-2", "Ana", "AN", AvatarColor.PEACH, hasJoined = true),
                    JoinMember("member-3", "Bruno", "BR", AvatarColor.GREEN, hasJoined = false),
                    JoinMember("member-4", "Carla", "CA", AvatarColor.LILAC, hasJoined = false),
                ),
                state.members,
            )
            assertEquals("blitzsplit.com/g/$CODE", state.displayLink)
            assertEquals(
                listOf(
                    Route.Groups,
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
    fun `GIVEN the creator deleted the account WHEN the screen opens THEN shows the invite with no creator`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = trip.copy(creatorName = null))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(JoinInviteStatus.READY, state.status)
            assertNull(state.creatorName)
        }

    @Test
    fun `GIVEN a link that was changed or a deleted group WHEN the screen opens THEN shows the invite is invalid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(JoinInviteStatus.INVALID, state.status)
        }

    @Test
    fun `GIVEN no internet WHEN the screen opens THEN shows offline keeping the link`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            online = false,
            fetchError = IllegalStateException("offline"),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(JoinInviteStatus.OFFLINE, state.status)
        assertEquals("blitzsplit.com/g/$CODE", state.displayLink)
    }

    @Test
    fun `GIVEN the invite could not open offline WHEN the device goes online THEN shows the invite by itself`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                fetchError = IllegalStateException("offline"),
            )
            invites.fetchError = null

            // When
            isOnline.value = true

            // Then
            assertEquals(JoinInviteStatus.READY, viewModel.uiState.value.status)
            assertEquals("Viagem Floripa", viewModel.uiState.value.groupName)
        }

    @Test
    fun `GIVEN the invite on screen WHEN the connection drops and comes back THEN does not read it again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = true)
            isOnline.value = false
            val fetchesBefore = invites.fetchedCodes.size

            // When
            isOnline.value = true

            // Then
            assertEquals(fetchesBefore, invites.fetchedCodes.size)
            assertEquals(JoinInviteStatus.READY, viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN the invite could not open WHEN trying again THEN reads it again and shows it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                fetchError = IllegalStateException("server down"),
            )
            invites.fetchError = null
            val fetchesBefore = invites.fetchedCodes.size

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnRetryClicked)

            // Then
            assertEquals(fetchesBefore + 1, invites.fetchedCodes.size)
            assertEquals(JoinInviteStatus.READY, viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN the invite could not open WHEN trying again while it loads THEN shows loading`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                fetchError = IllegalStateException("server down"),
            )
            invites.fetchError = null
            invites.fetchGate = CompletableDeferred()

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnRetryClicked)

            // Then
            assertEquals(JoinInviteStatus.LOADING, viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN the invite on screen WHEN trying again THEN does not read it again`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val fetchesBefore = invites.fetchedCodes.size

        // When
        viewModel.onEvent(JoinInviteUiEvent.OnRetryClicked)

        // Then
        assertEquals(fetchesBefore, invites.fetchedCodes.size)
        assertEquals(JoinInviteStatus.READY, viewModel.uiState.value.status)
    }

    @Test
    fun `GIVEN a member with the group on the device WHEN the link opens THEN opens the group in its place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                invite = trip.copy(isMember = true),
                storedGroup = tripGroup,
            )

            // When
            val backStack = navigator.backStack.value

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), backStack)
            assertEquals(0, syncCount)
        }

    @Test
    fun `GIVEN this account already joined from another device WHEN the link opens THEN syncs then opens the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                invite = trip.copy(isMember = true),
                storedGroup = null,
                syncBrings = tripGroup,
            )

            // When
            val backStack = navigator.backStack.value

            // Then
            assertEquals(1, syncCount)
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), backStack)
        }

    @Test
    fun `GIVEN a member offline with the group not on the device WHEN the link opens THEN shows offline`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                invite = trip.copy(isMember = true),
                online = false,
                storedGroup = null,
                syncBrings = null,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(1, syncCount)
            assertEquals(JoinInviteStatus.OFFLINE, state.status)
            assertEquals(
                listOf(
                    Route.Groups,
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
    fun `GIVEN a valid invite WHEN joining THEN opens who are you in the group on top`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

        // Then
        assertEquals(
            listOf(
                Route.Groups,
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
    fun `GIVEN an invalid invite WHEN joining THEN stays on the invite`() = runTest(testDispatcher) {
        // Given
        prepareScenario(invite = null)

        // When
        viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

        // Then
        assertEquals(
            listOf(
                Route.Groups,
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
    fun `GIVEN who are you in the group was open WHEN going back to the invite THEN reads it again without loading`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)
            invites.fetchGate = CompletableDeferred()
            val fetchesBefore = invites.fetchedCodes.size

            // When
            navigator.navigateBack()

            // Then
            assertEquals(fetchesBefore + 1, invites.fetchedCodes.size)
            assertEquals(JoinInviteStatus.READY, viewModel.uiState.value.status)
            assertEquals("Viagem Floripa", viewModel.uiState.value.groupName)
        }

    @Test
    fun `GIVEN Bruno joined while picking WHEN going back to the invite THEN lists him as joined`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)
            invites.invite = trip.copy(members = listOf(pierre, ana, bruno.copy(hasJoined = true), carla))

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf("Pierre", "Ana", "Bruno"), viewModel.uiState.value.joinedNames)
            assertEquals(JoinInviteStatus.READY, viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN the invite on top WHEN tapping not now THEN leaves the invite`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(JoinInviteUiEvent.OnNotNowClicked)

        // Then
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen over the invite WHEN tapping not now THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnNotNowClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
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
    fun `GIVEN an invalid invite over other screens WHEN going home THEN pops back to the root`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                invite = null,
                under = listOf(Route.Groups, Route.Profile),
            )

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnGoHomeClicked)

            // Then
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN another screen over the invite WHEN going home THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

        // When
        viewModel.onEvent(JoinInviteUiEvent.OnGoHomeClicked)

        // Then
        assertEquals(
            listOf(
                Route.Groups,
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
    fun `GIVEN an invalid invite on top WHEN scanning another QR THEN the reader takes the place of the invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = null)

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnScanAnotherClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.JoinWithLink(
                        skippedCode = CODE,
                        skippedMemberCode = null,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN an invalid personal link on top WHEN scanning another QR THEN the reader takes its place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = null,
            )

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnScanAnotherClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.JoinWithLink(
                        skippedCode = CODE,
                        skippedMemberCode = MEMBER_CODE,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN an invalid link opened over the reader WHEN scanning another QR THEN goes back to that reader`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                invite = null,
                under = listOf(Route.Groups, reader),
            )

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnScanAnotherClicked)

            // Then
            assertEquals(listOf(Route.Groups, reader), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a valid invite WHEN scanning another QR THEN stays on the invite`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(JoinInviteUiEvent.OnScanAnotherClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupLink), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the invite could not open offline WHEN scanning another QR THEN stays on the invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                fetchError = IllegalStateException("offline"),
            )

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnScanAnotherClicked)

            // Then
            assertEquals(JoinInviteStatus.OFFLINE, viewModel.uiState.value.status)
            assertEquals(listOf(Route.Groups, groupLink), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the invite on its way WHEN scanning another QR THEN stays on the invite`() = runTest(testDispatcher) {
        // Given
        prepareScenario(fetchGate = CompletableDeferred())

        // When
        viewModel.onEvent(JoinInviteUiEvent.OnScanAnotherClicked)

        // Then
        assertEquals(JoinInviteStatus.LOADING, viewModel.uiState.value.status)
        assertEquals(listOf(Route.Groups, groupLink), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen over an invalid invite WHEN scanning another QR THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = null)
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnScanAnotherClicked)

            // Then
            assertEquals(listOf(Route.Groups, groupLink, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a group link WHEN the screen opens THEN reads the group invite and is not personal`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isPersonal)
            assertNull(state.personalMember)
            assertEquals(
                listOf(
                    InviteTarget(
                        CODE,
                        memberCode = null,
                        inviterCode = null,
                    ),
                ),
                invites.fetchedTargets,
            )
        }

    @Test
    fun `GIVEN a personal link on its way WHEN the screen opens THEN is personal with the member link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                fetchGate = CompletableDeferred(),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(JoinInviteStatus.LOADING, state.status)
            assertTrue(state.isPersonal)
            assertEquals("blitzsplit.com/g/$CODE/$MEMBER_CODE", state.displayLink)
        }

    @Test
    fun `GIVEN a personal link shared by Ana WHEN the screen opens THEN reads the invite of that member and sharer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
            )

            // When
            val targets = invites.fetchedTargets

            // Then
            assertEquals(listOf(InviteTarget(CODE, MEMBER_CODE, INVITER_CODE)), targets)
        }

    @Test
    fun `GIVEN Carla personal link shared by Ana WHEN the screen opens THEN shows Ana invited you as Carla`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(JoinInviteStatus.READY, state.status)
            assertTrue(state.isPersonal)
            assertEquals(
                JoinMember(CARLA_ID, "Carla", "CA", AvatarColor.LILAC, hasJoined = false),
                state.personalMember,
            )
            assertEquals("Ana", state.inviterName)
            assertEquals("Viagem Floripa", state.groupName)
            assertEquals(4, state.memberCount)
        }

    @Test
    fun `GIVEN a personal link that names no sharer WHEN the screen opens THEN names nobody as the sender`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink.copy(inviterCode = null),
                invite = carlaInvite.copy(inviterName = null),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(JoinInviteStatus.READY, state.status)
            assertNull(state.inviterName)
        }

    @Test
    fun `GIVEN neither the sharer nor the creator has an account WHEN the personal link opens THEN nobody invites`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite.copy(
                    inviterName = null,
                    creatorName = null,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(JoinInviteStatus.READY, state.status)
            assertNull(state.inviterName)
        }

    @Test
    fun `GIVEN Carla renamed since the link was sent WHEN the personal link opens THEN shows her current name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite.copy(members = listOf(pierre, ana, bruno, carla.copy(name = "Carla Souza"))),
            )

            // When
            val member = viewModel.uiState.value.personalMember

            // Then
            assertEquals("Carla Souza", member?.name)
        }

    @Test
    fun `GIVEN Carla was removed from the group WHEN her personal link opens THEN shows the invite is invalid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite.copy(
                    members = listOf(pierre, ana, bruno),
                    personalMemberId = null,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(JoinInviteStatus.INVALID, state.status)
        }

    @Test
    fun `GIVEN someone already joined as Carla WHEN her personal link opens THEN shows the invite is invalid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite.copy(members = listOf(pierre, ana, bruno, carla.copy(hasJoined = true))),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(JoinInviteStatus.INVALID, state.status)
        }

    @Test
    fun `GIVEN the group link was changed WHEN a personal link opens THEN shows the invite is invalid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = null,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(JoinInviteStatus.INVALID, state.status)
        }

    @Test
    fun `GIVEN this account joined as Carla WHEN her personal link opens THEN opens the group in its place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite.copy(
                    isMember = true,
                    members = listOf(pierre, ana, bruno, carla.copy(hasJoined = true)),
                ),
                storedGroup = tripGroup,
            )

            // When
            val backStack = navigator.backStack.value

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), backStack)
        }

    @Test
    fun `GIVEN no internet WHEN a personal link opens THEN shows offline keeping the member link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                online = false,
                fetchError = IllegalStateException("offline"),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(JoinInviteStatus.OFFLINE, state.status)
            assertTrue(state.isPersonal)
            assertEquals("blitzsplit.com/g/$CODE/$MEMBER_CODE", state.displayLink)
        }

    @Test
    fun `GIVEN a personal link that could not open offline WHEN the device goes online THEN shows Carla invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
                online = false,
                fetchError = IllegalStateException("offline"),
            )
            invites.fetchError = null

            // When
            isOnline.value = true

            // Then
            assertEquals(JoinInviteStatus.READY, viewModel.uiState.value.status)
            assertEquals(
                "Carla",
                viewModel.uiState.value.personalMember
                    ?.name,
            )
            assertEquals(InviteTarget(CODE, MEMBER_CODE, INVITER_CODE), invites.fetchedTargets.last())
        }

    @Test
    fun `GIVEN a personal link that could not open WHEN trying again THEN reads the same personal invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
                online = false,
                fetchError = IllegalStateException("server down"),
            )
            invites.fetchError = null

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnRetryClicked)

            // Then
            assertEquals(JoinInviteStatus.READY, viewModel.uiState.value.status)
            assertEquals(
                listOf(
                    InviteTarget(CODE, MEMBER_CODE, INVITER_CODE),
                    InviteTarget(CODE, MEMBER_CODE, INVITER_CODE),
                ),
                invites.fetchedTargets,
            )
        }

    @Test
    fun `GIVEN Carla personal invite WHEN joining THEN joins straight in as Carla`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            route = carlaLink,
            invite = carlaInvite,
        )

        // When
        viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

        // Then
        assertEquals(1, joinCalls)
        assertEquals(listOf(CODE), invites.joins.map { it.code })
        assertEquals(listOf<JoinChoice>(JoinChoice.AsMember(CARLA_ID)), invites.joins.map { it.choice })
    }

    @Test
    fun `GIVEN Carla personal invite WHEN joining THEN opens the group over the home as Carla`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
                joinOutcome = JoinOutcome.Joined(GROUP_ID),
            )

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

            // Then
            assertEquals(listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = "Carla")), navigator.backStack.value)
            assertFalse(viewModel.uiState.value.isJoining)
        }

    @Test
    fun `GIVEN Carla link opened over the welcome WHEN joining as Carla THEN the welcome is gone`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
                under = listOf(Route.Welcome),
                joinOutcome = JoinOutcome.Joined(GROUP_ID),
            )

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

            // Then
            assertEquals(listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = "Carla")), navigator.backStack.value)
        }

    @Test
    fun `GIVEN this account joined as Bruno before WHEN joining as Carla THEN opens the group as Bruno`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
                joinOutcome = JoinOutcome.AlreadyMember(GROUP_ID, memberId = BRUNO_ID),
            )

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

            // Then
            assertEquals(listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = "Bruno")), navigator.backStack.value)
        }

    @Test
    fun `GIVEN joined as a member not in the invite WHEN joining as Carla THEN opens the group unnamed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
                joinOutcome = JoinOutcome.AlreadyMember(GROUP_ID, memberId = "member-9"),
            )

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN joined as an unknown member WHEN joining as Carla THEN opens the group unnamed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
                joinOutcome = JoinOutcome.AlreadyMember(GROUP_ID, memberId = null),
            )

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Carla taken on another device WHEN joining as Carla THEN reads the invite again as invalid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
                joinOutcome = JoinOutcome.MemberTaken,
            )
            invites.invite = carlaInvite.copy(members = listOf(pierre, ana, bruno, carla.copy(hasJoined = true)))

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

            // Then
            assertEquals(2, invites.fetchedTargets.size)
            assertEquals(JoinInviteStatus.INVALID, viewModel.uiState.value.status)
            assertFalse(viewModel.uiState.value.isJoining)
            assertEquals(listOf(Route.Groups, carlaLink), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Carla was removed meanwhile WHEN joining as Carla THEN reads the invite again as invalid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
                joinOutcome = JoinOutcome.MemberGone,
            )
            invites.invite = carlaInvite.copy(
                members = listOf(pierre, ana, bruno),
                personalMemberId = null,
            )

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

            // Then
            assertEquals(2, invites.fetchedTargets.size)
            assertEquals(JoinInviteStatus.INVALID, viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN the server finds the name taken WHEN joining as Carla THEN reads the invite again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
                joinOutcome = JoinOutcome.NameTaken,
            )
            invites.invite = carlaInvite.copy(members = listOf(pierre, ana, bruno, carla.copy(hasJoined = true)))

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

            // Then
            assertEquals(2, invites.fetchedTargets.size)
            assertEquals(JoinInviteStatus.INVALID, viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN the group link was changed meanwhile WHEN joining as Carla THEN reads the invite again as invalid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
                joinOutcome = JoinOutcome.InvalidLink,
            )
            invites.invite = null

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

            // Then
            assertEquals(2, invites.fetchedTargets.size)
            assertEquals(JoinInviteStatus.INVALID, viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN no internet WHEN joining as Carla THEN shows the join error and stays on the invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
                joinError = IllegalStateException("offline"),
            )

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

            // Then
            assertEquals(listOf<JoinInviteUiAction>(JoinInviteUiAction.ShowJoinError), actions)
            assertEquals(JoinInviteStatus.READY, viewModel.uiState.value.status)
            assertFalse(viewModel.uiState.value.isJoining)
            assertEquals(listOf(Route.Groups, carlaLink), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Carla personal invite WHEN the join is on its way THEN shows it joining`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            route = carlaLink,
            invite = carlaInvite,
            joinGate = CompletableDeferred(),
        )

        // When
        viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

        // Then
        assertTrue(viewModel.uiState.value.isJoining)
        assertEquals(listOf(Route.Groups, carlaLink), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a join as Carla on its way WHEN joining again THEN joins only once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            route = carlaLink,
            invite = carlaInvite,
            joinGate = CompletableDeferred(),
        )
        viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

        // When
        viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

        // Then
        assertEquals(1, joinCalls)
    }

    @Test
    fun `GIVEN a join as Carla on its way WHEN tapping not Carla THEN the list does not open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
                joinGate = CompletableDeferred(),
            )
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnNotMeClicked)

            // Then
            assertEquals(listOf(Route.Groups, carlaLink), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the invite was closed while joining as Carla WHEN the join goes through THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            val joinGate = CompletableDeferred<Unit>()
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
                joinGate = joinGate,
                joinOutcome = JoinOutcome.Joined(GROUP_ID),
            )
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)
            navigator.navigateBack()

            // When
            joinGate.complete(Unit)

            // Then
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Carla personal link no longer valid WHEN joining THEN nothing is sent`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            route = carlaLink,
            invite = carlaInvite.copy(members = listOf(pierre, ana, bruno, carla.copy(hasJoined = true))),
        )

        // When
        viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

        // Then
        assertEquals(0, joinCalls)
        assertEquals(listOf(Route.Groups, carlaLink), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Carla personal invite still loading WHEN joining THEN nothing is sent`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            route = carlaLink,
            invite = carlaInvite,
            fetchGate = CompletableDeferred(),
        )

        // When
        viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

        // Then
        assertEquals(0, joinCalls)
        assertEquals(listOf(Route.Groups, carlaLink), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Carla personal invite WHEN tapping not Carla THEN opens who are you in the group without joining`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
            )

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnNotMeClicked)

            // Then
            assertEquals(listOf(Route.Groups, carlaLink, Route.JoinPick(CODE)), navigator.backStack.value)
            assertEquals(0, joinCalls)
        }

    @Test
    fun `GIVEN Carla personal link no longer valid WHEN tapping not Carla THEN stays on the invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = null,
            )

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnNotMeClicked)

            // Then
            assertEquals(listOf(Route.Groups, carlaLink), navigator.backStack.value)
        }

    @Test
    fun `GIVEN another screen over Carla personal invite WHEN tapping not Carla THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
            )
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnNotMeClicked)

            // Then
            assertEquals(listOf(Route.Groups, carlaLink, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN another screen over Carla personal invite WHEN joining as Carla THEN joins nobody`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
            )
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnJoinClicked)

            // Then
            assertEquals(emptyList(), invites.joins)
            assertEquals(listOf(Route.Groups, carlaLink, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN not Carla opened the list WHEN going back to the invite THEN reads the personal invite again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = carlaInvite,
            )
            viewModel.onEvent(JoinInviteUiEvent.OnNotMeClicked)
            invites.invite = carlaInvite.copy(members = listOf(pierre, ana, bruno, carla.copy(hasJoined = true)))

            // When
            navigator.navigateBack()

            // Then
            assertEquals(InviteTarget(CODE, MEMBER_CODE, INVITER_CODE), invites.fetchedTargets.last())
            assertEquals(JoinInviteStatus.INVALID, viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN Carla personal invite on top WHEN tapping not now THEN leaves the invite`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            route = carlaLink,
            invite = carlaInvite,
        )

        // When
        viewModel.onEvent(JoinInviteUiEvent.OnNotNowClicked)

        // Then
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
        assertEquals(0, joinCalls)
    }

    @Test
    fun `GIVEN an invalid personal link over other screens WHEN going home THEN pops back to the root`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = carlaLink,
                invite = null,
                under = listOf(Route.Groups, Route.Profile),
            )

            // When
            viewModel.onEvent(JoinInviteUiEvent.OnGoHomeClicked)

            // Then
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    private val pierre = InviteMember("member-1", "Pierre", AvatarColor.BLUE, hasJoined = true)
    private val ana = InviteMember("member-2", "Ana", AvatarColor.PEACH, hasJoined = true)
    private val bruno = InviteMember("member-3", "Bruno", AvatarColor.GREEN, hasJoined = false)
    private val carla = InviteMember("member-4", "Carla", AvatarColor.LILAC, hasJoined = false)

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
        personalMemberId = null,
        inviterName = null,
    )

    private val tripGroup = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        currency = GroupCurrency.BRL,
        members = listOf(
            GroupMember(
                "member-1",
                "Pierre",
                AvatarColor.BLUE,
                isMe = true,
                hasJoined = true,
                inviteCode = null,
            ),
        ),
        isPending = false,
        inviteCode = CODE,
        isCreatedByMe = false,
    )

    // Why: the reader opened from the home, which skips no invite.
    private val reader = Route.JoinWithLink(
        skippedCode = null,
        skippedMemberCode = null,
    )

    private val groupLink = Route.JoinInvite(
        CODE,
        memberCode = null,
        inviterCode = null,
    )

    // Why: Carla's personal link, shared by Ana.
    private val carlaLink = Route.JoinInvite(
        CODE,
        memberCode = MEMBER_CODE,
        inviterCode = INVITER_CODE,
    )

    private val carlaInvite = trip.copy(
        personalMemberId = CARLA_ID,
        inviterName = "Ana",
    )

    private fun TestScope.prepareScenario(
        invite: Invite? = trip,
        fetchError: Throwable? = null,
        fetchGate: CompletableDeferred<Unit>? = null,
        online: Boolean = true,
        storedGroup: Group? = null,
        syncBrings: Group? = null,
        under: List<Route> = listOf(Route.Groups),
        route: Route.JoinInvite = groupLink,
        joinOutcome: JoinOutcome = JoinOutcome.Joined(GROUP_ID),
        joinError: Throwable? = null,
        joinGate: CompletableDeferred<Unit>? = null,
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
        groups = FakeGroupRepository(initialGroups = listOfNotNull(storedGroup))
        isOnline = MutableStateFlow(online)
        syncCount = 0
        joinCalls = 0
        actions.clear()
        navigator = BackStackNavigator(startRoute = under.first()).also { navigator ->
            under.drop(1).forEach(navigator::navigate)
            navigator.navigate(route)
        }
        viewModel = JoinInviteViewModel(
            route = route,
            navigator = navigator,
            getInvite = GetInviteUseCase(invites),
            joinGroup = JoinGroup { joined, choice ->
                joinCalls++
                invites.joinGroup(joined.code, choice, AvatarColor.default)
            },
            observeGroup = ObserveGroupUseCase(groups),
            syncNow = SyncNow {
                syncCount++
                syncBrings?.let { groups.groups.value = groups.groups.value + it }
                Result.success(Unit)
            },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val CODE = "7Hk2pQxZ"
        const val MEMBER_CODE = "c3Rt9z"
        const val INVITER_CODE = "a1B2c3"
        const val GROUP_ID = "group-1"
        const val BRUNO_ID = "member-3"
        const val CARLA_ID = "member-4"
    }
}
