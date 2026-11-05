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
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openActivity
import com.quare.blitzsplit.core.navigation.openSearchFromPage
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveGroupsLayout
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateGroupsLayout
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillPayer
import com.quare.blitzsplit.feature.groups.presentation.model.SearchScope
import com.quare.blitzsplit.feature.groups.presentation.model.SearchUiEvent
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
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
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
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))
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
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))

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
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))

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
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("churros")))

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
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("  CHÚR ")))

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
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("carla")))

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
        viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("pierre")))

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
        viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged(" pizza ")))

        // Then
        assertTrue(viewModel.uiState.value.search.showsNoResults)
        assertEquals("pizza", viewModel.uiState.value.search.trimmedQuery)
    }

    @Test
    fun `GIVEN a search WHEN picking Grupos THEN only the groups show`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
        viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnScopeChanged(SearchScope.GROUPS)))

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
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnScopeChanged(SearchScope.BILLS)))

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
        viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("an")))

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnScopeChanged(SearchScope.PEOPLE)))

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
        viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))

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
        viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnPersonClicked("ape")))

        // Then
        assertEquals(
            listOf(Route.Groups, Route.GroupDetail("ape", joinedAs = null, GroupTab.MEMBERS)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a bill found WHEN tapping it THEN opens the bill over its group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnBillClicked("churrasco", "carne")))

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    "churrasco",
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
                Route.BillDetail("churrasco", "carne", focusCommentId = null),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a bill already open WHEN tapping another result THEN nothing else opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
        viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnBillClicked("churrasco", "carne")))

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnPersonClicked("ape")))

        // Then
        assertEquals(3, navigator.backStack.value.size)
    }

    @Test
    fun `GIVEN a result open from the search WHEN closing it THEN forgets the text and keeps the result open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("churrasco"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnClosed))

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
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnScopeChanged(SearchScope.BILLS)))
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

    @Test
    fun `GIVEN the search open in the groups WHEN picking Despesas THEN tracks the scope from the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnScopeChanged(SearchScope.BILLS)))

            // Then
            assertEquals(
                listOf(
                    "search_opened" to mapOf<String, Any>("source" to "groups"),
                    "search_scope_selected" to mapOf<String, Any>("scope" to SearchScope.BILLS, "source" to "groups"),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a group found in the groups WHEN tapping it THEN tracks only search_result_clicked of a group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnGroupClicked("churrasco")))

            // Then
            assertEquals(
                listOf(
                    "search_opened" to mapOf<String, Any>("source" to "groups"),
                    "search_result_clicked" to mapOf<String, Any>("kind" to "group", "source" to "groups"),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the search in the groups WHEN tapping a person and a bill found THEN tracks each kind without ids`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("carla")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnPersonClicked("ape")))
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnBillClicked("ape", "mercado")))

            // Then
            assertEquals(
                listOf("person", "bill").map { kind ->
                    "search_result_clicked" to mapOf<String, Any>("kind" to kind, "source" to "groups")
                },
                trackedEvents.drop(1),
            )
        }

    @Test
    fun `GIVEN a group over the list WHEN tapping the search tab THEN opens the search over the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                backStack = listOf(
                    Route.Home,
                    Route.Groups,
                    Route.GroupDetail(
                        churrasco.id,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                    ),
                ),
            )

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchTabClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
            assertTrue(viewModel.uiState.value.isSearching)
            assertEquals(listOf("search_opened" to mapOf<String, Any>("source" to "groups")), trackedEvents)
        }

    @Test
    fun `GIVEN no group yet and a screen over the list WHEN tapping the search tab THEN nothing moves`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = emptyList(),
                backStack = listOf(Route.Home, Route.Groups, Route.Profile),
            )

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchTabClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, Route.Profile), navigator.backStack.value)
            assertFalse(viewModel.uiState.value.isSearching)
        }

    @Test
    fun `GIVEN the search open in the groups WHEN typing and closing it THEN tracks only the opening`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnClosed))

            // Then
            assertEquals(listOf("search_opened" to mapOf<String, Any>("source" to "groups")), trackedEvents)
        }

    @Test
    fun `GIVEN no group yet WHEN opening the search THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            storedExpenses = emptyList(),
        )

        // When
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a screen pushed over the groups WHEN opening the search THEN tracks nothing`() =
        runTest(testDispatcher) {
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
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a group open beside the list WHEN opening the search THEN tracks search_opened from the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("churrasco"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // Then
            assertTrue(viewModel.uiState.value.isSearching)
            assertEquals(listOf("search_opened" to mapOf<String, Any>("source" to "groups")), trackedEvents)
        }

    @Test
    fun `GIVEN the search already open in the groups WHEN opening it again THEN tracks only the first opening`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // Then
            assertEquals(listOf("search_opened" to mapOf<String, Any>("source" to "groups")), trackedEvents)
        }

    @Test
    fun `GIVEN the search open in the groups WHEN tapping the Grupos tab around it THEN closes it over the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(backStack = listOf(Route.Home, Route.Groups))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnHomeTabClicked(HomeTab.GROUPS)))

            // Then
            assertFalse(viewModel.uiState.value.isSearching)
            assertEquals("", viewModel.uiState.value.search.query)
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a group opened from the search in the groups WHEN tapping Grupos THEN goes back to the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(backStack = listOf(Route.Home, Route.Groups))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnGroupClicked("churrasco")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnHomeTabClicked(HomeTab.GROUPS)))

            // Then
            assertFalse(viewModel.uiState.value.isSearching)
            assertEquals("", viewModel.uiState.value.search.query)
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search open in the groups WHEN tapping Inicio around it THEN closes it and goes to Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(backStack = listOf(Route.Home, Route.Groups))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnHomeTabClicked(HomeTab.HOME)))

            // Then
            assertFalse(viewModel.uiState.value.isSearching)
            assertEquals("", viewModel.uiState.value.search.query)
            assertEquals(listOf(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search open in the groups WHEN tapping Saldos around it THEN Saldos takes the groups place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(backStack = listOf(Route.Home, Route.Groups))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnHomeTabClicked(HomeTab.BALANCES)))

            // Then
            assertFalse(viewModel.uiState.value.isSearching)
            assertEquals("", viewModel.uiState.value.search.query)
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the search open in the groups WHEN tapping the Contatos tab around it THEN Contatos takes its place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(backStack = listOf(Route.Home, Route.Groups))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnHomeTabClicked(HomeTab.CONTACTS)))

            // Then
            assertFalse(viewModel.uiState.value.isSearching)
            assertEquals("", viewModel.uiState.value.search.query)
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search open in the groups WHEN tapping a tab around it THEN tracks only the opening`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(backStack = listOf(Route.Home, Route.Groups))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnHomeTabClicked(HomeTab.BALANCES)))

            // Then
            assertEquals(listOf("search_opened" to mapOf<String, Any>("source" to "groups")), trackedEvents)
        }

    @Test
    fun `GIVEN a group opened from a search closed since WHEN a late tap on Grupos arrives THEN keeps the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(backStack = listOf(Route.Home, Route.Groups))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnGroupClicked("churrasco")))
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnClosed))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnHomeTabClicked(HomeTab.GROUPS)))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Groups,
                    Route.GroupDetail(
                        "churrasco",
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the search closed in the groups WHEN a late tap on Inicio around it arrives THEN stays on the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(backStack = listOf(Route.Home, Route.Groups))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnClosed))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnHomeTabClicked(HomeTab.HOME)))

            // Then
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search closed in the groups WHEN a late tap on Saldos around it arrives THEN stays on the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(backStack = listOf(Route.Home, Route.Groups))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnClosed))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnHomeTabClicked(HomeTab.BALANCES)))

            // Then
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the groups over Inicio WHEN the search route is pushed THEN they open their own search`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(backStack = listOf(Route.Home, Route.Groups))

            // When
            navigator.navigate(Route.Search)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
            assertTrue(viewModel.uiState.value.isSearching)
            assertEquals("", viewModel.uiState.value.search.query)
        }

    @Test
    fun `GIVEN the groups over Inicio WHEN the search route is pushed over them THEN tracks nothing from the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(backStack = listOf(Route.Home, Route.Groups))

            // When
            navigator.navigate(Route.Search)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN no group on the device WHEN the search route is pushed over the groups THEN the route stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = emptyList(),
                storedExpenses = emptyList(),
                backStack = listOf(Route.Home, Route.Groups),
            )

            // When
            navigator.navigate(Route.Search)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, Route.Search), navigator.backStack.value)
            assertFalse(viewModel.uiState.value.search.isOpen)
        }

    @Test
    fun `GIVEN the search route over the groups and no group WHEN a sync brings one THEN the groups take it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = emptyList(),
                storedExpenses = emptyList(),
                backStack = listOf(Route.Home, Route.Groups),
            )
            navigator.navigate(Route.Search)

            // When
            groups.groups.value = listOf(ape)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
            assertTrue(viewModel.uiState.value.isSearching)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a group opened from the groups WHEN the search route is pushed over the group THEN it stays`() =
        runTest(testDispatcher) {
            // Given
            val group = Route.GroupDetail(
                "churrasco",
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            )
            prepareScenario(backStack = listOf(Route.Home, Route.Groups, group))

            // When
            navigator.navigate(Route.Search)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, group, Route.Search), navigator.backStack.value)
            assertFalse(viewModel.uiState.value.search.isOpen)
        }

    @Test
    fun `GIVEN Contatos over Inicio WHEN the search route is pushed over it THEN it stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario(backStack = listOf(Route.Home, Route.Contacts))

        // When
        navigator.navigate(Route.Search)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts, Route.Search), navigator.backStack.value)
        assertFalse(viewModel.uiState.value.search.isOpen)
    }

    @Test
    fun `GIVEN a group opened from the groups WHEN searching from that page THEN the groups open their own search`() =
        runTest(testDispatcher) {
            // Given
            val group = Route.GroupDetail(
                "churrasco",
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            )
            prepareScenario(backStack = listOf(Route.Home, Route.Groups, group))

            // When
            navigator.openSearchFromPage()

            // Then
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
            assertTrue(viewModel.uiState.value.isSearching)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the search open in the groups WHEN Atividade takes the groups place THEN the search closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(backStack = listOf(Route.Home, Route.Groups))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))

            // When
            navigator.openActivity()

            // Then
            assertEquals(listOf(Route.Home, Route.Activity(personKey = null)), navigator.backStack.value)
            assertFalse(viewModel.uiState.value.search.isOpen)
            assertEquals("", viewModel.uiState.value.search.query)
        }

    @Test
    fun `GIVEN the search closed by Atividade WHEN the groups come back THEN the search stays closed and empty`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(backStack = listOf(Route.Home, Route.Groups))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))
            navigator.replaceAbove(1, Route.Activity(personKey = null))

            // When
            navigator.replaceAbove(1, Route.Groups)

            // Then
            assertFalse(viewModel.uiState.value.isSearching)
            assertEquals("", viewModel.uiState.value.search.query)
            assertTrue(viewModel.uiState.value.search.results.isEmpty)
        }

    @Test
    fun `GIVEN the search open in the groups WHEN a result opens a group over them THEN the search stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(backStack = listOf(Route.Home, Route.Groups))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("chur")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnGroupClicked("churrasco")))

            // Then
            assertEquals(3, navigator.backStack.value.size)
            assertTrue(viewModel.uiState.value.search.isOpen)
            assertEquals("chur", viewModel.uiState.value.search.query)
        }

    @Test
    fun `GIVEN a name missing a letter WHEN searching in the groups THEN finds the person and suggests it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(churrasco, aniversario, ape, viagem))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("Larisa")))

            // Then
            val search = viewModel.uiState.value.search
            assertEquals(listOf("Larissa"), search.results.people.map { it.name })
            assertEquals(listOf("name:larissa"), search.results.contacts.map { it.personKey })
            assertEquals("Larissa", search.results.suggestion)
            assertTrue(search.showsSuggestion)
        }

    @Test
    fun `GIVEN a suggestion in the groups WHEN tapping it THEN searches it instead without a suggestion`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(churrasco, aniversario, ape, viagem))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("Larisa")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnSuggestionClicked("Larissa")))

            // Then
            val search = viewModel.uiState.value.search
            assertEquals("Larissa", search.query)
            assertEquals(listOf("Larissa"), search.results.people.map { it.name })
            assertNull(search.results.suggestion)
            assertFalse(search.showsSuggestion)
        }

    @Test
    fun `GIVEN a suggestion in the groups WHEN tapping it THEN tracks search_suggestion_clicked from the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(churrasco, aniversario, ape, viagem))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("Larisa")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnSuggestionClicked("Larissa")))

            // Then
            assertEquals(
                listOf(
                    "search_opened" to mapOf<String, Any>("source" to "groups"),
                    "search_suggestion_clicked" to mapOf<String, Any>("source" to "groups"),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the search closed WHEN a late tap on a suggestion arrives THEN the search stays closed and empty`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(churrasco, aniversario, ape, viagem))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("Larisa")))
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnClosed))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnSuggestionClicked("Larissa")))

            // Then
            val search = viewModel.uiState.value.search
            assertFalse(search.isOpen)
            assertEquals("", search.query)
            assertTrue(search.results.isEmpty)
        }

    @Test
    fun `GIVEN a guest in two groups WHEN searching their name in the groups THEN finds one contact and both rows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("carla")))

            // Then
            val results = viewModel.uiState.value.search.results
            val contact = results.contacts.single()
            assertEquals("name:carla", contact.personKey)
            assertEquals("Carla", contact.name)
            assertEquals(2, contact.groupCount)
            assertEquals(listOf("aniversario", "ape"), results.people.map { it.groupId })
        }

    @Test
    fun `GIVEN a contact found in the groups WHEN tapping it THEN their profile opens over the search`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("carla")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnContactClicked("name:carla")))

            // Then
            assertEquals(listOf(Route.Groups, Route.ContactProfile("name:carla")), navigator.backStack.value)
            assertTrue(viewModel.uiState.value.search.isOpen)
            assertEquals("carla", viewModel.uiState.value.search.query)
        }

    @Test
    fun `GIVEN the pane beside the search WHEN tapping a contact THEN their profile opens over the group beside`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("carla")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnContactClicked("name:carla")))

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail("aniversario", joinedAs = null, GroupTab.MEMBERS),
                    Route.ContactProfile("name:carla"),
                ),
                navigator.backStack.value,
            )
            assertTrue(viewModel.uiState.value.search.isOpen)
        }

    @Test
    fun `GIVEN a bill already open from the search WHEN tapping a contact THEN nothing else opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnBillClicked("churrasco", "carne")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnContactClicked("name:carla")))

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail("churrasco", joinedAs = null, GroupTab.EXPENSES),
                    Route.BillDetail("churrasco", "carne", focusCommentId = null),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a contact found in the groups WHEN tapping it THEN tracks a contact result from the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("carla")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnContactClicked("name:carla")))

            // Then
            assertEquals(
                listOf(
                    "search_opened" to mapOf<String, Any>("source" to "groups"),
                    "search_result_clicked" to mapOf<String, Any>("kind" to "contact", "source" to "groups"),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN another group beside the search WHEN tapping a bill THEN its group takes the place under the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnQueryChanged("mercado")))

            // When
            viewModel.onEvent(GroupsUiEvent.OnSearch(SearchUiEvent.OnBillClicked("churrasco", "carne")))

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail("churrasco", joinedAs = null, GroupTab.EXPENSES),
                    Route.BillDetail("churrasco", "carne", focusCommentId = null),
                ),
                navigator.backStack.value,
            )
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

    // Why: no bill yet, so it comes last in the home's order.
    private val viagem = group(
        id = "viagem",
        name = "Viagem a Paraty",
        names = listOf("Pierre", "Larissa", "Bruno"),
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
        userIds: Map<String, String> = emptyMap(),
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
                userId = userIds[memberName],
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

    private fun prepareScenario(
        storedGroups: List<Group> = listOf(churrasco, aniversario, ape),
        storedExpenses: List<Expense> = storedBills,
        backStack: List<Route> = listOf(Route.Groups),
    ) {
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        groups = FakeGroupRepository(initialGroups = storedGroups)
        navigator = BackStackNavigator(startRoute = backStack.first()).also { navigator ->
            backStack.drop(1).forEach(navigator::navigate)
        }
        val deletions = InMemoryGroupDeletionRepository()
        viewModel = GroupsViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
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
            observeFirstDownload = ObserveFirstDownload { flowOf(null) },
        )
    }
}
