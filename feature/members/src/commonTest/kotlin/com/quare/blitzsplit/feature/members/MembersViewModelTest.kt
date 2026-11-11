package com.quare.blitzsplit.feature.members

import androidx.lifecycle.ViewModelStore
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.data.repository.InMemoryAddPersonRequestRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.AddMemberUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CheckMemberNameUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeAddPersonRequestUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveAddPersonRequestUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ReleaseMemberUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.RemoveMemberUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.RenameMemberUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.members.presentation.model.MemberNameEditor
import com.quare.blitzsplit.feature.members.presentation.model.MembersMember
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiAction
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent
import com.quare.blitzsplit.feature.members.presentation.model.PersonalInvite
import com.quare.blitzsplit.feature.members.presentation.viewmodel.MembersViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class MembersViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var addPersonRequests: InMemoryAddPersonRequestRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private val isRoomRead = MutableStateFlow(true)
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: MembersViewModel
    private val actions = mutableListOf<MembersUiAction>()

    // Why: the server's answer to a release, which the cases that wait for it complete.
    private val serverAnswer = CompletableDeferred<Unit>()

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
        prepareScenario(isRoomReadAtStart = false)

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
                        photoUrl = null,
                        isMe = true,
                        hasJoined = true,
                        personalLink = null,
                        balance = null,
                        hasOpenBalance = false,
                        canBeReleased = false,
                    ),
                    MembersMember(
                        "member-2",
                        "Ana",
                        "A",
                        AvatarColor.PEACH,
                        photoUrl = null,
                        isMe = false,
                        hasJoined = false,
                        personalLink = null,
                        balance = null,
                        hasOpenBalance = false,
                        canBeReleased = false,
                    ),
                    MembersMember(
                        "member-3",
                        "Bruno",
                        "B",
                        AvatarColor.GREEN,
                        photoUrl = null,
                        isMe = false,
                        hasJoined = true,
                        personalLink = null,
                        balance = null,
                        hasOpenBalance = false,
                        canBeReleased = true,
                    ),
                    MembersMember(
                        "member-4",
                        "Carla",
                        "C",
                        AvatarColor.LILAC,
                        photoUrl = null,
                        isMe = false,
                        hasJoined = false,
                        personalLink = null,
                        balance = null,
                        hasOpenBalance = false,
                        canBeReleased = false,
                    ),
                ),
                state.members,
            )
        }

    @Test
    fun `GIVEN a group with an invite code WHEN the screen opens THEN shows the link without the scheme`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(inviteCode = "7Hk2pQ"))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("blitzsplit.com/g/7Hk2pQ", state.displayLink)
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
            assertEquals("blitzsplit.com/g/7Hk2pQ", state.displayLink)
            assertEquals(listOf("Pierre", "Ana", "Bruno", "Carla", "Diego"), state.members.map { it.name })
            assertEquals(5, state.members.size)
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
                listOf(
                    Route.Groups,
                    Route.GroupDetail("group-1", joinedAs = null, GroupTab.MEMBERS),
                    Route.Invite("group-1", isNewGroup = false),
                ),
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
        assertEquals(
            listOf(Route.Groups, Route.GroupDetail("group-1", joinedAs = null, GroupTab.MEMBERS), Route.Profile),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a bill of the group beside it WHEN tapping share THEN Convidar takes the place of the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.BillDetail("group-1", "dinner", focusCommentId = null))

            // When
            viewModel.onEvent(MembersUiEvent.OnShareClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail("group-1", joinedAs = null, GroupTab.MEMBERS),
                    Route.Invite("group-1", isNewGroup = false),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a guest with spaces and capitals in the name WHEN tapping their row THEN opens their contact by name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(members = listOf(pierre, ana, bruno, carla.copy(name = "Carla  Dias"))),
            )
            val event = MembersUiEvent.OnMemberClicked("member-4")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail("group-1", joinedAs = null, GroupTab.MEMBERS),
                    Route.ContactProfile("name:carla dias"),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN someone who joined with an account WHEN tapping their row THEN opens their contact by account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(members = listOf(pierre, ana, bruno.copy(userId = "user-bruno"), carla)),
            )
            val event = MembersUiEvent.OnMemberClicked("member-3")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail("group-1", joinedAs = null, GroupTab.MEMBERS),
                    Route.ContactProfile("user:user-bruno"),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN this device user with an account WHEN tapping their own row THEN opens Perfil`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(members = listOf(pierre.copy(userId = "user-pierre"), ana, bruno, carla)),
            )
            val event = MembersUiEvent.OnMemberClicked("member-1")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.GroupDetail("group-1", joinedAs = null, GroupTab.MEMBERS), Route.Profile),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the screen open WHEN tapping the row of an unknown member THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val event = MembersUiEvent.OnMemberClicked("member-99")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.GroupDetail("group-1", joinedAs = null, GroupTab.MEMBERS)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Room not read yet WHEN tapping the row of a member THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomReadAtStart = false)
        val event = MembersUiEvent.OnMemberClicked("member-4")

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            listOf(Route.Groups, Route.GroupDetail("group-1", joinedAs = null, GroupTab.MEMBERS)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a bill of the group beside it WHEN tapping a guest THEN their contact takes the place of the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.BillDetail("group-1", "dinner", focusCommentId = null))
            val event = MembersUiEvent.OnMemberClicked("member-4")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail("group-1", joinedAs = null, GroupTab.MEMBERS),
                    Route.ContactProfile("name:carla"),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a bill of the group beside it WHEN tapping their own row THEN Perfil opens over the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.BillDetail("group-1", "dinner", focusCommentId = null))
            val event = MembersUiEvent.OnMemberClicked("member-1")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail("group-1", joinedAs = null, GroupTab.MEMBERS),
                    Route.BillDetail("group-1", "dinner", focusCommentId = null),
                    Route.Profile,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN another screen on top WHEN tapping the row of a guest THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Invite("group-1", isNewGroup = false))
            val event = MembersUiEvent.OnMemberClicked("member-4")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail("group-1", joinedAs = null, GroupTab.MEMBERS),
                    Route.Invite("group-1", isNewGroup = false),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN another screen on top WHEN tapping their own row THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Invite("group-1", isNewGroup = false))
        val event = MembersUiEvent.OnMemberClicked("member-1")

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail("group-1", joinedAs = null, GroupTab.MEMBERS),
                Route.Invite("group-1", isNewGroup = false),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a native back closed the group WHEN tapping the row of a guest THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()
            val event = MembersUiEvent.OnMemberClicked("member-4")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN someone whose link has not come yet WHEN tapping their personal invite THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val event = MembersUiEvent.OnPersonalInviteClicked("member-2")

            // When
            viewModel.onEvent(event)

            // Then
            assertNull(viewModel.uiState.value.personalInvite)
            assertEquals(emptyList(), actions)
            assertEquals(
                listOf(Route.Groups, Route.GroupDetail("group-1", joinedAs = null, GroupTab.MEMBERS)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the codes synced WHEN the screen opens THEN Carla has her link from this device user`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(carlaLink, state.members.first { it.id == "member-4" }.personalLink)
            assertTrue(state.members.first { it.id == "member-4" }.isPersonalInviteReady)
        }

    @Test
    fun `GIVEN this device user without a code yet WHEN the screen opens THEN the personal link has no sender`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = tripWithCodes.copy(members = listOf(pierre, ana, brunoWithCode, carlaWithCode)),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                "https://blitzsplit.com/g/7Hk2pQ8x/c3Rt9z",
                state.members.first { it.id == "member-4" }.personalLink,
            )
        }

    @Test
    fun `GIVEN a member added on this device WHEN the screen opens THEN that member has no personal link yet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.members.first { it.id == "member-2" }.personalLink)
            assertFalse(state.members.first { it.id == "member-2" }.isPersonalInviteReady)
        }

    @Test
    fun `GIVEN the member code synced but no group code WHEN the screen opens THEN Carla has no personal link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes.copy(inviteCode = null))

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.members.first { it.id == "member-4" }.personalLink)
            assertFalse(state.members.first { it.id == "member-4" }.isPersonalInviteReady)
        }

    @Test
    fun `GIVEN Carla with her personal link WHEN tapping her personal invite THEN opens it with her link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            val event = MembersUiEvent.OnPersonalInviteClicked("member-4")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                PersonalInvite(
                    member = carlaWithLink,
                    link = carlaLink,
                    displayLink = "blitzsplit.com/g/7Hk2pQ8x/c3Rt9z",
                    isLinkCopied = false,
                    billCount = 0,
                    billShareMinor = 0,
                ),
                viewModel.uiState.value.personalInvite,
            )
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN bills with a part for Carla WHEN tapping her personal invite THEN counts those bills and her parts`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = tripWithCodes,
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ),
                    expense(
                        "bill-2",
                        paidBy = "member-4",
                        splitWith = listOf("member-2", "member-4"),
                        amountMinor = 3_000,
                    ),
                    expense(
                        "bill-3",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-2"),
                    ),
                    expense(
                        "bill-4",
                        paidBy = "member-1",
                        splitWith = listOf("member-1"),
                    ).let { it.copy(shares = it.shares + ExpenseShare("member-4", 0, weight = 0)) },
                ),
            )

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // Then
            val invite = viewModel.uiState.value.personalInvite
            assertEquals(2, invite?.billCount)
            assertEquals(6_500L, invite?.billShareMinor)
        }

    @Test
    fun `GIVEN the personal invite of Carla open WHEN a sync brings a bill with her part THEN the invite counts it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // When
            expenses.expenses.value =
                listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ),
                )

            // Then
            val invite = viewModel.uiState.value.personalInvite
            assertEquals(1, invite?.billCount)
            assertEquals(5_000L, invite?.billShareMinor)
        }

    @Test
    fun `GIVEN this device user without a code yet WHEN tapping a personal invite THEN the link has no sender`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = tripWithCodes.copy(members = listOf(pierre, ana, brunoWithCode, carlaWithCode)),
            )
            val event = MembersUiEvent.OnPersonalInviteClicked("member-4")

            // When
            viewModel.onEvent(event)

            // Then
            val invite = viewModel.uiState.value.personalInvite
            assertEquals("https://blitzsplit.com/g/7Hk2pQ8x/c3Rt9z", invite?.link)
            assertEquals("blitzsplit.com/g/7Hk2pQ8x/c3Rt9z", invite?.displayLink)
        }

    @Test
    fun `GIVEN the codes synced WHEN tapping the personal invite of an unknown member THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            val event = MembersUiEvent.OnPersonalInviteClicked("member-99")

            // When
            viewModel.onEvent(event)

            // Then
            assertNull(viewModel.uiState.value.personalInvite)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the codes synced WHEN tapping the personal invite of this device user THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            val event = MembersUiEvent.OnPersonalInviteClicked("member-1")

            // When
            viewModel.onEvent(event)

            // Then
            assertNull(viewModel.uiState.value.personalInvite)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the codes synced WHEN tapping the personal invite of someone who joined THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            val event = MembersUiEvent.OnPersonalInviteClicked("member-3")

            // When
            viewModel.onEvent(event)

            // Then
            assertNull(viewModel.uiState.value.personalInvite)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN a member added on this device WHEN tapping their personal invite THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            val event = MembersUiEvent.OnPersonalInviteClicked("member-2")

            // When
            viewModel.onEvent(event)

            // Then
            assertNull(viewModel.uiState.value.personalInvite)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN Carla has a code but the group has none yet WHEN tapping her personal invite THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes.copy(inviteCode = null))
            val event = MembersUiEvent.OnPersonalInviteClicked("member-4")

            // When
            viewModel.onEvent(event)

            // Then
            assertNull(viewModel.uiState.value.personalInvite)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the personal invite of Carla open WHEN dismissing it THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = tripWithCodes)
        viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

        // When
        viewModel.onEvent(MembersUiEvent.OnPersonalInviteDismissed)

        // Then
        assertNull(viewModel.uiState.value.personalInvite)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN the group code WHEN copying the link of the link card THEN copies the whole group link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)

            // When
            viewModel.onEvent(MembersUiEvent.OnLinkCopyClicked)

            // Then
            assertEquals(
                listOf<MembersUiAction>(MembersUiAction.CopyGroupLink("https://blitzsplit.com/g/7Hk2pQ8x")),
                actions,
            )
        }

    @Test
    fun `GIVEN the group code WHEN reading the state THEN the link can be copied`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = tripWithCodes)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.canCopyLink)
    }

    @Test
    fun `GIVEN a group without its code yet WHEN reading the state THEN the link cannot be copied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes.copy(inviteCode = null))

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.canCopyLink)
        }

    @Test
    fun `GIVEN a group without its code yet WHEN copying the link of the link card THEN copies nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes.copy(inviteCode = null))

            // When
            viewModel.onEvent(MembersUiEvent.OnLinkCopyClicked)

            // Then
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the personal invite of Carla open WHEN copying THEN copies her whole link and shows it copied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalLinkCopyClicked)

            // Then
            assertEquals(listOf<MembersUiAction>(MembersUiAction.CopyLink(carlaLink)), actions)
            assertEquals(
                true,
                viewModel.uiState.value.personalInvite
                    ?.isLinkCopied,
            )
        }

    @Test
    fun `GIVEN the personal link just copied WHEN two seconds pass THEN it no longer shows copied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))
            viewModel.onEvent(MembersUiEvent.OnPersonalLinkCopyClicked)
            advanceTimeBy(1_999.milliseconds)
            runCurrent()
            val copiedJustBefore = viewModel.uiState.value.personalInvite
                ?.isLinkCopied

            // When
            advanceTimeBy(1.milliseconds)
            runCurrent()

            // Then
            assertEquals(true, copiedJustBefore)
            assertEquals(
                false,
                viewModel.uiState.value.personalInvite
                    ?.isLinkCopied,
            )
        }

    @Test
    fun `GIVEN the personal link copied 1500 ms ago WHEN copying again THEN shows copied 2 s from the second tap`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))
            viewModel.onEvent(MembersUiEvent.OnPersonalLinkCopyClicked)
            advanceTimeBy(1_500.milliseconds)
            runCurrent()

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalLinkCopyClicked)

            // Then
            advanceTimeBy(1_000.milliseconds)
            runCurrent()
            val copiedAfterTheFirstTimer = viewModel.uiState.value.personalInvite
                ?.isLinkCopied
            advanceTimeBy(1_000.milliseconds)
            runCurrent()
            assertEquals(true, copiedAfterTheFirstTimer)
            assertEquals(
                false,
                viewModel.uiState.value.personalInvite
                    ?.isLinkCopied,
            )
            assertEquals(
                listOf<MembersUiAction>(MembersUiAction.CopyLink(carlaLink), MembersUiAction.CopyLink(carlaLink)),
                actions,
            )
        }

    @Test
    fun `GIVEN the personal link just copied WHEN closing the invite and two seconds pass THEN it stays closed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))
            viewModel.onEvent(MembersUiEvent.OnPersonalLinkCopyClicked)

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteDismissed)

            // Then
            advanceTimeBy(2_000.milliseconds)
            runCurrent()
            assertNull(viewModel.uiState.value.personalInvite)
        }

    @Test
    fun `GIVEN the personal link copied and the invite closed WHEN opening it again THEN it does not show copied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))
            viewModel.onEvent(MembersUiEvent.OnPersonalLinkCopyClicked)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteDismissed)

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // Then
            assertEquals(
                false,
                viewModel.uiState.value.personalInvite
                    ?.isLinkCopied,
            )
        }

    @Test
    fun `GIVEN no personal invite open WHEN copying the personal link THEN copies nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = tripWithCodes)
        val event = MembersUiEvent.OnPersonalLinkCopyClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(actions.isEmpty())
        assertNull(viewModel.uiState.value.personalInvite)
    }

    @Test
    fun `GIVEN the personal invite of Carla open WHEN sharing THEN shares her link with her name and the group name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalLinkShareClicked)

            // Then
            assertEquals(
                listOf<MembersUiAction>(MembersUiAction.SharePersonalLink(carlaLink, "Carla", "Viagem Floripa")),
                actions,
            )
        }

    @Test
    fun `GIVEN no personal invite open WHEN sharing the personal link THEN shares nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = tripWithCodes)
        val event = MembersUiEvent.OnPersonalLinkShareClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN the personal invite of Carla open WHEN a sync renames her THEN shows her new name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))
            val carlaRenamed = tripWithCodes.copy(
                members = listOf(pierreWithCode, ana, brunoWithCode, carlaWithCode.copy(name = "Carla M.")),
            )

            // When
            groups.groups.value = listOf(carlaRenamed)

            // Then
            assertEquals(
                PersonalInvite(
                    member = carlaWithLink.copy(
                        name = "Carla M.",
                        initials = "CM",
                    ),
                    link = carlaLink,
                    displayLink = "blitzsplit.com/g/7Hk2pQ8x/c3Rt9z",
                    isLinkCopied = false,
                    billCount = 0,
                    billShareMinor = 0,
                ),
                viewModel.uiState.value.personalInvite,
            )
        }

    @Test
    fun `GIVEN the personal invite of Carla open and a sync renamed her WHEN sharing THEN shares with her new name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))
            groups.groups.value = listOf(
                tripWithCodes.copy(
                    members = listOf(pierreWithCode, ana, brunoWithCode, carlaWithCode.copy(name = "Carla M.")),
                ),
            )

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalLinkShareClicked)

            // Then
            assertEquals(
                listOf<MembersUiAction>(MembersUiAction.SharePersonalLink(carlaLink, "Carla M.", "Viagem Floripa")),
                actions,
            )
        }

    @Test
    fun `GIVEN the personal link of Carla copied WHEN a sync renames her THEN still shows it copied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))
            viewModel.onEvent(MembersUiEvent.OnPersonalLinkCopyClicked)
            val carlaRenamed = tripWithCodes.copy(
                members = listOf(pierreWithCode, ana, brunoWithCode, carlaWithCode.copy(name = "Carla M.")),
            )

            // When
            groups.groups.value = listOf(carlaRenamed)

            // Then
            assertEquals(
                true,
                viewModel.uiState.value.personalInvite
                    ?.isLinkCopied,
            )
            assertEquals(
                "Carla M.",
                viewModel.uiState.value.personalInvite
                    ?.member
                    ?.name,
            )
        }

    @Test
    fun `GIVEN copied and a sync renamed Carla WHEN two seconds pass THEN it no longer shows copied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))
            viewModel.onEvent(MembersUiEvent.OnPersonalLinkCopyClicked)
            groups.groups.value = listOf(
                tripWithCodes.copy(
                    members = listOf(pierreWithCode, ana, brunoWithCode, carlaWithCode.copy(name = "Carla M.")),
                ),
            )

            // When
            advanceTimeBy(2_000.milliseconds)
            runCurrent()

            // Then
            assertEquals(
                false,
                viewModel.uiState.value.personalInvite
                    ?.isLinkCopied,
            )
            assertEquals(
                "Carla M.",
                viewModel.uiState.value.personalInvite
                    ?.member
                    ?.name,
            )
        }

    @Test
    fun `GIVEN the personal invite of Carla open WHEN a sync says someone joined as her THEN closes it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))
            val carlaJoined = tripWithCodes.copy(
                members = listOf(pierreWithCode, ana, brunoWithCode, carlaWithCode.copy(hasJoined = true)),
            )

            // When
            groups.groups.value = listOf(carlaJoined)

            // Then
            assertNull(viewModel.uiState.value.personalInvite)
        }

    @Test
    fun `GIVEN the personal invite of Carla open WHEN a sync removes her THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = tripWithCodes)
        viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))
        val withoutCarla = tripWithCodes.copy(members = listOf(pierreWithCode, ana, brunoWithCode))

        // When
        groups.groups.value = listOf(withoutCarla)

        // Then
        assertNull(viewModel.uiState.value.personalInvite)
    }

    @Test
    fun `GIVEN the personal invite of Carla open WHEN a sync adds someone else THEN keeps it as it was`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))
            val before = viewModel.uiState.value.personalInvite
            val withDiego = tripWithCodes.copy(members = tripWithCodes.members + diego)

            // When
            groups.groups.value = listOf(withDiego)

            // Then
            assertEquals(before, viewModel.uiState.value.personalInvite)
        }

    @Test
    fun `GIVEN the personal invite open with no sender WHEN a sync brings the code of this device user THEN adds it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = tripWithCodes.copy(members = listOf(pierre, ana, brunoWithCode, carlaWithCode)),
            )
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // When
            groups.groups.value = listOf(tripWithCodes)

            // Then
            assertEquals(
                carlaLink,
                viewModel.uiState.value.personalInvite
                    ?.link,
            )
            assertEquals(
                "blitzsplit.com/g/7Hk2pQ8x/c3Rt9z",
                viewModel.uiState.value.personalInvite
                    ?.displayLink,
            )
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
    fun `GIVEN renaming a member WHEN typing the name of another member THEN can save it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-2"))

        // When
        viewModel.onEvent(MembersUiEvent.OnDraftChanged("bruno"))

        // Then
        val editor = viewModel.uiState.value.editor!!
        assertTrue(editor.canSave)
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
    fun `GIVEN the name of another member typed WHEN saving THEN closes the editor and adds them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Carla"))

            // When
            viewModel.onEvent(MembersUiEvent.OnSaveClicked)

            // Then
            assertNull(viewModel.uiState.value.editor)
            assertEquals(listOf("Carla"), groups.addedMembers.map { it.second.name })
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
        assertEquals(4, viewModel.uiState.value.members.size)
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
        assertEquals(4, viewModel.uiState.value.members.size)
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
            assertEquals(4, viewModel.uiState.value.members.size)
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
            assertEquals(4, viewModel.uiState.value.members.size)
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
        assertEquals(4, viewModel.uiState.value.members.size)
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
            assertEquals(4, viewModel.uiState.value.members.size)
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
            assertEquals(4, viewModel.uiState.value.members.size)
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
        assertEquals(4, viewModel.uiState.value.members.size)
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
        assertEquals(4, viewModel.uiState.value.members.size)
    }

    @Test
    fun `GIVEN a group without bills WHEN the screen opens THEN nobody shows a balance`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(listOf<MemberBalance?>(null, null, null, null), state.members.map { it.balance })
    }

    @Test
    fun `GIVEN a bill this device user paid with Carla WHEN the screen opens THEN each member shows their balance`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                listOf(
                    MemberBalance(
                        receivesMinor = 5_000,
                        owesMinor = 0,
                    ),
                    MemberBalance(
                        receivesMinor = 0,
                        owesMinor = 0,
                    ),
                    MemberBalance(
                        receivesMinor = 0,
                        owesMinor = 0,
                    ),
                    MemberBalance(
                        receivesMinor = 0,
                        owesMinor = 5_000,
                    ),
                ),
                state.members.map { it.balance },
            )
        }

    @Test
    fun `GIVEN this device user owes Ana and is owed by Carla WHEN the screen opens THEN their row holds both`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-2",
                        splitWith = listOf("member-2", "member-1"),
                    ),
                    expense(
                        "bill-2",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                        amountMinor = 6_000,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                MemberBalance(
                    receivesMinor = 3_000,
                    owesMinor = 5_000,
                ),
                state.members[0].balance,
            )
        }

    @Test
    fun `GIVEN a bill and a payment that settles it WHEN the screen opens THEN nobody has an open balance`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ),
                ),
                storedSettlements = listOf(
                    settlement(
                        "pay-1",
                        from = "member-4",
                        to = "member-1",
                        amountMinor = 5_000,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.members.none { it.hasOpenBalance })
        }

    @Test
    fun `GIVEN a group in dollars WHEN the screen opens THEN uses the currency of the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(currency = GroupCurrency.USD))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupCurrency.USD, state.currency)
        }

    @Test
    fun `GIVEN a group without this device user WHEN a bill is in it THEN the others show their balances`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(members = listOf(pierre.copy(isMe = false), ana, bruno, carla)),
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 5_000,
                ),
                state.members[3].balance,
            )
        }

    @Test
    fun `GIVEN the screen open without bills WHEN a sync brings a bill THEN the balances follow`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val bill = expense(
                "bill-1",
                paidBy = "member-1",
                splitWith = listOf("member-1", "member-4"),
            )

            // When
            expenses.expenses.value = listOf(bill)

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 5_000,
                ),
                state.members[3].balance,
            )
            assertTrue(state.members[3].hasOpenBalance)
        }

    @Test
    fun `GIVEN Carla owes this device user WHEN a sync brings her payment THEN she is settled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ),
                ),
            )
            val payment = settlement(
                "pay-1",
                from = "member-4",
                to = "member-1",
                amountMinor = 5_000,
            )

            // When
            settlements.settlements.value = listOf(payment)

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 0,
                ),
                state.members[3].balance,
            )
        }

    @Test
    fun `GIVEN Carla owes this device user WHEN tapping remove on her THEN asks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedExpenses = listOf(
                expense(
                    "bill-1",
                    paidBy = "member-1",
                    splitWith = listOf("member-1", "member-4"),
                ),
            ),
        )
        val event = MembersUiEvent.OnRemoveClicked("member-4")

        // When
        viewModel.onEvent(event)

        // Then
        assertNull(viewModel.uiState.value.memberToRemove)
        assertTrue(groups.removedMemberIds.isEmpty())
        assertTrue(
            viewModel.uiState.value.members[3]
                .hasOpenBalance,
        )
        assertFalse(
            viewModel.uiState.value.members[3]
                .canBeRemoved,
        )
    }

    @Test
    fun `GIVEN this device user owes Carla WHEN tapping remove on her THEN asks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedExpenses = listOf(
                expense(
                    "bill-1",
                    paidBy = "member-4",
                    splitWith = listOf("member-4", "member-1"),
                ),
            ),
        )
        val event = MembersUiEvent.OnRemoveClicked("member-4")

        // When
        viewModel.onEvent(event)

        // Then
        assertNull(viewModel.uiState.value.memberToRemove)
        assertTrue(groups.removedMemberIds.isEmpty())
    }

    @Test
    fun `GIVEN Carla owes Ana as much as this device user owes her WHEN tapping remove on her THEN asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-4",
                        splitWith = listOf("member-4", "member-1"),
                    ),
                    expense(
                        "bill-2",
                        paidBy = "member-2",
                        splitWith = listOf("member-2", "member-4"),
                    ),
                ),
            )
            val event = MembersUiEvent.OnRemoveClicked("member-4")

            // When
            viewModel.onEvent(event)

            // Then
            assertNull(viewModel.uiState.value.memberToRemove)
            assertTrue(groups.removedMemberIds.isEmpty())
            assertEquals(
                MemberBalance(
                    receivesMinor = 5_000,
                    owesMinor = 5_000,
                ),
                viewModel.uiState.value.members[3]
                    .balance,
            )
        }

    @Test
    fun `GIVEN Carla out of every bill WHEN tapping remove on her THEN asks first`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedExpenses = listOf(
                expense(
                    "bill-1",
                    paidBy = "member-1",
                    splitWith = listOf("member-1", "member-2"),
                ),
            ),
        )
        val event = MembersUiEvent.OnRemoveClicked("member-4")

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            carlaOnScreen.copy(
                balance = MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 0,
                ),
            ),
            viewModel.uiState.value.memberToRemove,
        )
    }

    @Test
    fun `GIVEN a payment settled the debt of Carla WHEN removing her THEN she leaves the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ),
                ),
                storedSettlements = listOf(
                    settlement(
                        "pay-1",
                        from = "member-4",
                        to = "member-1",
                        amountMinor = 5_000,
                    ),
                ),
            )
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))

            // When
            viewModel.onEvent(MembersUiEvent.OnRemoveConfirmed)

            // Then
            assertEquals(listOf("member-4"), groups.removedMemberIds)
        }

    @Test
    fun `GIVEN the removal of Carla asked WHEN a sync brings a bill with her THEN closes the question`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))
            val bill = expense(
                "bill-1",
                paidBy = "member-1",
                splitWith = listOf("member-1", "member-4"),
            )

            // When
            expenses.expenses.value = listOf(bill)

            // Then
            assertNull(viewModel.uiState.value.memberToRemove)
        }

    @Test
    fun `GIVEN the removal of Carla asked and a bill with her synced WHEN confirming THEN nothing is removed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))
            expenses.expenses.value =
                listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ),
                )

            // When
            viewModel.onEvent(MembersUiEvent.OnRemoveConfirmed)

            // Then
            assertTrue(groups.removedMemberIds.isEmpty())
            assertEquals(4, viewModel.uiState.value.members.size)
        }

    @Test
    fun `GIVEN the removal of Carla asked WHEN a sync brings a bill without her THEN still asks about Carla`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))
            val bill = expense(
                "bill-1",
                paidBy = "member-1",
                splitWith = listOf("member-1", "member-2"),
            )

            // When
            expenses.expenses.value = listOf(bill)

            // Then
            assertEquals(
                carlaOnScreen.copy(
                    balance = MemberBalance(
                        receivesMinor = 0,
                        owesMinor = 0,
                    ),
                ),
                viewModel.uiState.value.memberToRemove,
            )
        }

    @Test
    fun `GIVEN this device user did not create the group WHEN the screen opens THEN nobody can be released`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(isCreatedByMe = false))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(false, false, false, false), state.members.map { it.canBeReleased })
        }

    @Test
    fun `GIVEN Bruno joined and owes this device user WHEN the screen opens THEN he can still be released`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-3"),
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            val bruno = state.members.first { it.id == "member-3" }
            assertTrue(bruno.hasOpenBalance)
            assertTrue(bruno.canBeReleased)
            assertEquals(listOf(false, false, true, false), state.members.map { it.canBeReleased })
        }

    @Test
    fun `GIVEN Bruno joined WHEN the creator taps Liberar nome on him THEN asks first without releasing him`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val event = MembersUiEvent.OnReleaseClicked("member-3")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(brunoOnScreen, viewModel.uiState.value.memberToRelease)
            assertFalse(viewModel.uiState.value.isReleasing)
            assertTrue(groups.releasedMemberIds.isEmpty())
        }

    @Test
    fun `GIVEN this device user WHEN tapping Liberar nome on them THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = MembersUiEvent.OnReleaseClicked("member-1")

        // When
        viewModel.onEvent(event)

        // Then
        assertNull(viewModel.uiState.value.memberToRelease)
        assertTrue(groups.releasedMemberIds.isEmpty())
    }

    @Test
    fun `GIVEN Carla nobody joined as WHEN tapping Liberar nome on her THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = MembersUiEvent.OnReleaseClicked("member-4")

        // When
        viewModel.onEvent(event)

        // Then
        assertNull(viewModel.uiState.value.memberToRelease)
        assertTrue(groups.releasedMemberIds.isEmpty())
    }

    @Test
    fun `GIVEN an unknown member WHEN tapping Liberar nome on them THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = MembersUiEvent.OnReleaseClicked("member-unknown")

        // When
        viewModel.onEvent(event)

        // Then
        assertNull(viewModel.uiState.value.memberToRelease)
        assertTrue(groups.releasedMemberIds.isEmpty())
    }

    @Test
    fun `GIVEN this device user did not create the group WHEN tapping Liberar nome on Bruno THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(isCreatedByMe = false))
            val event = MembersUiEvent.OnReleaseClicked("member-3")

            // When
            viewModel.onEvent(event)

            // Then
            assertNull(viewModel.uiState.value.memberToRelease)
            assertTrue(groups.releasedMemberIds.isEmpty())
        }

    @Test
    fun `GIVEN the release of Bruno asked WHEN dismissing it THEN closes it without releasing him`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))

            // When
            viewModel.onEvent(MembersUiEvent.OnReleaseDismissed)

            // Then
            assertNull(viewModel.uiState.value.memberToRelease)
            assertFalse(viewModel.uiState.value.isReleasing)
            assertTrue(groups.releasedMemberIds.isEmpty())
        }

    @Test
    fun `GIVEN no release asked WHEN confirming a release THEN nothing is released`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = MembersUiEvent.OnReleaseConfirmed

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(groups.releasedMemberIds.isEmpty())
        assertFalse(viewModel.uiState.value.isReleasing)
        assertNull(viewModel.uiState.value.memberToRelease)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN the release of Bruno asked WHEN confirming THEN Liberar waits for the server with the question open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(waitsForServer = true)
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))

            // When
            viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)

            // Then
            assertTrue(viewModel.uiState.value.isReleasing)
            assertEquals(brunoOnScreen, viewModel.uiState.value.memberToRelease)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the server answering the release of Bruno WHEN dismissing it THEN the question stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(waitsForServer = true)
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))
            viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)

            // When
            viewModel.onEvent(MembersUiEvent.OnReleaseDismissed)

            // Then
            assertEquals(brunoOnScreen, viewModel.uiState.value.memberToRelease)
            assertTrue(viewModel.uiState.value.isReleasing)
        }

    @Test
    fun `GIVEN Liberar tapped twice while the server answers WHEN it answers THEN releases Bruno once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(waitsForServer = true)
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))
            viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)
            viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)

            // When
            serverAnswer.complete(Unit)
            runCurrent()

            // Then
            assertEquals(listOf("member-3"), groups.releasedMemberIds)
            assertNull(viewModel.uiState.value.memberToRelease)
            assertFalse(viewModel.uiState.value.isReleasing)
        }

    @Test
    fun `GIVEN the server answering the release of Bruno WHEN it succeeds THEN closes and he has not joined`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(waitsForServer = true)
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))
            viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)

            // When
            serverAnswer.complete(Unit)
            runCurrent()

            // Then
            val state = viewModel.uiState.value
            assertNull(state.memberToRelease)
            assertFalse(state.isReleasing)
            val bruno = state.members.first { it.id == "member-3" }
            assertEquals("Bruno", bruno.name)
            assertEquals(AvatarColor.GREEN, bruno.avatarColor)
            assertFalse(bruno.hasJoined)
            assertFalse(bruno.canBeReleased)
            assertTrue(bruno.canBeInvited)
            assertEquals(4, state.members.size)
            assertEquals(1, state.members.count { it.hasJoined })
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the release of Bruno asked WHEN confirming THEN asks the server to release Bruno`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))

            // When
            viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)

            // Then
            assertEquals(listOf("member-3"), groups.releasedMemberIds)
            assertNull(viewModel.uiState.value.memberToRelease)
            assertFalse(viewModel.uiState.value.isReleasing)
        }

    @Test
    fun `GIVEN no internet or a server error WHEN confirming the release of Bruno THEN says it could not release`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(releaseError = IllegalStateException("offline"))
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))

            // When
            viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)

            // Then
            assertEquals(listOf<MembersUiAction>(MembersUiAction.ShowReleaseError), actions)
            assertNull(viewModel.uiState.value.memberToRelease)
            assertFalse(viewModel.uiState.value.isReleasing)
            assertTrue(
                viewModel.uiState.value.members
                    .first { it.id == "member-3" }
                    .hasJoined,
            )
        }

    @Test
    fun `GIVEN the release of Bruno asked WHEN a sync says nobody joined as him THEN closes the question`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))
            val brunoNotJoined = trip.copy(members = listOf(pierre, ana, bruno.copy(hasJoined = false), carla))

            // When
            groups.groups.value = listOf(brunoNotJoined)

            // Then
            assertNull(viewModel.uiState.value.memberToRelease)
            assertFalse(viewModel.uiState.value.isReleasing)
        }

    @Test
    fun `GIVEN the release of Bruno asked WHEN a sync removes him THEN closes the question`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))
            val withoutBruno = trip.copy(members = listOf(pierre, ana, carla))

            // When
            groups.groups.value = listOf(withoutBruno)

            // Then
            assertNull(viewModel.uiState.value.memberToRelease)
        }

    @Test
    fun `GIVEN the release of Bruno asked and a sync removed him WHEN confirming THEN nothing is released`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))
            groups.groups.value = listOf(trip.copy(members = listOf(pierre, ana, carla)))

            // When
            viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)

            // Then
            assertTrue(groups.releasedMemberIds.isEmpty())
            assertFalse(viewModel.uiState.value.isReleasing)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the release of Bruno asked WHEN a sync renames him THEN still asks with his new name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))
            val brunoRenamed = trip.copy(members = listOf(pierre, ana, bruno.copy(name = "Bruno M."), carla))

            // When
            groups.groups.value = listOf(brunoRenamed)

            // Then
            assertEquals(
                brunoOnScreen.copy(
                    name = "Bruno M.",
                    initials = "BM",
                ),
                viewModel.uiState.value.memberToRelease,
            )
        }

    @Test
    fun `GIVEN the release of Bruno on its way WHEN a sync says nobody joined as him THEN the question stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(waitsForServer = true)
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))
            viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)
            val brunoNotJoined = trip.copy(members = listOf(pierre, ana, bruno.copy(hasJoined = false), carla))

            // When
            groups.groups.value = listOf(brunoNotJoined)

            // Then
            assertEquals(brunoOnScreen, viewModel.uiState.value.memberToRelease)
            assertTrue(viewModel.uiState.value.isReleasing)
        }

    @Test
    fun `GIVEN the server answering the release of Bruno WHEN a sync removes him THEN the question stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(waitsForServer = true)
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))
            viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)
            val withoutBruno = trip.copy(members = listOf(pierre, ana, carla))

            // When
            groups.groups.value = listOf(withoutBruno)

            // Then
            assertEquals(brunoOnScreen, viewModel.uiState.value.memberToRelease)
            assertTrue(viewModel.uiState.value.isReleasing)
        }

    @Test
    fun `GIVEN a sync came while the server answered WHEN the server answers THEN closes the question`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(waitsForServer = true)
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))
            viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)
            groups.groups.value = listOf(trip.copy(members = listOf(pierre, ana, bruno.copy(hasJoined = false), carla)))

            // When
            serverAnswer.complete(Unit)
            runCurrent()

            // Then
            assertNull(viewModel.uiState.value.memberToRelease)
            assertFalse(viewModel.uiState.value.isReleasing)
        }

    @Test
    fun `GIVEN the release of Bruno on its way WHEN tapping Liberar nome on Carla THEN it stays on Bruno`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(members = listOf(pierre, ana, bruno, carla.copy(hasJoined = true))),
                waitsForServer = true,
            )
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))
            viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)

            // When
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-4"))

            // Then
            assertEquals(
                "member-3",
                viewModel.uiState.value.memberToRelease
                    ?.id,
            )
            assertTrue(viewModel.uiState.value.isReleasing)
        }

    @Test
    fun `GIVEN a group with members WHEN the screen opens and a sync changes it THEN sends no analytics event`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.value =
                listOf(
                    trip.copy(
                        name = "Floripa",
                        members = listOf(pierre, ana, bruno, carla, diego),
                    ),
                )

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the screen open WHEN tapping share THEN sends the share event with no params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(MembersUiEvent.OnShareClicked)

            // Then
            assertEquals(listOf("members_share_clicked" to noParams), trackedEvents)
        }

    @Test
    fun `GIVEN the group code WHEN copying the link of the link card THEN sends the copy event from members`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)

            // When
            viewModel.onEvent(MembersUiEvent.OnLinkCopyClicked)

            // Then
            assertEquals(listOf("invite_link_copy_clicked" to fromMembers), trackedEvents)
        }

    @Test
    fun `GIVEN the screen open WHEN tapping add THEN sends the add event with no params`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(MembersUiEvent.OnAddClicked)

        // Then
        assertEquals(listOf("member_add_clicked" to noParams), trackedEvents)
    }

    @Test
    fun `GIVEN a guest WHEN tapping their row THEN sends the person event from Membros without the member id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(MembersUiEvent.OnMemberClicked("member-4"))

            // Then
            assertEquals(listOf("person_clicked" to fromMembers), trackedEvents)
        }

    @Test
    fun `GIVEN an unknown member WHEN tapping their row THEN still sends the person event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(MembersUiEvent.OnMemberClicked("member-9"))

        // Then
        assertEquals(listOf("person_clicked" to fromMembers), trackedEvents)
    }

    @Test
    fun `GIVEN the screen open WHEN tapping rename on a member THEN sends the rename event without the member id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-4"))

            // Then
            assertEquals(listOf("member_rename_clicked" to noParams), trackedEvents)
        }

    @Test
    fun `GIVEN Carla with her personal link WHEN tapping her personal invite THEN sends the invite event alone`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // Then
            assertEquals(listOf("personal_invite_clicked" to noParams), trackedEvents)
        }

    @Test
    fun `GIVEN Carla without her link yet WHEN tapping her personal invite THEN still sends the invite event`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // Then
            assertEquals(listOf("personal_invite_clicked" to noParams), trackedEvents)
        }

    @Test
    fun `GIVEN the personal invite of Carla open WHEN dismissing it THEN sends nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteDismissed)

            // Then
            assertEquals(listOf("personal_invite_clicked" to noParams), trackedEvents)
        }

    @Test
    fun `GIVEN the personal invite of Carla open WHEN copying THEN sends the copy event without the link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalLinkCopyClicked)

            // Then
            assertEquals(
                listOf(
                    "personal_invite_clicked" to noParams,
                    "invite_link_copy_clicked" to mapOf<String, Any>("source" to "personal_invite"),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN no personal invite open WHEN copying the personal link THEN still sends the copy event`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalLinkCopyClicked)

            // Then
            assertEquals(
                listOf("invite_link_copy_clicked" to mapOf<String, Any>("source" to "personal_invite")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the personal invite of Carla open WHEN sharing THEN sends the share event without the link or names`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithCodes)
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalLinkShareClicked)

            // Then
            assertEquals(
                listOf(
                    "personal_invite_clicked" to noParams,
                    "invite_link_share_clicked" to mapOf<String, Any>("source" to "personal_invite"),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN someone who has not joined WHEN tapping remove THEN sends the remove event without the member id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))

            // Then
            assertEquals(listOf("member_remove_clicked" to noParams), trackedEvents)
        }

    @Test
    fun `GIVEN someone who joined WHEN tapping remove THEN still sends the remove event though nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-3"))

            // Then
            assertNull(viewModel.uiState.value.memberToRemove)
            assertEquals(listOf("member_remove_clicked" to noParams), trackedEvents)
        }

    @Test
    fun `GIVEN the removal of Carla asked WHEN confirming THEN sends the confirmation after the tap`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))

            // When
            viewModel.onEvent(MembersUiEvent.OnRemoveConfirmed)

            // Then
            assertEquals(
                listOf(
                    "member_remove_clicked" to noParams,
                    "member_remove_confirmed" to noParams,
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN no removal asked WHEN a confirmation arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(MembersUiEvent.OnRemoveConfirmed)

        // Then
        assertEquals(emptyList(), trackedEvents)
    }

    @Test
    fun `GIVEN no release asked WHEN a confirmation arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)

        // Then
        assertEquals(emptyList(), trackedEvents)
    }

    @Test
    fun `GIVEN Carla removed WHEN the Apple alert then closes itself THEN sends no cancellation`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))
            viewModel.onEvent(MembersUiEvent.OnRemoveConfirmed)

            // When
            viewModel.onEvent(MembersUiEvent.OnRemoveDismissed)

            // Then
            assertEquals(
                listOf(
                    "member_remove_clicked" to noParams,
                    "member_remove_confirmed" to noParams,
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the removal of Carla asked WHEN dismissing it THEN sends the cancellation after the tap`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))

            // When
            viewModel.onEvent(MembersUiEvent.OnRemoveDismissed)

            // Then
            assertEquals(
                listOf(
                    "member_remove_clicked" to noParams,
                    "member_remove_cancelled" to noParams,
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Bruno joined WHEN the creator taps Liberar nome THEN sends the release event without the member id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))

            // Then
            assertEquals(listOf("member_release_clicked" to noParams), trackedEvents)
        }

    @Test
    fun `GIVEN this device user did not create the group WHEN tapping Liberar nome THEN still sends the event`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(isCreatedByMe = false))

            // When
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))

            // Then
            assertNull(viewModel.uiState.value.memberToRelease)
            assertEquals(listOf("member_release_clicked" to noParams), trackedEvents)
        }

    @Test
    fun `GIVEN no internet WHEN confirming the release of Bruno THEN still sends the confirmation`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(releaseError = IllegalStateException("offline"))
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))

            // When
            viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)

            // Then
            assertEquals(
                listOf(
                    "member_release_clicked" to noParams,
                    "member_release_confirmed" to noParams,
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the release of Bruno asked WHEN dismissing it THEN sends the cancellation after the tap`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))

            // When
            viewModel.onEvent(MembersUiEvent.OnReleaseDismissed)

            // Then
            assertEquals(
                listOf(
                    "member_release_clicked" to noParams,
                    "member_release_cancelled" to noParams,
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the server answering the release of Bruno WHEN dismissing it THEN sends no cancellation`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(waitsForServer = true)
            viewModel.onEvent(MembersUiEvent.OnReleaseClicked("member-3"))
            viewModel.onEvent(MembersUiEvent.OnReleaseConfirmed)

            // When
            viewModel.onEvent(MembersUiEvent.OnReleaseDismissed)

            // Then
            assertEquals(
                listOf(
                    "member_release_clicked" to noParams,
                    "member_release_confirmed" to noParams,
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the add editor open WHEN typing a name THEN sends nothing more`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(MembersUiEvent.OnAddClicked)

        // When
        viewModel.onEvent(MembersUiEvent.OnDraftChanged("Diego"))

        // Then
        assertEquals(listOf("member_add_clicked" to noParams), trackedEvents)
    }

    @Test
    fun `GIVEN a free name typed in the add editor WHEN saving THEN sends the save event without the name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Diego"))

            // When
            viewModel.onEvent(MembersUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                listOf(
                    "member_add_clicked" to noParams,
                    "member_editor_save_clicked" to noParams,
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a blank draft WHEN saving THEN still sends the save event though nothing is written`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)

            // When
            viewModel.onEvent(MembersUiEvent.OnSaveClicked)

            // Then
            assertTrue(groups.addedMembers.isEmpty())
            assertEquals(
                listOf(
                    "member_add_clicked" to noParams,
                    "member_editor_save_clicked" to noParams,
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the rename editor open WHEN dismissing it THEN sends nothing more`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-4"))

        // When
        viewModel.onEvent(MembersUiEvent.OnEditorDismissed)

        // Then
        assertEquals(listOf("member_rename_clicked" to noParams), trackedEvents)
    }

    @Test
    fun `GIVEN Invite asked to add a person to the group WHEN the screen opens on top THEN Add person is open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(askedGroupId = "group-1")

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                MemberNameEditor(
                    memberId = null,
                    draft = "",
                ),
                state.editor,
            )
        }

    @Test
    fun `GIVEN the screen on top WHEN Invite asks to add a person to the group THEN Add person opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val groupId = "group-1"

            // When
            addPersonRequests.post(groupId)

            // Then
            assertEquals(
                MemberNameEditor(
                    memberId = null,
                    draft = "",
                ),
                viewModel.uiState.value.editor,
            )
        }

    @Test
    fun `GIVEN the screen on top WHEN Invite asks to add a person to another group THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val groupId = "group-2"

            // When
            addPersonRequests.post(groupId)

            // Then
            assertNull(viewModel.uiState.value.editor)
            assertEquals("group-2", addPersonRequests.observeRequest().first())
        }

    @Test
    fun `GIVEN Invite over the screen WHEN Invite asks to add a person to the group THEN nothing opens yet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Invite("group-1", isNewGroup = false))

            // When
            addPersonRequests.post("group-1")

            // Then
            assertNull(viewModel.uiState.value.editor)
        }

    @Test
    fun `GIVEN an ask while Invite is over the screen WHEN Invite closes THEN Add person opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Invite("group-1", isNewGroup = false))
            addPersonRequests.post("group-1")

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                MemberNameEditor(
                    memberId = null,
                    draft = "",
                ),
                viewModel.uiState.value.editor,
            )
        }

    @Test
    fun `GIVEN another tab of the group on top WHEN Invite asks to add a person to the group THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.GroupDetail("group-1", joinedAs = null, GroupTab.EXPENSES))

            // When
            addPersonRequests.post("group-1")

            // Then
            assertNull(viewModel.uiState.value.editor)
        }

    @Test
    fun `GIVEN Add person opened from the ask WHEN nothing closes it THEN the ask is still there`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(askedGroupId = "group-1")

            // When
            val ask = addPersonRequests.observeRequest().first()

            // Then
            assertEquals("group-1", ask)
        }

    @Test
    fun `GIVEN Add person opened from the ask WHEN dismissing it THEN closes it and drops the ask`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(askedGroupId = "group-1")

            // When
            viewModel.onEvent(MembersUiEvent.OnEditorDismissed)

            // Then
            assertNull(viewModel.uiState.value.editor)
            assertNull(addPersonRequests.observeRequest().first())
        }

    @Test
    fun `GIVEN a free name typed in Add person opened from the ask WHEN saving THEN adds them and drops the ask`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(askedGroupId = "group-1")
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Diego"))

            // When
            viewModel.onEvent(MembersUiEvent.OnSaveClicked)

            // Then
            assertNull(viewModel.uiState.value.editor)
            assertEquals(listOf("group-1" to "Diego"), groups.addedMembers.map { it.first to it.second.name })
            assertNull(addPersonRequests.observeRequest().first())
        }

    @Test
    fun `GIVEN Add person from the ask WHEN saving a name already in the group THEN adds it and drops the ask`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(askedGroupId = "group-1")
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Ana"))

            // When
            viewModel.onEvent(MembersUiEvent.OnSaveClicked)

            // Then
            assertNull(viewModel.uiState.value.editor)
            assertEquals(listOf("Ana"), groups.addedMembers.map { it.second.name })
            assertNull(addPersonRequests.observeRequest().first())
        }

    @Test
    fun `GIVEN Add person opened from the ask WHEN tapping invite with the link THEN opens Invite and drops the ask`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(askedGroupId = "group-1")

            // When
            viewModel.onEvent(MembersUiEvent.OnInviteInsteadClicked)

            // Then
            assertNull(viewModel.uiState.value.editor)
            assertEquals(
                listOf(Route.Groups, membersRoute, Route.Invite("group-1", isNewGroup = false)),
                navigator.backStack.value,
            )
            assertNull(addPersonRequests.observeRequest().first())
        }

    @Test
    fun `GIVEN Add person from the ask dismissed WHEN the screen comes back on top THEN Add person stays closed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(askedGroupId = "group-1")
            viewModel.onEvent(MembersUiEvent.OnEditorDismissed)
            navigator.navigate(Route.Profile)

            // When
            navigator.navigateBack()

            // Then
            assertNull(viewModel.uiState.value.editor)
        }

    @Test
    fun `GIVEN Add person from the ask dismissed WHEN Invite asks again THEN Add person opens again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(askedGroupId = "group-1")
            viewModel.onEvent(MembersUiEvent.OnEditorDismissed)

            // When
            addPersonRequests.post("group-1")

            // Then
            assertEquals(
                MemberNameEditor(
                    memberId = null,
                    draft = "",
                ),
                viewModel.uiState.value.editor,
            )
        }

    @Test
    fun `GIVEN the first of a screen mounted twice gone with Add person open WHEN the second opens THEN shows it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(askedGroupId = "group-1")
            clear(viewModel)
            val second = membersViewModel()

            // When
            val state = second.uiState.value

            // Then
            assertEquals(
                MemberNameEditor(
                    memberId = null,
                    draft = "",
                ),
                state.editor,
            )
            assertEquals("group-1", addPersonRequests.observeRequest().first())
        }

    @Test
    fun `GIVEN Add person open from the ask and a screen over the group WHEN the screen goes THEN the ask stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(askedGroupId = "group-1")
            navigator.navigate(Route.Profile)

            // When
            clear(viewModel)

            // Then
            assertEquals("group-1", addPersonRequests.observeRequest().first())
        }

    @Test
    fun `GIVEN Add person open from the ask and the group left WHEN the screen goes THEN the ask is dropped`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(askedGroupId = "group-1")
            navigator.navigateBack()

            // When
            clear(viewModel)

            // Then
            assertNull(addPersonRequests.observeRequest().first())
        }

    @Test
    fun `GIVEN the rename editor open WHEN Invite asks to add a person to the group THEN the rename stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-4"))

            // When
            addPersonRequests.post("group-1")

            // Then
            assertEquals(
                "member-4",
                viewModel.uiState.value.editor
                    ?.memberId,
            )
            assertEquals(
                "Carla",
                viewModel.uiState.value.editor
                    ?.draft,
            )
        }

    @Test
    fun `GIVEN an ask that came while renaming WHEN dismissing the rename THEN Add person opens from it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRenameClicked("member-4"))
            addPersonRequests.post("group-1")

            // When
            viewModel.onEvent(MembersUiEvent.OnEditorDismissed)

            // Then
            assertEquals(MemberNameEditor(null, ""), viewModel.uiState.value.editor)
            assertEquals("group-1", addPersonRequests.observeRequest().first())
        }

    @Test
    fun `GIVEN an ask that came while adding by tapping add WHEN saving THEN adds them and Add person opens from it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)
            viewModel.onEvent(MembersUiEvent.OnDraftChanged("Diego"))
            addPersonRequests.post("group-1")

            // When
            viewModel.onEvent(MembersUiEvent.OnSaveClicked)

            // Then
            assertEquals(listOf("Diego"), groups.addedMembers.map { it.second.name })
            assertEquals(MemberNameEditor(null, ""), viewModel.uiState.value.editor)
            assertEquals("group-1", addPersonRequests.observeRequest().first())
        }

    @Test
    fun `GIVEN an ask before Room is read WHEN the screen opens THEN Add person waits for the members`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                askedGroupId = "group-1",
                isRoomReadAtStart = false,
            )
            val editorBeforeRead = viewModel.uiState.value.editor

            // When
            isRoomRead.value = true

            // Then
            assertNull(editorBeforeRead)
            assertEquals(MemberNameEditor(null, ""), viewModel.uiState.value.editor)
        }

    @Test
    fun `GIVEN an ask behind a tapped Add person WHEN tapping invite with the link THEN only Invite opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)
            addPersonRequests.post("group-1")

            // When
            viewModel.onEvent(MembersUiEvent.OnInviteInsteadClicked)

            // Then
            assertNull(viewModel.uiState.value.editor)
            assertEquals(Route.Invite("group-1", isNewGroup = false), navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN Add person opened by tapping add WHEN tapping invite with the link THEN closes it and opens Invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)

            // When
            viewModel.onEvent(MembersUiEvent.OnInviteInsteadClicked)

            // Then
            assertNull(viewModel.uiState.value.editor)
            assertEquals(
                listOf(Route.Groups, membersRoute, Route.Invite("group-1", isNewGroup = false)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Add person open and a bill beside it WHEN tapping invite with the link THEN Invite replaces the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)
            navigator.navigate(Route.BillDetail("group-1", "dinner", focusCommentId = null))

            // When
            viewModel.onEvent(MembersUiEvent.OnInviteInsteadClicked)

            // Then
            assertNull(viewModel.uiState.value.editor)
            assertEquals(
                listOf(Route.Groups, membersRoute, Route.Invite("group-1", isNewGroup = false)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Add person open WHEN tapping invite with the link THEN sends the invite event with no params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnAddClicked)

            // When
            viewModel.onEvent(MembersUiEvent.OnInviteInsteadClicked)

            // Then
            assertEquals(
                listOf(
                    "member_add_clicked" to noParams,
                    "member_editor_invite_clicked" to noParams,
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a group with no bill WHEN the screen opens THEN lists only its default currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(GroupCurrency.BRL), state.currencies)
            assertEquals(GroupCurrency.BRL, state.currency)
        }

    @Test
    fun `GIVEN bills in Reais and Dollars WHEN the screen opens THEN lists both and shows the default one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = reaisAndDollarBills)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), state.currencies)
            assertEquals(GroupCurrency.BRL, state.currency)
        }

    @Test
    fun `GIVEN more bills in other currencies than the default WHEN the screen opens THEN the default is first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ),
                    expense(
                        "bill-2",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ).copy(currency = GroupCurrency.USD),
                    expense(
                        "bill-3",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ).copy(currency = GroupCurrency.USD),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), state.currencies)
        }

    @Test
    fun `GIVEN other currencies with different bill counts WHEN the screen opens THEN the busiest is first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ).copy(currency = GroupCurrency.USD),
                    expense(
                        "bill-2",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ).copy(currency = GroupCurrency.EUR),
                    expense(
                        "bill-3",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ).copy(currency = GroupCurrency.USD),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD, GroupCurrency.EUR), state.currencies)
        }

    @Test
    fun `GIVEN other currencies with as many bills each WHEN the screen opens THEN orders them by code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ).copy(currency = GroupCurrency.USD),
                    expense(
                        "bill-2",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ).copy(currency = GroupCurrency.EUR),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.EUR, GroupCurrency.USD), state.currencies)
        }

    @Test
    fun `GIVEN a payment in Dollars and no bill in them WHEN the screen opens THEN Dollars are listed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ),
                ),
                storedSettlements = listOf(
                    settlement(
                        "pay-1",
                        from = "member-4",
                        to = "member-1",
                        amountMinor = 1_000,
                    ).copy(currency = GroupCurrency.USD),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), state.currencies)
        }

    @Test
    fun `GIVEN bills in two currencies WHEN the screen opens THEN each member shows the default currency balance`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = reaisAndDollarBills)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                MemberBalance(
                    receivesMinor = 5_000,
                    owesMinor = 0,
                ),
                state.members[0].balance,
            )
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 5_000,
                ),
                state.members[1].balance,
            )
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 0,
                ),
                state.members[3].balance,
            )
        }

    @Test
    fun `GIVEN bills in two currencies WHEN picking Dollars THEN shows the currency and the balances in it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = reaisAndDollarBills)

            // When
            viewModel.onEvent(MembersUiEvent.OnCurrencySelected(GroupCurrency.USD))

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.USD, state.currency)
            assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), state.currencies)
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 1_500,
                ),
                state.members[0].balance,
            )
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 0,
                ),
                state.members[1].balance,
            )
            assertEquals(
                MemberBalance(
                    receivesMinor = 1_500,
                    owesMinor = 0,
                ),
                state.members[3].balance,
            )
        }

    @Test
    fun `GIVEN Dollars picked WHEN picking Reais again THEN goes back to the balances in Reais`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = reaisAndDollarBills)
            viewModel.onEvent(MembersUiEvent.OnCurrencySelected(GroupCurrency.USD))

            // When
            viewModel.onEvent(MembersUiEvent.OnCurrencySelected(GroupCurrency.BRL))

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.BRL, state.currency)
            assertEquals(
                MemberBalance(
                    receivesMinor = 5_000,
                    owesMinor = 0,
                ),
                state.members[0].balance,
            )
        }

    @Test
    fun `GIVEN bills in two currencies WHEN picking one the group has no money in THEN nothing changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = reaisAndDollarBills)
            val before = viewModel.uiState.value

            // When
            viewModel.onEvent(MembersUiEvent.OnCurrencySelected(GroupCurrency.JPY))

            // Then
            assertEquals(before, viewModel.uiState.value)
        }

    @Test
    fun `GIVEN a pick the group has no money in WHEN picking Dollars after it THEN Dollars show`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = reaisAndDollarBills)
            viewModel.onEvent(MembersUiEvent.OnCurrencySelected(GroupCurrency.JPY))

            // When
            viewModel.onEvent(MembersUiEvent.OnCurrencySelected(GroupCurrency.USD))

            // Then
            assertEquals(GroupCurrency.USD, viewModel.uiState.value.currency)
        }

    @Test
    fun `GIVEN Dollars picked WHEN a sync brings another bill THEN Dollars stay picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = reaisAndDollarBills)
        viewModel.onEvent(MembersUiEvent.OnCurrencySelected(GroupCurrency.USD))

        // When
        expenses.expenses.value = expenses.expenses.value +
            expense(
                "bill-9",
                paidBy = "member-1",
                splitWith = listOf("member-1", "member-2"),
            )

        // Then
        assertEquals(GroupCurrency.USD, viewModel.uiState.value.currency)
    }

    @Test
    fun `GIVEN Dollars picked WHEN a sync leaves the group with no money in Dollars THEN falls back to Reais`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = reaisAndDollarBills)
            viewModel.onEvent(MembersUiEvent.OnCurrencySelected(GroupCurrency.USD))

            // When
            expenses.expenses.value = expenses.expenses.value.filter { it.currency == GroupCurrency.BRL }

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.BRL, state.currency)
            assertEquals(listOf(GroupCurrency.BRL), state.currencies)
            assertEquals(
                MemberBalance(
                    receivesMinor = 5_000,
                    owesMinor = 0,
                ),
                state.members[0].balance,
            )
        }

    @Test
    fun `GIVEN Carla has a balance only in Dollars WHEN Reais are shown THEN she cannot be removed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = reaisAndDollarBills)

            // When
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))

            // Then
            val carla = viewModel.uiState.value.members
                .first { it.id == "member-4" }
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 0,
                ),
                carla.balance,
            )
            assertTrue(carla.hasOpenBalance)
            assertFalse(carla.canBeRemoved)
            assertNull(viewModel.uiState.value.memberToRemove)
        }

    @Test
    fun `GIVEN Carla has a balance only in Dollars WHEN Dollars are picked THEN she still cannot be removed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = reaisAndDollarBills)
            viewModel.onEvent(MembersUiEvent.OnCurrencySelected(GroupCurrency.USD))

            // When
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))

            // Then
            assertTrue(
                viewModel.uiState.value.members
                    .first { it.id == "member-4" }
                    .hasOpenBalance,
            )
            assertNull(viewModel.uiState.value.memberToRemove)
        }

    @Test
    fun `GIVEN Carla owes only in Reais WHEN Dollars are picked THEN she still cannot be removed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ),
                    expense(
                        "bill-2",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-2"),
                    ).copy(currency = GroupCurrency.USD),
                ),
            )
            viewModel.onEvent(MembersUiEvent.OnCurrencySelected(GroupCurrency.USD))

            // When
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))

            // Then
            assertTrue(
                viewModel.uiState.value.members
                    .first { it.id == "member-4" }
                    .hasOpenBalance,
            )
            assertNull(viewModel.uiState.value.memberToRemove)
        }

    @Test
    fun `GIVEN Carla settled in every currency WHEN removing her THEN asks to remove her`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedExpenses = reaisAndDollarBills,
            storedSettlements = listOf(
                settlement(
                    "pay-1",
                    from = "member-1",
                    to = "member-4",
                    amountMinor = 1_500,
                ).copy(currency = GroupCurrency.USD),
            ),
        )
        viewModel.onEvent(MembersUiEvent.OnCurrencySelected(GroupCurrency.USD))

        // When
        viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))

        // Then
        assertFalse(
            viewModel.uiState.value.members
                .first { it.id == "member-4" }
                .hasOpenBalance,
        )
        assertEquals(
            "member-4",
            viewModel.uiState.value.memberToRemove
                ?.id,
        )
    }

    @Test
    fun `GIVEN removal of Carla asked WHEN a sync brings her a debt in another currency THEN the question closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(MembersUiEvent.OnRemoveClicked("member-4"))

            // When
            expenses.expenses.value = listOf(
                expense(
                    "bill-1",
                    paidBy = "member-1",
                    splitWith = listOf("member-1", "member-4"),
                ).copy(currency = GroupCurrency.USD),
            )

            // Then
            assertNull(viewModel.uiState.value.memberToRemove)
        }

    @Test
    fun `GIVEN Carla with a Reais bill and a Dollars bill WHEN opening her personal invite THEN counts only Reais`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = tripWithCodes,
                storedExpenses = carlaInTwoCurrencies,
            )

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // Then
            val invite = viewModel.uiState.value.personalInvite
            assertEquals(1, invite?.billCount)
            assertEquals(5_000L, invite?.billShareMinor)
        }

    @Test
    fun `GIVEN Dollars picked WHEN opening the personal invite of Carla THEN counts only the Dollars bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = tripWithCodes,
                storedExpenses = carlaInTwoCurrencies,
            )
            viewModel.onEvent(MembersUiEvent.OnCurrencySelected(GroupCurrency.USD))

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // Then
            val invite = viewModel.uiState.value.personalInvite
            assertEquals(2, invite?.billCount)
            assertEquals(3_000L, invite?.billShareMinor)
        }

    @Test
    fun `GIVEN the personal invite of Carla open WHEN picking Dollars THEN the invite counts the Dollars bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = tripWithCodes,
                storedExpenses = carlaInTwoCurrencies,
            )
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // When
            viewModel.onEvent(MembersUiEvent.OnCurrencySelected(GroupCurrency.USD))

            // Then
            val invite = viewModel.uiState.value.personalInvite
            assertEquals(carlaLink, invite?.link)
            assertEquals(2, invite?.billCount)
            assertEquals(3_000L, invite?.billShareMinor)
        }

    @Test
    fun `GIVEN a Dollars bill for Carla WHEN opening her invite with Reais shown THEN counts no bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = tripWithCodes,
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-1",
                        splitWith = listOf("member-1", "member-4"),
                    ).copy(currency = GroupCurrency.USD),
                ),
            )

            // When
            viewModel.onEvent(MembersUiEvent.OnPersonalInviteClicked("member-4"))

            // Then
            val invite = viewModel.uiState.value.personalInvite
            assertEquals(0, invite?.billCount)
            assertEquals(0L, invite?.billShareMinor)
        }

    @Test
    fun `GIVEN a currency chip WHEN tapping Dollars THEN sends the currency event from Membros`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = reaisAndDollarBills)

            // When
            viewModel.onEvent(MembersUiEvent.OnCurrencySelected(GroupCurrency.USD))

            // Then
            assertEquals(listOf("balances_currency_clicked" to fromMembers), trackedEvents)
        }

    // Why: Ana owes Pierre in Reais, and Pierre owes Carla in Dollars: nobody is in both.
    private val reaisAndDollarBills = listOf(
        expense(
            "bill-1",
            paidBy = "member-1",
            splitWith = listOf("member-1", "member-2"),
        ),
        expense(
            "bill-2",
            paidBy = "member-4",
            splitWith = listOf("member-4", "member-1"),
            amountMinor = 3_000,
        ).copy(currency = GroupCurrency.USD),
    )

    // Why: Carla has one part in Reais and two in Dollars.
    private val carlaInTwoCurrencies = listOf(
        expense(
            "bill-1",
            paidBy = "member-1",
            splitWith = listOf("member-1", "member-4"),
        ),
        expense(
            "bill-2",
            paidBy = "member-1",
            splitWith = listOf("member-1", "member-4"),
            amountMinor = 2_000,
        ).copy(currency = GroupCurrency.USD),
        expense(
            "bill-3",
            paidBy = "member-2",
            splitWith = listOf("member-2", "member-4"),
            amountMinor = 4_000,
        ).copy(currency = GroupCurrency.USD),
    )

    private val noParams = emptyMap<String, Any>()

    // Why: the params of the events that name the screen they come from.
    private val fromMembers = mapOf<String, Any>("source" to "members")

    private val pierre = GroupMember(
        "member-1",
        "Pierre",
        AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )
    private val ana = GroupMember(
        "member-2",
        "Ana",
        AvatarColor.PEACH,
        isMe = false,
        hasJoined = false,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )
    private val bruno = GroupMember(
        "member-3",
        "Bruno",
        AvatarColor.GREEN,
        isMe = false,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )
    private val carla = GroupMember(
        "member-4",
        "Carla",
        AvatarColor.LILAC,
        isMe = false,
        hasJoined = false,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )
    private val diego = GroupMember(
        "member-5",
        "Diego",
        AvatarColor.SAND,
        isMe = false,
        hasJoined = false,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )
    private val anaP = GroupMember(
        "member-6",
        "Ana P.",
        AvatarColor.SAND,
        isMe = false,
        hasJoined = false,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private val carlaOnScreen = MembersMember(
        "member-4",
        "Carla",
        "C",
        AvatarColor.LILAC,
        photoUrl = null,
        isMe = false,
        hasJoined = false,
        personalLink = null,
        balance = null,
        hasOpenBalance = false,
        canBeReleased = false,
    )

    private val brunoOnScreen = MembersMember(
        "member-3",
        "Bruno",
        "B",
        AvatarColor.GREEN,
        photoUrl = null,
        isMe = false,
        hasJoined = true,
        personalLink = null,
        balance = null,
        hasOpenBalance = false,
        canBeReleased = true,
    )

    private val trip = Group(
        id = "group-1",
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.BLUE,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(pierre, ana, bruno, carla),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private val pierreWithCode = pierre.copy(inviteCode = "p1Er2e")
    private val brunoWithCode = bruno.copy(inviteCode = "b7Ru2n")
    private val carlaWithCode = carla.copy(inviteCode = "c3Rt9z")

    private val tripWithCodes = trip.copy(
        members = listOf(pierreWithCode, ana, brunoWithCode, carlaWithCode),
        inviteCode = "7Hk2pQ8x",
    )

    private val carlaLink = "https://blitzsplit.com/g/7Hk2pQ8x/c3Rt9z?from=p1Er2e"
    private val carlaWithLink = carlaOnScreen.copy(personalLink = carlaLink)

    // Why: one person paid the whole bill, split evenly between the members in it.
    private fun expense(
        id: String,
        paidBy: String,
        splitWith: List<String>,
        amountMinor: Long = 10_000,
    ): Expense = Expense(
        id = id,
        groupId = "group-1",
        description = "Conta $id",
        currency = GroupCurrency.BRL,
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(paidBy, amountMinor)),
        shares = splitWith.map { ExpenseShare(it, amountMinor / splitWith.size, weight = 1) },
        items = null,
        categoryId = null,
        spentAt = 1_000L,
        isPending = false,
        updatedAt = 1_000L,
        updatedByMemberId = null,
    )

    private fun settlement(
        id: String,
        from: String,
        to: String,
        amountMinor: Long,
    ): Settlement = Settlement(
        id = id,
        groupId = "group-1",
        fromMemberId = from,
        toMemberId = to,
        currency = GroupCurrency.BRL,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = 1_000L,
        isPending = false,
        updatedAt = 0L,
    )

    // Why: the group's screen on its Members tab, whose ViewModel this is.
    private val membersRoute = Route.GroupDetail("group-1", joinedAs = null, GroupTab.MEMBERS)

    // Why: what the platform does when the screen goes, which runs the ViewModel's onCleared.
    private fun clear(viewModel: MembersViewModel) {
        ViewModelStore().apply { put("members", viewModel) }.clear()
    }

    // Why: another ViewModel of the same screen over the same fakes, as a screen mounted twice builds.
    private fun membersViewModel(): MembersViewModel {
        val observeGroup = ObserveGroupUseCase(groups)
        return MembersViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = membersRoute,
            navigator = navigator,
            checkMemberName = CheckMemberNameUseCase(),
            addMember = AddMemberUseCase(groups, observeGroup) { 1_000L },
            renameMember = RenameMemberUseCase(groups) { 1_000L },
            removeMember = RemoveMemberUseCase(groups) { 1_000L },
            releaseMember = ReleaseMemberUseCase(groups),
            consumeAddPersonRequest = ConsumeAddPersonRequestUseCase(addPersonRequests),
            observeAddPersonRequest = ObserveAddPersonRequestUseCase(addPersonRequests),
            observeGroup = ObserveGroup { groupId ->
                isRoomRead.flatMapLatest { isRead -> if (isRead) observeGroup(groupId) else emptyFlow() }
            },
            observeExpenses = ObserveExpensesUseCase(expenses),
            observeSettlements = ObserveSettlementsUseCase(settlements),
        )
    }

    private fun TestScope.prepareScenario(
        storedGroup: Group = trip,
        isRoomReadAtStart: Boolean = true,
        memberChangeError: Throwable? = null,
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
        releaseError: Throwable? = null,
        waitsForServer: Boolean = false,
        // Why: Invite's "Add someone without the app", asked for this group before the screen opens.
        askedGroupId: String? = null,
    ) {
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        isRoomRead.value = isRoomReadAtStart
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup)).also {
            it.memberChangeError = memberChangeError
            it.releaseError = releaseError
            if (waitsForServer) it.releaseGate = serverAnswer
        }
        navigator =
            BackStackNavigator(
                startRoute = Route.Groups,
            ).also { it.navigate(membersRoute) }
        addPersonRequests = InMemoryAddPersonRequestRepository().also { requests ->
            askedGroupId?.let(requests::post)
        }
        viewModel = membersViewModel()
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
