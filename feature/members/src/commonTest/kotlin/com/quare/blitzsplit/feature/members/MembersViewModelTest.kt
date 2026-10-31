package com.quare.blitzsplit.feature.members

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RecordSettlementUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.AddMemberUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CheckMemberNameUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ReleaseMemberUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.RemoveMemberUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.RenameMemberUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
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
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var account: FakeAccountRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private val recordedPayments = InMemoryRecordedPaymentRepository()
    private val isRoomRead = MutableStateFlow(true)
    private lateinit var recordSettlement: RecordSettlementUseCase
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
                        isMe = true,
                        hasJoined = true,
                        personalLink = null,
                        balance = null,
                        canBeReleased = false,
                    ),
                    MembersMember(
                        "member-2",
                        "Ana",
                        "AN",
                        AvatarColor.PEACH,
                        isMe = false,
                        hasJoined = false,
                        personalLink = null,
                        balance = null,
                        canBeReleased = false,
                    ),
                    MembersMember(
                        "member-3",
                        "Bruno",
                        "BR",
                        AvatarColor.GREEN,
                        isMe = false,
                        hasJoined = true,
                        personalLink = null,
                        balance = null,
                        canBeReleased = true,
                    ),
                    MembersMember(
                        "member-4",
                        "Carla",
                        "CA",
                        AvatarColor.LILAC,
                        isMe = false,
                        hasJoined = false,
                        personalLink = null,
                        balance = null,
                        canBeReleased = false,
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
                listOf(Route.Groups, Route.Members("group-1", joinedAs = null), Route.Invite("group-1")),
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
            listOf(Route.Groups, Route.Members("group-1", joinedAs = null), Route.Invite("group-1")),
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
                listOf(Route.Groups, Route.Members("group-1", joinedAs = null), Route.Invite("group-1")),
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
            listOf(Route.Groups, Route.Members("group-1", joinedAs = null), Route.Profile),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the screen open WHEN tapping the balances tab THEN Saldos takes its place`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = MembersUiEvent.OnBalancesTabClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf(Route.Groups, Route.Balances("group-1")), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the screen open WHEN tapping the expenses tab THEN the group takes its place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(MembersUiEvent.OnExpensesTabClicked)

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail("group-1")), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, Route.Members("group-1", joinedAs = null)), navigator.backStack.value)
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
                ),
                viewModel.uiState.value.personalInvite,
            )
            assertTrue(actions.isEmpty())
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
    fun `GIVEN just joined with an anonymous account WHEN tapping a personal invite THEN opens it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = tripWithCodes,
                joinedAs = "Pierre",
                accountStatus = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID),
            )
            val event = MembersUiEvent.OnPersonalInviteClicked("member-4")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                carlaLink,
                viewModel.uiState.value.personalInvite
                    ?.link,
            )
        }

    @Test
    fun `GIVEN connected with Google WHEN tapping a personal invite THEN opens it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = tripWithCodes,
            accountStatus = withGoogle,
        )
        val event = MembersUiEvent.OnPersonalInviteClicked("member-4")

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            carlaLink,
            viewModel.uiState.value.personalInvite
                ?.link,
        )
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

    @Test
    fun `GIVEN the group was just joined as Bruno WHEN the screen opens THEN says Bruno joined`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinedAs = "Bruno")

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("Bruno", state.joinedAs)
        }

    @Test
    fun `GIVEN the screen opened from the groups list WHEN it opens THEN there is no joined banner`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinedAs = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.joinedAs)
        }

    @Test
    fun `GIVEN just joined with a local only account WHEN the screen opens THEN asks to protect the access`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                joinedAs = "Bruno",
                accountStatus = AccountStatus.LocalOnly,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isAccountUnprotected)
            assertTrue(state.showsProtectAccess)
        }

    @Test
    fun `GIVEN just joined with an anonymous account WHEN the screen opens THEN asks to protect the access`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                joinedAs = "Bruno",
                accountStatus = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isAccountUnprotected)
            assertTrue(state.showsProtectAccess)
        }

    @Test
    fun `GIVEN just joined with Google WHEN the screen opens THEN does not ask to protect the access`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                joinedAs = "Bruno",
                accountStatus = AccountStatus.Connected(
                    FakeAccountRepository.CONNECTED_USER_ID,
                    AccountProvider.GOOGLE,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isAccountUnprotected)
            assertFalse(state.showsProtectAccess)
        }

    @Test
    fun `GIVEN just joined with Apple WHEN the screen opens THEN does not ask to protect the access`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                joinedAs = "Bruno",
                accountStatus = withApple,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isAccountUnprotected)
            assertFalse(state.showsProtectAccess)
        }

    @Test
    fun `GIVEN asked to protect the access WHEN Google gets connected THEN stops asking`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            joinedAs = "Bruno",
            accountStatus = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID),
        )

        // When
        account.status = withGoogle

        // Then
        assertFalse(viewModel.uiState.value.showsProtectAccess)
        assertEquals("Bruno", viewModel.uiState.value.joinedAs)
    }

    @Test
    fun `GIVEN just joined with Google WHEN Google is removed THEN asks to protect the access`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                joinedAs = "Bruno",
                accountStatus = AccountStatus.Connected(
                    FakeAccountRepository.CONNECTED_USER_ID,
                    AccountProvider.GOOGLE,
                ),
            )

            // When
            account.status = AccountStatus.Anonymous(FakeAccountRepository.CONNECTED_USER_ID)

            // Then
            assertTrue(viewModel.uiState.value.showsProtectAccess)
        }

    @Test
    fun `GIVEN opened from the groups list with a local only account WHEN it opens THEN does not ask to protect`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                joinedAs = null,
                accountStatus = AccountStatus.LocalOnly,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isAccountUnprotected)
            assertFalse(state.showsProtectAccess)
        }

    @Test
    fun `GIVEN opened from the groups list with Google WHEN Google is removed THEN does not ask to protect`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                joinedAs = null,
                accountStatus = AccountStatus.Connected(
                    FakeAccountRepository.CONNECTED_USER_ID,
                    AccountProvider.GOOGLE,
                ),
            )

            // When
            account.status = AccountStatus.Anonymous(FakeAccountRepository.CONNECTED_USER_ID)

            // Then
            assertFalse(viewModel.uiState.value.isAccountUnprotected)
            assertFalse(viewModel.uiState.value.showsProtectAccess)
        }

    @Test
    fun `GIVEN asked to protect the access WHEN tapping it THEN opens the profile over the members`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinedAs = "Bruno")
            val event = MembersUiEvent.OnProtectAccessClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Members("group-1", joinedAs = "Bruno"), Route.Profile),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN another screen on top WHEN tapping protect the access THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinedAs = "Bruno")
            navigator.navigate(Route.Invite("group-1"))

            // When
            viewModel.onEvent(MembersUiEvent.OnProtectAccessClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Members("group-1", joinedAs = "Bruno"), Route.Invite("group-1")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a group without bills WHEN the screen opens THEN there is no balance card and nobody is settled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.myBalance)
            assertFalse(state.isEveryoneSettled)
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
            assertEquals(
                MemberBalance(
                    receivesMinor = 5_000,
                    owesMinor = 0,
                ),
                state.myBalance,
            )
            assertFalse(state.isEveryoneSettled)
        }

    @Test
    fun `GIVEN this device user owes Ana and is owed by Carla WHEN the screen opens THEN the card holds both`() =
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
                state.myBalance,
            )
            assertEquals(
                MemberBalance(
                    receivesMinor = 3_000,
                    owesMinor = 5_000,
                ),
                state.members[0].balance,
            )
        }

    @Test
    fun `GIVEN a bill and a payment that settles it WHEN the screen opens THEN everyone is settled`() =
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
            assertTrue(state.isEveryoneSettled)
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 0,
                ),
                state.myBalance,
            )
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
    fun `GIVEN a group without this device user WHEN a bill is in it THEN there is no balance card`() =
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
            assertNull(state.myBalance)
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
                    receivesMinor = 5_000,
                    owesMinor = 0,
                ),
                state.myBalance,
            )
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
    fun `GIVEN Carla owes this device user WHEN a sync brings her payment THEN everyone is settled`() =
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
            assertTrue(state.isEveryoneSettled)
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 0,
                ),
                state.myBalance,
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
            assertEquals(4, viewModel.uiState.value.memberCount)
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
    fun `GIVEN this device user owes Ana and more to Bruno WHEN tapping Acertar THEN opens the payment to Bruno`() =
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
                        paidBy = "member-3",
                        splitWith = listOf("member-3", "member-1"),
                        amountMinor = 16_000,
                    ),
                ),
            )
            val event = MembersUiEvent.OnSettleClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.Members("group-1", joinedAs = null),
                    Route.RegisterPayment("group-1", "member-1", "member-3"),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the debt to Bruno paid since WHEN tapping Acertar THEN opens the payment to Ana`() =
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
                        paidBy = "member-3",
                        splitWith = listOf("member-3", "member-1"),
                        amountMinor = 16_000,
                    ),
                ),
            )
            settlements.settlements.value =
                listOf(
                    settlement(
                        "pay-1",
                        from = "member-1",
                        to = "member-3",
                        amountMinor = 8_000,
                    ),
                )

            // When
            viewModel.onEvent(MembersUiEvent.OnSettleClicked)

            // Then
            assertEquals(
                Route.RegisterPayment("group-1", "member-1", "member-2"),
                navigator.backStack.value.last(),
            )
        }

    @Test
    fun `GIVEN this device user is only owed WHEN tapping Acertar THEN the back stack stays`() =
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
            val event = MembersUiEvent.OnSettleClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Members("group-1", joinedAs = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a group without bills WHEN tapping Acertar THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = MembersUiEvent.OnSettleClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            listOf(Route.Groups, Route.Members("group-1", joinedAs = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a group without this device user WHEN tapping Acertar THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(members = listOf(pierre.copy(isMe = false), ana, bruno, carla)),
                storedExpenses = listOf(
                    expense(
                        "bill-1",
                        paidBy = "member-2",
                        splitWith = listOf("member-2", "member-1"),
                    ),
                ),
            )
            val event = MembersUiEvent.OnSettleClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Members("group-1", joinedAs = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN another screen on top WHEN tapping Acertar THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedExpenses = listOf(
                expense(
                    "bill-1",
                    paidBy = "member-2",
                    splitWith = listOf("member-2", "member-1"),
                ),
            ),
        )
        navigator.navigate(Route.Invite("group-1"))

        // When
        viewModel.onEvent(MembersUiEvent.OnSettleClicked)

        // Then
        assertEquals(
            listOf(Route.Groups, Route.Members("group-1", joinedAs = null), Route.Invite("group-1")),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN another screen on top WHEN tapping the balances tab THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Invite("group-1"))

            // When
            viewModel.onEvent(MembersUiEvent.OnBalancesTabClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Members("group-1", joinedAs = null), Route.Invite("group-1")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Registrar pagamento over the screen WHEN it closes after recording THEN shows the notice once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.RegisterPayment("group-1", "member-2", "member-1"))
            val settlementId = record(
                from = "member-2",
                to = "member-1",
            )

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                PaymentNotice(
                    settlementId = settlementId,
                    fromName = "Ana",
                    fromIsMe = false,
                    toName = "Pierre",
                ),
                viewModel.uiState.value.recordedPayment,
            )
            assertNull(recordedPayments.observeRecorded().first())
        }

    @Test
    fun `GIVEN this device user paid Bruno WHEN Registrar pagamento closes THEN the notice is from me`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.RegisterPayment("group-1", "member-1", "member-3"))
            val settlementId = record(
                from = "member-1",
                to = "member-3",
            )

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                PaymentNotice(
                    settlementId = settlementId,
                    fromName = "Pierre",
                    fromIsMe = true,
                    toName = "Bruno",
                ),
                viewModel.uiState.value.recordedPayment,
            )
        }

    @Test
    fun `GIVEN a payment from someone no longer in the group WHEN Registrar pagamento closes THEN no payer name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.RegisterPayment("group-1", "member-9", "member-1"))
            val settlementId = record(
                from = "member-9",
                to = "member-1",
            )

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                PaymentNotice(
                    settlementId = settlementId,
                    fromName = null,
                    fromIsMe = false,
                    toName = "Pierre",
                ),
                viewModel.uiState.value.recordedPayment,
            )
        }

    @Test
    fun `GIVEN Registrar pagamento still on top WHEN a payment is recorded THEN the notice waits`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.RegisterPayment("group-1", "member-2", "member-1"))

            // When
            val settlementId = record(
                from = "member-2",
                to = "member-1",
            )

            // Then
            assertNull(viewModel.uiState.value.recordedPayment)
            assertEquals(settlementId, recordedPayments.observeRecorded().first()?.id)
        }

    @Test
    fun `GIVEN Room not read yet WHEN a payment is recorded THEN the notice waits for the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isRoomReadAtStart = false)

            // When
            val settlementId = record(
                from = "member-2",
                to = "member-1",
            )

            // Then
            assertNull(viewModel.uiState.value.recordedPayment)
            assertEquals(settlementId, recordedPayments.observeRecorded().first()?.id)
        }

    @Test
    fun `GIVEN a payment recorded before Room was read WHEN the group loads THEN shows the notice with names`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isRoomReadAtStart = false)
            val settlementId = record(
                from = "member-2",
                to = "member-1",
            )

            // When
            isRoomRead.value = true

            // Then
            assertEquals(
                PaymentNotice(
                    settlementId = settlementId,
                    fromName = "Ana",
                    fromIsMe = false,
                    toName = "Pierre",
                ),
                viewModel.uiState.value.recordedPayment,
            )
            assertNull(recordedPayments.observeRecorded().first())
        }

    @Test
    fun `GIVEN the screen on top WHEN another group records a payment THEN shows no notice and leaves it waiting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val settlementId = record(
                from = "member-2",
                to = "member-1",
                groupId = "group-2",
            )

            // Then
            assertNull(viewModel.uiState.value.recordedPayment)
            assertEquals(settlementId, recordedPayments.observeRecorded().first()?.id)
        }

    @Test
    fun `GIVEN a debt of Ana WHEN a payment is recorded with the screen on top THEN shows the notice and settles it`() =
        runTest(testDispatcher) {
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

            // When
            val settlementId = record(
                from = "member-2",
                to = "member-1",
            )

            // Then
            val state = viewModel.uiState.value
            assertEquals(settlementId, state.recordedPayment?.settlementId)
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 0,
                ),
                state.myBalance,
            )
            assertTrue(state.isEveryoneSettled)
        }

    @Test
    fun `GIVEN the payment notice on screen WHEN it was shown THEN it hides without deleting the payment`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val settlementId = record(
                from = "member-2",
                to = "member-1",
            )

            // When
            viewModel.onEvent(MembersUiEvent.OnRecordedPaymentNoticeShown)

            // Then
            assertNull(viewModel.uiState.value.recordedPayment)
            assertTrue(settlements.deletedIds.isEmpty())
            assertEquals(listOf(settlementId), settlements.settlements.value.map { it.id })
        }

    @Test
    fun `GIVEN the device fails to delete WHEN tapping Desfazer THEN says so and the payment still counts`() =
        runTest(testDispatcher) {
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
            record(
                from = "member-2",
                to = "member-1",
            )
            settlements.deleteError = IllegalStateException("disk full")

            // When
            viewModel.onEvent(MembersUiEvent.OnUndoPaymentClicked)

            // Then
            assertEquals(listOf<MembersUiAction>(MembersUiAction.ShowChangeError), actions)
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 0,
                ),
                viewModel.uiState.value.myBalance,
            )
        }

    @Test
    fun `GIVEN the payment notice on screen WHEN tapping Desfazer THEN deletes that payment and the debt comes back`() =
        runTest(testDispatcher) {
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
            val settlementId = record(
                from = "member-2",
                to = "member-1",
            )

            // When
            viewModel.onEvent(MembersUiEvent.OnUndoPaymentClicked)

            // Then
            assertEquals(listOf(settlementId), settlements.deletedIds)
            assertEquals(listOf(1_000L), settlements.deletedAts)
            assertNull(viewModel.uiState.value.recordedPayment)
            assertEquals(
                MemberBalance(
                    receivesMinor = 5_000,
                    owesMinor = 0,
                ),
                viewModel.uiState.value.myBalance,
            )
        }

    @Test
    fun `GIVEN no payment notice WHEN tapping Desfazer THEN deletes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedSettlements = listOf(
                settlement(
                    "pay-1",
                    from = "member-2",
                    to = "member-1",
                    amountMinor = 5_000,
                ),
            ),
        )
        val event = MembersUiEvent.OnUndoPaymentClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(settlements.deletedIds.isEmpty())
        assertEquals(listOf("pay-1"), settlements.settlements.value.map { it.id })
    }

    @Test
    fun `GIVEN the notice already shown WHEN tapping Desfazer THEN deletes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val settlementId = record(
            from = "member-2",
            to = "member-1",
        )
        viewModel.onEvent(MembersUiEvent.OnRecordedPaymentNoticeShown)

        // When
        viewModel.onEvent(MembersUiEvent.OnUndoPaymentClicked)

        // Then
        assertTrue(settlements.deletedIds.isEmpty())
        assertEquals(listOf(settlementId), settlements.settlements.value.map { it.id })
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
            assertEquals(4, state.memberCount)
            assertEquals(1, state.joinedCount)
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

    private val withGoogle = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)
    private val withApple = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.APPLE)

    private val pierre = GroupMember(
        "member-1",
        "Pierre",
        AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
        inviteCode = null,
    )
    private val ana = GroupMember(
        "member-2",
        "Ana",
        AvatarColor.PEACH,
        isMe = false,
        hasJoined = false,
        inviteCode = null,
    )
    private val bruno = GroupMember(
        "member-3",
        "Bruno",
        AvatarColor.GREEN,
        isMe = false,
        hasJoined = true,
        inviteCode = null,
    )
    private val carla = GroupMember(
        "member-4",
        "Carla",
        AvatarColor.LILAC,
        isMe = false,
        hasJoined = false,
        inviteCode = null,
    )
    private val diego = GroupMember(
        "member-5",
        "Diego",
        AvatarColor.SAND,
        isMe = false,
        hasJoined = false,
        inviteCode = null,
    )
    private val anaP = GroupMember(
        "member-6",
        "Ana P.",
        AvatarColor.SAND,
        isMe = false,
        hasJoined = false,
        inviteCode = null,
    )

    private val carlaOnScreen = MembersMember(
        "member-4",
        "Carla",
        "CA",
        AvatarColor.LILAC,
        isMe = false,
        hasJoined = false,
        personalLink = null,
        balance = null,
        canBeReleased = false,
    )

    private val brunoOnScreen = MembersMember(
        "member-3",
        "Bruno",
        "BR",
        AvatarColor.GREEN,
        isMe = false,
        hasJoined = true,
        personalLink = null,
        balance = null,
        canBeReleased = true,
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
        isCreatedByMe = true,
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
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(paidBy, amountMinor)),
        shares = splitWith.map { ExpenseShare(it, amountMinor / splitWith.size, weight = 1) },
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
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = 1_000L,
        isPending = false,
    )

    // Why: what Registrar pagamento does on Salvar, so the notice reaches this screen the way it does in the app.
    private suspend fun record(
        from: String,
        to: String,
        amountMinor: Long = 5_000,
        groupId: String = "group-1",
    ): String = recordSettlement(
        groupId = groupId,
        fromMemberId = from,
        toMemberId = to,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = 1_000L,
    ).getOrThrow()

    private fun TestScope.prepareScenario(
        storedGroup: Group = trip,
        isRoomReadAtStart: Boolean = true,
        memberChangeError: Throwable? = null,
        joinedAs: String? = null,
        accountStatus: AccountStatus = AccountStatus.LocalOnly,
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
        releaseError: Throwable? = null,
        waitsForServer: Boolean = false,
    ) {
        account = FakeAccountRepository(initialStatus = accountStatus)
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        isRoomRead.value = isRoomReadAtStart
        recordSettlement = RecordSettlementUseCase(settlements, recordedPayments) { 1_000L }
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup)).also {
            it.memberChangeError = memberChangeError
            it.releaseError = releaseError
            if (waitsForServer) it.releaseGate = serverAnswer
        }
        navigator =
            BackStackNavigator(
                startRoute = Route.Groups,
            ).also { it.navigate(Route.Members("group-1", joinedAs)) }
        val observeGroup = ObserveGroupUseCase(groups)
        viewModel = MembersViewModel(
            route = Route.Members("group-1", joinedAs),
            navigator = navigator,
            checkMemberName = CheckMemberNameUseCase(),
            addMember = AddMemberUseCase(groups, observeGroup) { 1_000L },
            renameMember = RenameMemberUseCase(groups) { 1_000L },
            removeMember = RemoveMemberUseCase(groups) { 1_000L },
            releaseMember = ReleaseMemberUseCase(groups),
            consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
            deleteSettlement = DeleteSettlementUseCase(settlements, recordedPayments) { 1_000L },
            observeGroup = ObserveGroup { groupId ->
                isRoomRead.flatMapLatest { isRead -> if (isRead) observeGroup(groupId) else emptyFlow() }
            },
            observeGroupBalances = ObserveGroupBalancesUseCase(expenses, settlements),
            observeRecordedPayment = ObserveRecordedPaymentUseCase(recordedPayments),
            observeAccount = ObserveAccountUseCase(account),
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
