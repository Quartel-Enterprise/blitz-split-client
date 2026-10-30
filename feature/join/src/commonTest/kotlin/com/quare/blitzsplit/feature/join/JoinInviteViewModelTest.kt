package com.quare.blitzsplit.feature.join

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteMember
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
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinMember
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinInviteViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JoinInviteViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var invites: FakeInviteRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: JoinInviteViewModel
    private var syncCount = 0

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
            assertEquals(listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = null)), backStack)
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
            assertEquals(listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = null)), backStack)
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
            ),
        ),
        isPending = false,
        inviteCode = CODE,
    )

    private fun prepareScenario(
        invite: Invite? = trip,
        fetchError: Throwable? = null,
        fetchGate: CompletableDeferred<Unit>? = null,
        online: Boolean = true,
        storedGroup: Group? = null,
        syncBrings: Group? = null,
        under: List<Route> = listOf(Route.Groups),
    ) {
        invites = FakeInviteRepository(invite = invite).also {
            it.fetchError = fetchError
            it.fetchGate = fetchGate
        }
        groups = FakeGroupRepository(initialGroups = listOfNotNull(storedGroup))
        isOnline = MutableStateFlow(online)
        syncCount = 0
        navigator = BackStackNavigator(startRoute = under.first()).also { navigator ->
            under.drop(1).forEach(navigator::navigate)
            navigator.navigate(
                Route.JoinInvite(
                    CODE,
                    memberCode = null,
                    inviterCode = null,
                ),
            )
        }
        viewModel = JoinInviteViewModel(
            route = Route.JoinInvite(
                CODE,
                memberCode = null,
                inviterCode = null,
            ),
            navigator = navigator,
            getInvite = GetInviteUseCase(invites),
            joinGroup = JoinGroup { joined, choice -> invites.joinGroup(joined.code, choice, AvatarColor.default) },
            observeGroup = ObserveGroupUseCase(groups),
            syncNow = SyncNow {
                syncCount++
                syncBrings?.let { groups.groups.value = groups.groups.value + it }
                Result.success(Unit)
            },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
    }

    private companion object {
        const val CODE = "7Hk2pQxZ"
        const val GROUP_ID = "group-1"
    }
}
