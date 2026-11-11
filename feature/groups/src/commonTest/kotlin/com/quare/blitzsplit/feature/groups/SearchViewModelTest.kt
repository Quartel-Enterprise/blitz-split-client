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
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearch
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearchResults
import com.quare.blitzsplit.feature.groups.presentation.model.SearchContactHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchPersonHit
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
    fun `GIVEN the search over Contatos WHEN a sync deletes every group THEN stays open with what was typed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Contacts)
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("apê"))

            // When
            groups.groups.value = emptyList()

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts, Route.Search), navigator.backStack.value)
            assertEquals("apê", viewModel.uiState.value.search.query)
        }

    @Test
    fun `GIVEN no group on the device WHEN the search opens over Saldos THEN it stays open`() =
        runTest(testDispatcher) {
            // Given
            val balances = Route.GlobalBalances(BalancesFilter.ALL)
            prepareScenario(
                tab = balances,
                storedGroups = emptyList(),
                storedExpenses = emptyList(),
            )

            // When
            val backStack = navigator.backStack.value

            // Then
            assertEquals(listOf(Route.Home, balances, Route.Search), backStack)
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
                        isOverBill = false,
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

    @Test
    fun `GIVEN the search over a tab WHEN picking Pessoas THEN tracks search_scope_selected from the search`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SearchUiEvent.OnScopeChanged(SearchScope.PEOPLE))

            // Then
            assertEquals(
                listOf(
                    "search_scope_selected" to mapOf<String, Any>("scope" to SearchScope.PEOPLE, "source" to "search"),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the search over a tab WHEN tapping a group found THEN tracks a group result without its id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("chur"))

            // When
            viewModel.onEvent(SearchUiEvent.OnGroupClicked("churrasco"))

            // Then
            assertEquals(
                listOf("search_result_clicked" to mapOf<String, Any>("kind" to "group", "source" to "search")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the search over a tab WHEN tapping a person found THEN tracks a person result without the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("carla"))

            // When
            viewModel.onEvent(SearchUiEvent.OnPersonClicked("ape"))

            // Then
            assertEquals(
                listOf("search_result_clicked" to mapOf<String, Any>("kind" to "person", "source" to "search")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the search over a tab WHEN tapping a bill found THEN tracks a bill result without ids`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("carne"))

            // When
            viewModel.onEvent(SearchUiEvent.OnBillClicked("churrasco", "carne"))

            // Then
            assertEquals(
                listOf("search_result_clicked" to mapOf<String, Any>("kind" to "bill", "source" to "search")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the search over a tab WHEN typing and closing it THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SearchUiEvent.OnQueryChanged("chur"))
        viewModel.onEvent(SearchUiEvent.OnClosed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the search over Inicio WHEN tapping the Inicio tab THEN goes back to Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Home)

            // When
            viewModel.onEvent(SearchUiEvent.OnHomeTabClicked(HomeTab.HOME))

            // Then
            assertEquals(listOf(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search over Saldos WHEN tapping the Saldos tab THEN goes back to that Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = balances)

            // When
            viewModel.onEvent(SearchUiEvent.OnHomeTabClicked(HomeTab.BALANCES))

            // Then
            assertEquals(listOf(Route.Home, balances), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search over Voce deve WHEN tapping the Saldos tab THEN goes back to it keeping the filter`() =
        runTest(testDispatcher) {
            // Given
            val youOwe = Route.GlobalBalances(BalancesFilter.YOU_OWE)
            prepareScenario(tab = youOwe)

            // When
            viewModel.onEvent(SearchUiEvent.OnHomeTabClicked(HomeTab.BALANCES))

            // Then
            assertEquals(listOf(Route.Home, youOwe), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search over Inicio WHEN tapping the Contatos tab THEN Contatos opens right above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Home)

            // When
            viewModel.onEvent(SearchUiEvent.OnHomeTabClicked(HomeTab.CONTACTS))

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search over Inicio WHEN tapping the Grupos tab THEN Grupos opens right above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Home)

            // When
            viewModel.onEvent(SearchUiEvent.OnHomeTabClicked(HomeTab.GROUPS))

            // Then
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search over Contatos WHEN tapping the Saldos tab THEN Saldos with everyone opens above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Contacts)

            // When
            viewModel.onEvent(SearchUiEvent.OnHomeTabClicked(HomeTab.BALANCES))

            // Then
            assertEquals(listOf(Route.Home, balances), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search over Contatos WHEN tapping the Inicio tab THEN closes both and goes back to Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Contacts)

            // When
            viewModel.onEvent(SearchUiEvent.OnHomeTabClicked(HomeTab.HOME))

            // Then
            assertEquals(listOf(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a group already opened from the search WHEN a late tab tap arrives THEN the group stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Home)
            viewModel.onEvent(SearchUiEvent.OnGroupClicked("ape"))

            // When
            viewModel.onEvent(SearchUiEvent.OnHomeTabClicked(HomeTab.CONTACTS))

            // Then
            assertEquals(listOf(Route.Home, expensesOf("ape")), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search already closed WHEN a late tab tap arrives THEN the tab under it stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Contacts)
            viewModel.onEvent(SearchUiEvent.OnClosed)

            // When
            viewModel.onEvent(SearchUiEvent.OnHomeTabClicked(HomeTab.BALANCES))

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search over a tab WHEN tapping a tab of the bar THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(tab = Route.Contacts)

        // When
        viewModel.onEvent(SearchUiEvent.OnHomeTabClicked(HomeTab.GROUPS))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a tap on each tab around the search WHEN reading its analytics THEN none is tracked from anywhere`() {
        // Given
        val events = HomeTab.entries.map { SearchUiEvent.OnHomeTabClicked(it) }

        // When
        val analytics = events.map { it.analytics } + events.map { it.toEventAnalytics(source = "groups") }

        // Then
        assertEquals(List(events.size * 2) { EventAnalytics.NotTracked }, analytics)
    }

    @Test
    fun `GIVEN a name missing a letter WHEN searching it THEN finds the person and suggests the name as written`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(churrasco, aniversario, ape, viagem))

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("Larisa"))

            // Then
            val search = viewModel.uiState.value.search
            assertEquals(listOf("Larissa"), search.results.people.map { it.name })
            assertEquals(listOf("viagem"), search.results.people.map { it.groupId })
            assertEquals("Larissa", search.results.suggestion)
            assertTrue(search.showsSuggestion)
            assertFalse(search.showsNoResults)
        }

    @Test
    fun `GIVEN a text a bill holds and a group name comes close to WHEN searching THEN only what holds it shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("churros"))

            // Then
            val search = viewModel.uiState.value.search
            assertEquals(listOf("aniversario"), search.results.groups.map { it.group.id })
            assertTrue(search.results.bills.isEmpty())
            assertTrue(search.results.people.isEmpty())
            assertTrue(search.results.contacts.isEmpty())
            assertNull(search.results.suggestion)
            assertFalse(search.showsSuggestion)
        }

    @Test
    fun `GIVEN only close matches WHEN searching THEN each section lists the closest first and suggests the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = storedBills + grill)

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("churrasko"))

            // Then
            val results = viewModel.uiState.value.search.results
            assertEquals(listOf("churrasco"), results.groups.map { it.group.id })
            assertEquals(listOf("carne", "grill"), results.bills.map { it.id })
            assertEquals("Churrasco", results.suggestion)
        }

    @Test
    fun `GIVEN a bill holding the text and a person close to it WHEN searching everything THEN only the bill shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(churrasco, aniversario, ape, viagem),
                storedExpenses = storedBills + presente,
            )

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("larisa"))

            // Then
            val results = viewModel.uiState.value.search.results
            assertEquals(listOf("presente"), results.bills.map { it.id })
            assertTrue(results.people.isEmpty())
            assertTrue(results.contacts.isEmpty())
            assertNull(results.suggestion)
        }

    @Test
    fun `GIVEN a bill holding the text and a person close to it WHEN picking Pessoas THEN the close person shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(churrasco, aniversario, ape, viagem),
                storedExpenses = storedBills + presente,
            )
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("larisa"))

            // When
            viewModel.onEvent(SearchUiEvent.OnScopeChanged(SearchScope.PEOPLE))

            // Then
            val search = viewModel.uiState.value.search
            assertEquals(listOf("Larissa"), search.results.people.map { it.name })
            assertEquals(listOf("name:larissa"), search.results.contacts.map { it.personKey })
            assertTrue(search.results.bills.isEmpty())
            assertEquals("Larissa", search.results.suggestion)
            assertTrue(search.showsSuggestion)
        }

    @Test
    fun `GIVEN a settled group with two bills close to the text WHEN searching THEN it comes by the closest bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = storedBills + brigadeiros)

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("brigadeyros"))

            // Then
            val results = viewModel.uiState.value.search.results
            val hit = results.groups.single()
            assertEquals("aniversario", hit.group.id)
            assertNull(hit.nameMatch)
            assertEquals("Brigadeiros", hit.billDescription)
            assertTrue(results.bills.isEmpty())
            assertEquals("Brigadeiros", results.suggestion)
        }

    @Test
    fun `GIVEN a settled group with two bills as close to the text WHEN searching THEN it comes by the most recent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = storedBills + brigadeiros)

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("brigadeyro"))

            // Then
            val hit = viewModel.uiState.value.search.results.groups
                .single()
            assertEquals("aniversario", hit.group.id)
            assertEquals("Brigadeiro", hit.billDescription)
        }

    @Test
    fun `GIVEN a settled group named close to a text its bill holds WHEN searching THEN only what holds it shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    churrasco,
                    group(
                        id = "aniversario",
                        name = "Churras da Ana",
                        names = listOf("Pierre", "Ana", "Carla"),
                    ),
                    ape,
                ),
            )

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("churros"))

            // Then
            val results = viewModel.uiState.value.search.results
            assertEquals(listOf("aniversario"), results.groups.map { it.group.id })
            assertNull(results.suggestion)
        }

    @Test
    fun `GIVEN a suggestion WHEN tapping it THEN searches it instead and finds it without a suggestion`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(churrasco, aniversario, ape, viagem))
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("Larisa"))

            // When
            viewModel.onEvent(SearchUiEvent.OnSuggestionClicked("Larissa"))

            // Then
            val search = viewModel.uiState.value.search
            assertEquals("Larissa", search.query)
            assertEquals(listOf("Larissa"), search.results.people.map { it.name })
            assertNull(search.results.suggestion)
            assertFalse(search.showsSuggestion)
            assertTrue(search.showsResults)
        }

    @Test
    fun `GIVEN the search over a tab WHEN tapping a suggestion THEN tracks the suggestion from the search`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(churrasco, aniversario, ape, viagem))
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("Larisa"))

            // When
            viewModel.onEvent(SearchUiEvent.OnSuggestionClicked("Larissa"))

            // Then
            assertEquals(
                listOf("search_suggestion_clicked" to mapOf<String, Any>("source" to "search")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a suggestion but nothing typed WHEN reading the search THEN shows no suggestion`() {
        // Given
        val search = GroupsSearch.closed.copy(
            isOpen = true,
            query = "  ",
            results = larissaResults,
        )

        // When
        val showsSuggestion = search.showsSuggestion

        // Then
        assertFalse(showsSuggestion)
    }

    @Test
    fun `GIVEN a suggestion but no result WHEN reading the search THEN shows no suggestion`() {
        // Given
        val search = GroupsSearch.closed.copy(
            isOpen = true,
            query = "Larisa",
            results = GroupsSearchResults.none.copy(suggestion = "Larissa"),
        )

        // When
        val showsSuggestion = search.showsSuggestion

        // Then
        assertFalse(showsSuggestion)
    }

    @Test
    fun `GIVEN a guest in two groups WHEN searching their name THEN finds one contact counting both groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("carla"))

            // Then
            val results = viewModel.uiState.value.search.results
            assertEquals(
                listOf(
                    SearchContactHit(
                        personKey = "name:carla",
                        name = "Carla",
                        nameMatch = TextMatch(0, 5),
                        initial = "C",
                        color = AvatarColor.PEACH,
                        photoUrl = null,
                        groupCount = 2,
                    ),
                ),
                results.contacts,
            )
            assertEquals(listOf("aniversario", "ape"), results.people.map { it.groupId })
        }

    @Test
    fun `GIVEN people found in several groups WHEN searching THEN the contacts follow their first row`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("n"))

            // Then
            val results = viewModel.uiState.value.search.results
            assertEquals(listOf("Ana", "Bruno", "Ana"), results.people.map { it.name })
            assertEquals(listOf("name:ana", "name:bruno"), results.contacts.map { it.personKey })
            assertEquals(listOf(2, 1), results.contacts.map { it.groupCount })
        }

    @Test
    fun `GIVEN an account and a guest of the same name WHEN searching it THEN finds two contacts`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "churrasco",
                        name = "Churrasco de sábado",
                        names = listOf("Pierre", "Ana", "Bruno"),
                        userIds = mapOf("Ana" to "u-ana"),
                    ),
                    aniversario,
                    ape,
                ),
            )

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("ana"))

            // Then
            val contacts = viewModel.uiState.value.search.results.contacts
            assertEquals(listOf("user:u-ana", "name:ana"), contacts.map { it.personKey })
            assertEquals(listOf(1, 1), contacts.map { it.groupCount })
        }

    @Test
    fun `GIVEN an account named differently in two groups WHEN searching THEN finds one contact counting both`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "churrasco",
                        name = "Churrasco de sábado",
                        names = listOf("Pierre", "Ana", "Bruno", "Bia"),
                        userIds = mapOf("Bia" to "u-bia"),
                    ),
                    aniversario,
                    group(
                        id = "ape",
                        name = "Apê 302",
                        names = listOf("Pierre", "Carla", "Bia Souza"),
                        userIds = mapOf("Bia Souza" to "u-bia"),
                    ),
                ),
            )

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("bia"))

            // Then
            val results = viewModel.uiState.value.search.results
            assertEquals(listOf("Bia", "Bia Souza"), results.people.map { it.name })
            val contact = results.contacts.single()
            assertEquals("user:u-bia", contact.personKey)
            assertEquals("Bia", contact.name)
            assertEquals(2, contact.groupCount)
        }

    @Test
    fun `GIVEN close people only WHEN searching THEN the contacts follow their first row among the closest`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = mariannes)

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("marianne"))

            // Then
            val results = viewModel.uiState.value.search.results
            assertEquals(listOf("Marianna", "Mariane", "Mariana"), results.people.map { it.name })
            assertEquals(listOf("name:marianna", "user:u-mari"), results.contacts.map { it.personKey })
            assertEquals("Marianna", results.suggestion)
        }

    @Test
    fun `GIVEN a person also in a group this user left WHEN searching THEN counts only the groups in common`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(churrasco, aniversario, ape, leftGroup))

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("carla"))

            // Then
            val results = viewModel.uiState.value.search.results
            assertEquals(listOf("aniversario", "ape", "left"), results.people.map { it.groupId })
            assertEquals(2, results.contacts.single().groupCount)
        }

    @Test
    fun `GIVEN a person only in a group this user left WHEN searching THEN shows their row but no contact`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(churrasco, aniversario, ape, leftGroup))

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("teodoro"))

            // Then
            val results = viewModel.uiState.value.search.results
            assertEquals(listOf("Teodoro"), results.people.map { it.name })
            assertTrue(results.contacts.isEmpty())
        }

    @Test
    fun `GIVEN an account found close in two groups WHEN searching THEN the contact shows its closest name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = mariannes)

            // When
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("marianne"))

            // Then
            val contact = viewModel.uiState.value.search.results.contacts
                .single { it.personKey == "user:u-mari" }
            assertEquals("Mariane", contact.name)
            assertEquals(TextMatch(0, 7), contact.nameMatch)
            assertEquals(2, contact.groupCount)
        }

    @Test
    fun `GIVEN a person found WHEN picking Grupos THEN no contact shows`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SearchUiEvent.OnQueryChanged("carla"))

        // When
        viewModel.onEvent(SearchUiEvent.OnScopeChanged(SearchScope.GROUPS))

        // Then
        assertTrue(
            viewModel.uiState.value.search.results.contacts
                .isEmpty(),
        )
    }

    @Test
    fun `GIVEN a person found WHEN picking Pessoas THEN their contact shows`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SearchUiEvent.OnQueryChanged("carla"))

        // When
        viewModel.onEvent(SearchUiEvent.OnScopeChanged(SearchScope.PEOPLE))

        // Then
        assertEquals(
            listOf("name:carla"),
            viewModel.uiState.value.search.results.contacts
                .map { it.personKey },
        )
    }

    @Test
    fun `GIVEN the search over Contatos WHEN tapping a contact THEN their profile takes the search place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Contacts)

            // When
            viewModel.onEvent(SearchUiEvent.OnContactClicked("name:carla"))

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, Route.ContactProfile("name:carla")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the search over Inicio WHEN tapping a contact THEN their profile opens right above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Home)

            // When
            viewModel.onEvent(SearchUiEvent.OnContactClicked("user:u-ana"))

            // Then
            assertEquals(listOf(Route.Home, Route.ContactProfile("user:u-ana")), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a group already opened from the search WHEN a late tap on a contact arrives THEN nothing else opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(tab = Route.Contacts)
            viewModel.onEvent(SearchUiEvent.OnGroupClicked("churrasco"))

            // When
            viewModel.onEvent(SearchUiEvent.OnContactClicked("name:carla"))

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, expensesOf("churrasco")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the search over a tab WHEN tapping a contact found THEN tracks a contact result without the key`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("carla"))

            // When
            viewModel.onEvent(SearchUiEvent.OnContactClicked("name:carla"))

            // Then
            assertEquals(
                listOf("search_result_clicked" to mapOf<String, Any>("kind" to "contact", "source" to "search")),
                trackedEvents,
            )
        }

    private val balances = Route.GlobalBalances(BalancesFilter.ALL)

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

    // Why: no bill yet, so it comes last in the home's order.
    private val viagem = group(
        id = "viagem",
        name = "Viagem a Paraty",
        names = listOf("Pierre", "Larissa", "Bruno"),
    )

    private val presente = bill(
        id = "presente",
        groupId = "viagem",
        description = "Presente da Larisa",
        paidBy = "Bruno",
        splitOver = listOf("Pierre", "Bruno"),
        updatedAt = 500L,
    )

    // Why: the most recent bill, though further from "churrasko" than the churrasco's one.
    private val grill = bill(
        id = "grill",
        groupId = "ape",
        description = "Churrasqueira",
        paidBy = "Carla",
        splitOver = listOf("Pierre", "Carla"),
        updatedAt = 4_000L,
    )

    // Why: two more of Ana's own bills, so the birthday stays settled; the older one is the closer to "brigadeyros".
    private val brigadeiros = listOf(
        bill(
            id = "brigadeiros",
            groupId = "aniversario",
            description = "Brigadeiros",
            paidBy = "Ana",
            splitOver = listOf("Ana"),
            updatedAt = 1_500L,
        ),
        bill(
            id = "brigadeiro",
            groupId = "aniversario",
            description = "Brigadeiro",
            paidBy = "Ana",
            splitOver = listOf("Ana"),
            updatedAt = 2_500L,
        ),
    )

    /*
     * Why: an account named "Mariana" in the churrasco and "Mariane" in the apê, and a guest "Marianna" in the birthday:
     * "marianne" is two typos from the first and one from the other two.
     */
    private val mariannes = listOf(
        group(
            id = "churrasco",
            name = "Churrasco de sábado",
            names = listOf("Pierre", "Ana", "Bruno", "Mariana"),
            userIds = mapOf("Mariana" to "u-mari"),
        ),
        group(
            id = "aniversario",
            name = "Aniversário da Ana",
            names = listOf("Pierre", "Ana", "Carla", "Marianna"),
        ),
        group(
            id = "ape",
            name = "Apê 302",
            names = listOf("Pierre", "Carla", "Mariane"),
            userIds = mapOf("Mariane" to "u-mari"),
        ),
    )

    // Why: a group still on the device that this user is no longer in, which Contacts doesn't count.
    private val leftGroup = group(
        id = "left",
        name = "Antigo",
        names = listOf("Carla", "Teodoro"),
    ).let { group -> group.copy(members = group.members.map { it.copy(isMe = false) }) }

    private val larissaResults = GroupsSearchResults.none.copy(
        people = listOf(
            SearchPersonHit(
                memberId = "viagem-Larissa",
                personKey = "name:larissa",
                name = "Larissa",
                nameMatch = TextMatch(0, 7),
                initial = "L",
                color = AvatarColor.PEACH,
                photoUrl = null,
                groupId = "viagem",
                groupName = "Viagem a Paraty",
            ),
        ),
        suggestion = "Larissa",
    )

    private fun expensesOf(groupId: String): Route = Route.GroupDetail(
        groupId,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
        isOverBill = false,
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
        currency = GroupCurrency.BRL,
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
    private fun prepareScenario(
        tab: Route = Route.Contacts,
        storedGroups: List<Group> = listOf(churrasco, aniversario, ape),
        storedExpenses: List<Expense> = storedBills,
    ) {
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        groups = FakeGroupRepository(initialGroups = storedGroups)
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
