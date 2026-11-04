package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.groups.presentation.model.SearchScope
import com.quare.blitzsplit.feature.groups.presentation.model.SearchUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.TextMatch
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: "Buscar" (5.2) over Início, Saldos, Contatos and Atividade (Route.Search): what it finds opens in its place.
class SearchViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: SearchViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the search over a tab WHEN it opens THEN shows it open on Tudo with nothing typed and no result`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val search = viewModel.uiState.value.search

            // Then
            assertTrue(search.isOpen)
            assertEquals("", search.query)
            assertEquals(SearchScope.ALL, search.scope)
            assertTrue(search.results.isEmpty)
            assertFalse(search.showsResults)
            assertFalse(search.showsNoResults)
        }

    @Test
    fun `GIVEN the search WHEN typing part of a name THEN finds the groups and the bills of the open groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("chur"))

            // Then
            val search = viewModel.uiState.value.search
            assertEquals("chur", search.query)
            assertEquals(listOf("churrasco", "aniversario"), search.results.groups.map { it.group.id })
            assertEquals(TextMatch(0, 4), search.results.groups[0].nameMatch)
            assertEquals(listOf("carne"), search.results.bills.map { it.id })
            assertTrue(search.results.people.isEmpty())
            assertTrue(search.showsResults)
        }

    @Test
    fun `GIVEN the search WHEN typing with upper case and an accent THEN finds what has neither`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("  CHÚR "))

            // Then
            assertEquals(
                listOf("churrasco", "aniversario"),
                viewModel.uiState.value.search.results.groups
                    .map { it.group.id },
            )
        }

    @Test
    fun `GIVEN a name in two groups WHEN searching it THEN finds the person once per group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("carla"))

            // Then
            val people = viewModel.uiState.value.search.results.people
            assertEquals(listOf("aniversario", "ape"), people.map { it.groupId })
            assertEquals(listOf("Carla", "Carla"), people.map { it.name })
        }

    @Test
    fun `GIVEN the name of this device user WHEN searching it THEN finds nobody`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SearchUiEvent.OnQueryChanged("pierre"))

        // Then
        assertTrue(
            viewModel.uiState.value.search.results.people
                .isEmpty(),
        )
        assertTrue(viewModel.uiState.value.search.showsNoResults)
    }

    @Test
    fun `GIVEN a settled group with a matching bill WHEN searching everything THEN the bill comes as its group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("churros"))

            // Then
            val results = viewModel.uiState.value.search.results
            val hit = results.groups.single()
            assertEquals("aniversario", hit.group.id)
            assertNull(hit.nameMatch)
            assertEquals("Churros", hit.billDescription)
            assertTrue(results.bills.isEmpty())
        }

    @Test
    fun `GIVEN a search WHEN picking Grupos THEN only the groups show`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SearchUiEvent.OnQueryChanged("chur"))

        // When
        viewModel.onEvent(SearchUiEvent.OnScopeChanged(SearchScope.GROUPS))

        // Then
        val search = viewModel.uiState.value.search
        assertEquals(SearchScope.GROUPS, search.scope)
        assertEquals("chur", search.query)
        assertEquals(listOf("churrasco", "aniversario"), search.results.groups.map { it.group.id })
        assertTrue(search.results.bills.isEmpty())
        assertTrue(search.results.people.isEmpty())
    }

    @Test
    fun `GIVEN a search WHEN picking Pessoas THEN only the people show`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SearchUiEvent.OnQueryChanged("an"))

        // When
        viewModel.onEvent(SearchUiEvent.OnScopeChanged(SearchScope.PEOPLE))

        // Then
        val results = viewModel.uiState.value.search.results
        assertEquals(listOf("Ana", "Ana"), results.people.map { it.name })
        assertTrue(results.groups.isEmpty())
        assertTrue(results.bills.isEmpty())
    }

    @Test
    fun `GIVEN a search WHEN picking Despesas THEN lists the settled groups bills too the most recent first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("chur"))

            // When
            viewModel.onEvent(SearchUiEvent.OnScopeChanged(SearchScope.BILLS))

            // Then
            val results = viewModel.uiState.value.search.results
            assertEquals(listOf("carne", "churros"), results.bills.map { it.id })
            assertTrue(results.groups.isEmpty())
        }

    @Test
    fun `GIVEN a scope picked before typing WHEN typing THEN the results keep the scope`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SearchUiEvent.OnScopeChanged(SearchScope.BILLS))

        // When
        viewModel.onEvent(SearchUiEvent.OnQueryChanged("chur"))

        // Then
        val search = viewModel.uiState.value.search
        assertEquals(SearchScope.BILLS, search.scope)
        assertEquals(listOf("carne", "churros"), search.results.bills.map { it.id })
        assertTrue(search.results.groups.isEmpty())
    }

    @Test
    fun `GIVEN a search on Despesas WHEN a sync brings a matching bill THEN it shows keeping the text and scope`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("chur"))
            viewModel.onEvent(SearchUiEvent.OnScopeChanged(SearchScope.BILLS))

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
            val search = viewModel.uiState.value.search
            assertTrue(search.isOpen)
            assertEquals("chur", search.query)
            assertEquals(SearchScope.BILLS, search.scope)
            assertEquals(listOf("grill", "carne", "churros"), search.results.bills.map { it.id })
        }

    @Test
    fun `GIVEN a search WHEN a sync brings a matching group THEN it shows`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SearchUiEvent.OnQueryChanged("praia"))

        // When
        groups.groups.value += group(
            id = "praia",
            name = "Praia do Rosa",
            names = listOf("Pierre", "Bruno"),
        )

        // Then
        assertEquals(
            listOf("praia"),
            viewModel.uiState.value.search.results.groups
                .map { it.group.id },
        )
    }

    @Test
    fun `GIVEN a group found WHEN a sync deletes it THEN it leaves the results`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SearchUiEvent.OnQueryChanged("apê"))

        // When
        groups.groups.value = listOf(churrasco, aniversario)

        // Then
        assertTrue(viewModel.uiState.value.search.showsNoResults)
    }

    @Test
    fun `GIVEN the search over Contatos WHEN a sync deletes every group THEN goes back to Contatos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Contacts)
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("apê"))

            // When
            groups.groups.value = emptyList()

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search over Contatos WHEN tapping a group THEN its Despesas takes the search place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Contacts)

            // When
            viewModel.onEvent(SearchUiEvent.OnGroupClicked("churrasco"))

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, expensesOf("churrasco")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the search over Inicio WHEN tapping a group THEN its Despesas opens right above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Home)

            // When
            viewModel.onEvent(SearchUiEvent.OnGroupClicked("ape"))

            // Then
            assertEquals(listOf(Route.Home, expensesOf("ape")), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search over Saldos WHEN tapping a person THEN the Membros of their group takes the search place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = balances)

            // When
            viewModel.onEvent(SearchUiEvent.OnPersonClicked("ape"))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    balances,
                    Route.GroupDetail(
                        "ape",
                        joinedAs = null,
                        tab = GroupTab.MEMBERS,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the search over Atividade WHEN tapping a bill THEN opens its Conta over its group instead`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = activity)

            // When
            viewModel.onEvent(SearchUiEvent.OnBillClicked("churrasco", "carne"))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    activity,
                    expensesOf("churrasco"),
                    Route.BillDetail("churrasco", "carne", focusCommentId = null),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the search over Contatos WHEN closing it THEN goes back to Contatos`() = runTest(testDispatcher) {
        // Given
        prepareScenario(tab = Route.Contacts)

        // When
        viewModel.onEvent(SearchUiEvent.OnClosed)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group already opened from the search WHEN a second tap on a result arrives THEN nothing else opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Contacts)
            viewModel.onEvent(SearchUiEvent.OnGroupClicked("churrasco"))

            // When
            viewModel.onEvent(SearchUiEvent.OnPersonClicked("ape"))

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, expensesOf("churrasco")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a bill already opened from the search WHEN a second tap on a bill arrives THEN nothing else opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = activity)
            viewModel.onEvent(SearchUiEvent.OnBillClicked("churrasco", "carne"))

            // When
            viewModel.onEvent(SearchUiEvent.OnBillClicked("ape", "mercado"))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    activity,
                    expensesOf("churrasco"),
                    Route.BillDetail("churrasco", "carne", focusCommentId = null),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a group already opened from the search WHEN a late close arrives THEN the group stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Contacts)
            viewModel.onEvent(SearchUiEvent.OnGroupClicked("churrasco"))

            // When
            viewModel.onEvent(SearchUiEvent.OnClosed)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, expensesOf("churrasco")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the search already closed WHEN a late tap on a result arrives THEN the tab stays as it is`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Contacts)
            viewModel.onEvent(SearchUiEvent.OnClosed)

            // When
            viewModel.onEvent(SearchUiEvent.OnGroupClicked("churrasco"))

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search already closed WHEN a second close arrives THEN the tab stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Contacts)
            viewModel.onEvent(SearchUiEvent.OnClosed)

            // When
            viewModel.onEvent(SearchUiEvent.OnClosed)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    private val balances = Route.GlobalBalances(BalancesFilter.ALL, charge = null)

    private val activity = Route.Activity(personKey = null)

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

    private fun expensesOf(groupId: String): Route = Route.GroupDetail(
        groupId,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
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
        shape = GroupShape.SQUARE,
        photo = null,
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
                photoUrl = null,
            )
        },
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
        categoryId = null,
        spentAt = updatedAt,
        isPending = false,
        updatedAt = updatedAt,
        updatedByMemberId = null,
    )

    // Why: the search opens over the tab it was asked from, right above Início unless it is Início itself.
    private fun prepareScenario(tab: Route = Route.Contacts) {
        expenses = FakeExpenseRepository(initialExpenses = storedBills)
        groups = FakeGroupRepository(initialGroups = listOf(churrasco, aniversario, ape))
        navigator = BackStackNavigator(startRoute = Route.Home).also {
            if (tab != Route.Home) it.navigate(tab)
            it.navigate(Route.Search)
        }
        viewModel = SearchViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            navigator = navigator,
            currentTimeMillis = CurrentTimeMillis { 10_000L },
            currentTimeZone = CurrentTimeZone { TimeZone.UTC },
            observeGroups = ObserveGroupsUseCase(groups),
            observeGroupsMoney = ObserveGroupsMoneyUseCase(expenses, FakeSettlementRepository()),
        )
    }
}
