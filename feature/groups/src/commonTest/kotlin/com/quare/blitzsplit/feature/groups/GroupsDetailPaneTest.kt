package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeGroupDeletionUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupDeletionsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveGroupsLayout
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateGroupsLayout
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: a list and detail (a tablet lying down, an unfolded book) draws the group beside the list, still on one back stack.
class GroupsDetailPaneTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: GroupsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a group beside the list WHEN tapping another group THEN it takes the place of the first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("churrasco"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("ape"))

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail("ape", joinedAs = null)), navigator.backStack.value)
            assertEquals("ape", viewModel.uiState.value.selectedGroupId)
        }

    @Test
    fun `GIVEN a phone with a group open WHEN a late tap reaches another row THEN the first group stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("churrasco"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("ape"))

            // Then
            assertEquals(
                listOf(Route.Groups, Route.GroupDetail("churrasco", joinedAs = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the Membros of a group beside the list WHEN reading the state THEN that group is the one selected`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            navigator.navigate(Route.Members("ape"))

            // Then
            assertEquals("ape", viewModel.uiState.value.selectedGroupId)
        }

    @Test
    fun `GIVEN the group closed WHEN back on the groups THEN nothing is selected`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
        viewModel.onEvent(GroupsUiEvent.OnGroupClicked("ape"))

        // When
        navigator.navigateBack()

        // Then
        assertNull(viewModel.uiState.value.selectedGroupId)
    }

    @Test
    fun `GIVEN a bill over the group beside the list WHEN tapping another group THEN nothing changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("churrasco"))
            navigator.navigate(Route.BillDetail("churrasco", "carne"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("ape"))

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail("churrasco", joinedAs = null),
                    Route.BillDetail("churrasco", "carne"),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a group beside the list WHEN tapping Rachar conta THEN asks which group with that one marked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("ape"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(
                Route.SplitBill(
                    groupId = "ape",
                    expenseId = null,
                    picksGroupFirst = true,
                ),
                navigator.backStack.value.last(),
            )
        }

    @Test
    fun `GIVEN a group beside the list WHEN tapping Convidar on another row THEN opens over the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("ape"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnInviteClicked("churrasco"))

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail("ape", joinedAs = null),
                    Route.Invite("churrasco", isNewGroup = false),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the group beside the list WHEN leaving it from its row THEN the pane closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("aniversario"))
            viewModel.onEvent(GroupsUiEvent.OnLeaveGroupClicked("aniversario"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnGroupExitConfirmed)

            // Then
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN another group beside the list WHEN leaving a group from its row THEN the pane stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("ape"))
            viewModel.onEvent(GroupsUiEvent.OnLeaveGroupClicked("aniversario"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnGroupExitConfirmed)

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail("ape", joinedAs = null)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the pane beside the search WHEN typing THEN the first result opens beside the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("chur"))

            // Then
            assertEquals(
                listOf(Route.Groups, Route.GroupDetail("churrasco", joinedAs = null)),
                navigator.backStack.value,
            )
            assertTrue(viewModel.uiState.value.search.isOpen)
        }

    @Test
    fun `GIVEN the pane beside the search WHEN only a person matches THEN opens the Membros they were found in`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("carla"))

            // Then
            assertEquals(listOf(Route.Groups, Route.Members("aniversario")), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the pane beside the search WHEN only a bill matches THEN opens its group without the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("mercado"))

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail("ape", joinedAs = null)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN another result tapped WHEN typing on with the same first result THEN the tapped one stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("chur"))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("aniversario"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("churr"))

            // Then
            assertEquals(
                listOf(Route.Groups, Route.GroupDetail("aniversario", joinedAs = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN another result tapped WHEN the device turns upright and back THEN the tapped one stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("chur"))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("aniversario"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = false))
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))

            // Then
            assertEquals(
                listOf(Route.Groups, Route.GroupDetail("aniversario", joinedAs = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN another group stacked beside the list WHEN tapping a row THEN it replaces both`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("churrasco"))
            navigator.navigate(Route.GroupDetail("ape", joinedAs = null))

            // When
            val selectedBefore = viewModel.uiState.value.selectedGroupId
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("aniversario"))

            // Then
            assertEquals("ape", selectedBefore)
            assertEquals(
                listOf(Route.Groups, Route.GroupDetail("aniversario", joinedAs = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a phone WHEN typing in the search THEN nothing opens by itself`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("chur"))

        // Then
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a search with results WHEN the window starts drawing the pane THEN opens the first result there`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("chur"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))

            // Then
            assertEquals(
                listOf(Route.Groups, Route.GroupDetail("churrasco", joinedAs = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a group beside the list WHEN the window stops drawing the pane THEN the group stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("ape"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = false))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("churrasco"))

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail("ape", joinedAs = null)), navigator.backStack.value)
        }

    private val churrasco = group(
        id = "churrasco",
        name = "Churrasco de sábado",
        names = listOf("Pierre", "Ana", "Bruno"),
    )

    // Why: settled, since Ana paid her own bill.
    private val aniversario = group(
        id = "aniversario",
        name = "Aniversário da Ana",
        names = listOf("Pierre", "Ana", "Carla"),
    )

    private val ape = group(
        id = "ape",
        name = "Apê 302",
        names = listOf("Pierre", "Carla"),
    )

    private val storedBills = listOf(
        bill(
            id = "carne",
            groupId = "churrasco",
            description = "Carne e carvão do churrasco",
            paidBy = "Bruno",
            splitOver = listOf("Pierre", "Ana", "Bruno"),
            updatedAt = 3_000L,
        ),
        bill(
            id = "churros",
            groupId = "aniversario",
            description = "Churros",
            paidBy = "Ana",
            splitOver = listOf("Ana"),
            updatedAt = 2_000L,
        ),
        bill(
            id = "mercado",
            groupId = "ape",
            description = "Mercado",
            paidBy = "Carla",
            splitOver = listOf("Pierre", "Carla"),
            updatedAt = 1_000L,
        ),
    )

    // Why: Pierre, the first name, is this device's user.
    private fun group(
        id: String,
        name: String,
        names: List<String>,
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.OTHER,
        color = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        members = names.mapIndexed { index, memberName ->
            GroupMember(
                id = "$id-$memberName",
                name = memberName,
                avatarColor = AvatarColor.PEACH,
                isMe = index == 0,
                hasJoined = false,
                userId = null,
                inviteCode = null,
            )
        },
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    private fun bill(
        id: String,
        groupId: String,
        description: String,
        paidBy: String,
        splitOver: List<String>,
        updatedAt: Long,
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = description,
        amountMinor = 600L,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer("$groupId-$paidBy", 600L)),
        shares = splitOver.map { ExpenseShare("$groupId-$it", 600L / splitOver.size, 1L) },
        spentAt = updatedAt,
        isPending = false,
        updatedAt = updatedAt,
        updatedByMemberId = null,
    )

    private fun prepareScenario(
        storedGroups: List<Group> = listOf(churrasco, aniversario, ape),
        storedExpenses: List<Expense> = storedBills,
    ) {
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        groups = FakeGroupRepository(initialGroups = storedGroups)
        navigator = BackStackNavigator(startRoute = Route.Groups)
        val deletions = InMemoryGroupDeletionRepository()
        viewModel = GroupsViewModel(
            observeProfile = ObserveProfileUseCase(
                FakeProfileRepository(UserProfile("Pierre", AvatarColor.GREEN, null)),
            ),
            observeAccount = ObserveAccountUseCase(FakeAccountRepository()),
            observeSyncState = ObserveSyncState { MutableStateFlow(SyncState.SYNCED) },
            observeGroups = ObserveGroupsUseCase(groups),
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
            retrySync = RetrySync {},
            syncNow = SyncNow { Result.success(Unit) },
            observeGroupsMoney = ObserveGroupsMoneyUseCase(expenses, FakeSettlementRepository()),
            currentTimeMillis = CurrentTimeMillis { 10_000L },
            currentTimeZone = CurrentTimeZone { TimeZone.UTC },
            minuteTicks = MinuteTicks { flowOf(Unit) },
            navigator = navigator,
            observeGroupDeletions = ObserveGroupDeletionsUseCase(deletions),
            consumeGroupDeletion = ConsumeGroupDeletionUseCase(deletions),
            leaveGroup = LeaveGroup { Result.success(GroupExitOutcome.DONE) },
            deleteGroup = DeleteGroup { Result.success(GroupExitOutcome.DONE) },
            observeRecordedPayment = ObserveRecordedPayment { flowOf(null) },
            consumeRecordedPayment = ConsumeRecordedPayment {},
            deleteSettlement = DeleteSettlement { Result.success(Unit) },
            observePeopleBalances = { emptyFlow() },
            observeGroupsLayout = ObserveGroupsLayout { flowOf(GroupsLayout.initial) },
            updateGroupsLayout = UpdateGroupsLayout {},
        )
    }
}
