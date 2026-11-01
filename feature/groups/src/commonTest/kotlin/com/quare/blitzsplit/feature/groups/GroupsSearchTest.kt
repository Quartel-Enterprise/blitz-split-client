package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
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
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillPayer
import com.quare.blitzsplit.feature.groups.presentation.model.SearchScope
import com.quare.blitzsplit.feature.groups.presentation.model.TextMatch
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: "Buscar" (5.2): the groups, people and bills on this device, the settled groups included, offline too.
class GroupsSearchTest {
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
    fun `GIVEN no group yet WHEN opening the search THEN it stays closed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            storedExpenses = emptyList(),
        )

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

        // Then
        assertFalse(viewModel.uiState.value.canSearch)
        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun `GIVEN a screen over the groups WHEN opening the search THEN it stays closed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(
            Route.JoinWithLink(
                skippedCode = null,
                skippedMemberCode = null,
            ),
        )

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

        // Then
        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun `GIVEN the search open WHEN the last group goes and a new one comes THEN the search stays closed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("chur"))
            groups.groups.value = emptyList()

            // When
            groups.groups.value = listOf(ape)

            // Then
            assertFalse(viewModel.uiState.value.isSearching)
            assertEquals("", viewModel.uiState.value.search.query)
        }

    @Test
    fun `GIVEN the search closed WHEN a late keystroke arrives THEN the search stays closed and empty`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("chur"))

            // Then
            assertFalse(viewModel.uiState.value.isSearching)
            assertEquals("", viewModel.uiState.value.search.query)
            assertTrue(viewModel.uiState.value.search.results.isEmpty)
        }

    @Test
    fun `GIVEN the groups WHEN opening the search THEN shows it with nothing typed and no result`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // Then
            val search = viewModel.uiState.value.search
            assertTrue(viewModel.uiState.value.isSearching)
            assertFalse(search.hasQuery)
            assertFalse(search.showsResults)
            assertFalse(search.showsNoResults)
        }

    @Test
    fun `GIVEN the search open WHEN typing part of a name THEN finds the group and the bills of the open groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("chur"))

            // Then
            val results = viewModel.uiState.value.search.results
            assertEquals(listOf("churrasco", "aniversario"), results.groups.map { it.group.id })
            assertEquals(TextMatch(0, 4), results.groups[0].nameMatch)
            assertEquals(listOf("carne"), results.bills.map { it.id })
            assertEquals(TextMatch(18, 22), results.bills[0].descriptionMatch)
            assertEquals(listOf(SearchBillPayer("Bruno", isMe = false)), results.bills[0].payers)
            assertTrue(results.people.isEmpty())
            assertTrue(viewModel.uiState.value.search.showsResults)
        }

    @Test
    fun `GIVEN a settled group with a matching bill WHEN searching everything THEN the bill comes as its group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("churros"))

            // Then
            val hit = viewModel.uiState.value.search.results.groups
                .single()
            assertEquals("aniversario", hit.group.id)
            assertNull(hit.nameMatch)
            assertEquals("Churros", hit.billDescription)
            assertEquals(TextMatch(0, 7), hit.billMatch)
            assertTrue(
                viewModel.uiState.value.search.results.bills
                    .isEmpty(),
            )
        }

    @Test
    fun `GIVEN upper case and an accent typed WHEN searching THEN still finds what has neither`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("  CHÚR "))

            // Then
            assertEquals(
                listOf("churrasco", "aniversario"),
                viewModel.uiState.value.search.results.groups
                    .map { it.group.id },
            )
        }

    @Test
    fun `GIVEN a name in two groups WHEN searching it THEN finds the person once per group in the home order`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("carla"))

            // Then
            val people = viewModel.uiState.value.search.results.people
            assertEquals(listOf("aniversario", "ape"), people.map { it.groupId })
            assertEquals(listOf("Carla", "Carla"), people.map { it.name })
            assertEquals("C", people[0].initial)
        }

    @Test
    fun `GIVEN the name of this device user WHEN searching it THEN finds nobody`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("pierre"))

        // Then
        assertTrue(
            viewModel.uiState.value.search.results.people
                .isEmpty(),
        )
        assertTrue(viewModel.uiState.value.search.showsNoResults)
    }

    @Test
    fun `GIVEN nothing with the text WHEN searching THEN says nothing was found`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged(" pizza "))

        // Then
        assertTrue(viewModel.uiState.value.search.showsNoResults)
        assertEquals("pizza", viewModel.uiState.value.search.trimmedQuery)
    }

    @Test
    fun `GIVEN a search WHEN picking Grupos THEN only the groups show`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
        viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("chur"))

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearchScopeChanged(SearchScope.GROUPS))

        // Then
        val results = viewModel.uiState.value.search.results
        assertEquals(2, results.groups.size)
        assertTrue(results.bills.isEmpty())
        assertEquals(SearchScope.GROUPS, viewModel.uiState.value.search.scope)
    }

    @Test
    fun `GIVEN a search WHEN picking Despesas THEN lists the settled groups bills too the most recent first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("chur"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchScopeChanged(SearchScope.BILLS))

            // Then
            val results = viewModel.uiState.value.search.results
            assertEquals(listOf("carne", "churros"), results.bills.map { it.id })
            assertTrue(results.groups.isEmpty())
        }

    @Test
    fun `GIVEN a search WHEN picking Pessoas THEN only the people show`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
        viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("an"))

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearchScopeChanged(SearchScope.PEOPLE))

        // Then
        val results = viewModel.uiState.value.search.results
        assertEquals(listOf("Ana", "Ana"), results.people.map { it.name })
        assertTrue(results.groups.isEmpty())
        assertTrue(results.bills.isEmpty())
    }

    @Test
    fun `GIVEN a search WHEN a sync brings a matching bill THEN it shows`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
        viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("chur"))

        // When
        expenses.expenses.value += bill(
            id = "grill",
            groupId = "ape",
            description = "Churrasqueira",
            paidBy = "Carla",
            splitOver = listOf("Pierre", "Carla"),
            updatedAt = 4_000L,
        )

        // Then
        assertEquals(
            listOf("grill", "carne"),
            viewModel.uiState.value.search.results.bills
                .map { it.id },
        )
    }

    @Test
    fun `GIVEN a person found WHEN tapping them THEN opens the members of their group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearchPersonClicked("ape"))

        // Then
        assertEquals(listOf(Route.Groups, Route.Members("ape", joinedAs = null)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a bill found WHEN tapping it THEN opens the bill over its group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearchBillClicked("churrasco", "carne"))

        // Then
        assertEquals(
            listOf(Route.Groups, Route.GroupDetail("churrasco"), Route.BillDetail("churrasco", "carne")),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a bill already open WHEN tapping another result THEN nothing else opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
        viewModel.onEvent(GroupsUiEvent.OnSearchBillClicked("churrasco", "carne"))

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearchPersonClicked("ape"))

        // Then
        assertEquals(3, navigator.backStack.value.size)
    }

    @Test
    fun `GIVEN a result open from the search WHEN closing it THEN forgets the text and keeps the result open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("chur"))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("churrasco"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchClosed)

            // Then
            val search = viewModel.uiState.value.search
            assertFalse(search.isOpen)
            assertEquals("", search.query)
            assertEquals(2, navigator.backStack.value.size)
        }

    @Test
    fun `GIVEN a result open from the search tab WHEN leaving the tab THEN forgets the text and goes back home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("chur"))
            viewModel.onEvent(GroupsUiEvent.OnSearchScopeChanged(SearchScope.BILLS))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("churrasco"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchTabLeft)

            // Then
            val search = viewModel.uiState.value.search
            assertFalse(search.isOpen)
            assertEquals("", search.query)
            assertEquals(SearchScope.ALL, search.scope)
            assertTrue(search.results.isEmpty)
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
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
        items = null,
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
            observeActivityUnreadCount = ObserveActivityUnreadCount { flowOf(0) },
        )
    }
}
