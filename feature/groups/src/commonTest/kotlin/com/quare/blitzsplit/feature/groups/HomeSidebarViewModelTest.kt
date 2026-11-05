package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.groups.presentation.model.HomeSidebarUiEvent
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.HomeSidebarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class HomeSidebarViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var profiles: FakeProfileRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: HomeSidebarViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the groups not read yet WHEN the sidebar opens THEN draws nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            groupsRead = false,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertTrue(state.openGroups.isEmpty())
    }

    @Test
    fun `GIVEN no group on the device WHEN the sidebar opens THEN is loaded with no open group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = emptyList())

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertTrue(state.openGroups.isEmpty())
        }

    @Test
    fun `GIVEN no profile yet WHEN the sidebar opens THEN shows no name with the default color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("", state.displayName)
            assertEquals("", state.avatarInitial)
            assertEquals(AvatarColor.default, state.avatarColor)
            assertNull(state.photoUrl)
        }

    @Test
    fun `GIVEN a name with spaces around it WHEN the sidebar opens THEN shows it trimmed with its initial`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = UserProfile("  pierre Vieira  ", AvatarColor.GREEN, photo = null))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("pierre Vieira", state.displayName)
            assertEquals("P", state.avatarInitial)
            assertEquals(AvatarColor.GREEN, state.avatarColor)
            assertNull(state.photoUrl)
        }

    @Test
    fun `GIVEN a profile with a photo WHEN the sidebar opens THEN shows the photo address`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            profile = UserProfile("Pierre", AvatarColor.GREEN, ProfilePhoto.Remote("https://photos/pierre.jpg")),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals("https://photos/pierre.jpg", state.photoUrl)
    }

    @Test
    fun `GIVEN the sidebar open WHEN the profile is renamed THEN shows the new name and initial`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val renamed = UserProfile("Ana", AvatarColor.LILAC, photo = null)

            // When
            profiles.profile.value = renamed

            // Then
            assertEquals("Ana", viewModel.uiState.value.displayName)
            assertEquals("A", viewModel.uiState.value.avatarInitial)
            assertEquals(AvatarColor.LILAC, viewModel.uiState.value.avatarColor)
        }

    @Test
    fun `GIVEN an account connected with Apple WHEN the sidebar opens THEN shows Apple as its account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Connected("pierre", AccountProvider.APPLE))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(AccountProvider.APPLE, state.accountProvider)
        }

    @Test
    fun `GIVEN an anonymous account WHEN the sidebar opens THEN shows no account`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Anonymous("anonymous"))

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.accountProvider)
    }

    @Test
    fun `GIVEN a device only account WHEN the sidebar opens THEN shows no account`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.accountProvider)
    }

    @Test
    fun `GIVEN an anonymous account WHEN it connects Google THEN shows Google as its account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Anonymous("anonymous"))
            val connected = AccountStatus.Connected("pierre", AccountProvider.GOOGLE)

            // When
            account.status = connected

            // Then
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.accountProvider)
        }

    @Test
    fun `GIVEN several open groups WHEN the sidebar opens THEN lists them by latest activity`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(
                        id = "old",
                        updatedAt = daysAgo(30),
                    ),
                    trio(
                        id = "renamed",
                        updatedAt = daysAgo(2),
                    ),
                    trio(
                        id = "billed",
                        updatedAt = daysAgo(30),
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "b1",
                        groupId = "billed",
                        updatedAt = daysAgo(1),
                    ),
                ),
            )

            // When
            val ids = viewModel.uiState.value.openGroups
                .map { it.id }

            // Then
            assertEquals(listOf("billed", "renamed", "old"), ids)
        }

    @Test
    fun `GIVEN a settled group and one without bills WHEN opening THEN leaves out only the settled one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(id = "settled"), trio(id = "empty")),
                storedExpenses = listOf(bill("b1", groupId = "settled")),
                storedSettlements = listOf(
                    payment(
                        "p1",
                        groupId = "settled",
                        from = "Ana",
                        to = "Pierre",
                        amountMinor = 3000L,
                    ),
                    payment(
                        "p2",
                        groupId = "settled",
                        from = "Bruno",
                        to = "Pierre",
                        amountMinor = 3000L,
                    ),
                ),
            )

            // When
            val ids = viewModel.uiState.value.openGroups
                .map { it.id }

            // Then
            assertEquals(listOf("empty"), ids)
        }

    @Test
    fun `GIVEN a group where only others still owe WHEN the sidebar opens THEN keeps it open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "b1",
                        paidBy = "Ana",
                        splitOver = listOf("Ana", "Bruno"),
                    ),
                ),
            )

            // When
            val ids = viewModel.uiState.value.openGroups
                .map { it.id }

            // Then
            assertEquals(listOf("trio"), ids)
        }

    @Test
    fun `GIVEN an open group WHEN the last payment settles it THEN it leaves the open groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "other")),
                storedExpenses = listOf(bill("b1")),
                storedSettlements = listOf(
                    payment(
                        "p1",
                        from = "Ana",
                        to = "Pierre",
                        amountMinor = 3000L,
                    ),
                ),
            )
            val lastPayment = payment(
                "p2",
                from = "Bruno",
                to = "Pierre",
                amountMinor = 3000L,
            )

            // When
            settlements.settlements.value += lastPayment

            // Then
            assertEquals(
                listOf("other"),
                viewModel.uiState.value.openGroups
                    .map { it.id },
            )
        }

    @Test
    fun `GIVEN the open groups WHEN a sync brings a new group THEN lists it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio(updatedAt = daysAgo(10))))
        val arrived = trio(
            id = "arrived",
            updatedAt = daysAgo(1),
        )

        // When
        groups.groups.value += arrived

        // Then
        assertEquals(
            listOf("arrived", "trio"),
            viewModel.uiState.value.openGroups
                .map { it.id },
        )
    }

    @Test
    fun `GIVEN a group opened from Grupos WHEN tapping Grupos THEN goes back to it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(stack = listOf(Route.Home, Route.Groups, expensesOf("trio"), Route.GroupStats("trio")))

        // When
        viewModel.onEvent(HomeSidebarUiEvent.OnTabClicked(HomeTab.GROUPS))

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the profile over a filtered Saldos WHEN tapping Saldos THEN goes back to it with its filter`() =
        runTest(testDispatcher) {
            // Given
            val balances = Route.GlobalBalances(BalancesFilter.YOU_OWE, charge = null)
            prepareScenario(stack = listOf(Route.Home, balances, Route.Profile))

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnTabClicked(HomeTab.BALANCES))

            // Then
            assertEquals(listOf(Route.Home, balances), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a group opened from Grupos WHEN tapping Inicio THEN goes back to Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario(stack = listOf(Route.Home, Route.Groups, expensesOf("trio")))

        // When
        viewModel.onEvent(HomeSidebarUiEvent.OnTabClicked(HomeTab.HOME))

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group page WHEN tapping the profile THEN opens the profile over it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(stack = listOf(Route.Home, Route.Groups, expensesOf("trio")))

        // When
        viewModel.onEvent(HomeSidebarUiEvent.OnProfileClicked)

        // Then
        assertEquals(
            listOf(Route.Home, Route.Groups, expensesOf("trio"), Route.Profile),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN another group over Grupos WHEN tapping a group THEN opens it in the place of the other`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                stack = listOf(
                    Route.Home,
                    Route.Groups,
                    Route.GroupDetail(
                        "other",
                        joinedAs = null,
                        tab = GroupTab.BALANCES,
                    ),
                    Route.GroupStats("other"),
                ),
            )

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnGroupClicked("trio"))

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, expensesOf("trio")), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the same group on Membros under Estatisticas WHEN tapping it THEN goes back to it on Membros`() =
        runTest(testDispatcher) {
            // Given
            val members = Route.GroupDetail(
                "trio",
                joinedAs = null,
                tab = GroupTab.MEMBERS,
            )
            prepareScenario(stack = listOf(Route.Home, Route.Groups, members, Route.GroupStats("trio")))

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnGroupClicked("trio"))

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, members), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the profile over Grupos WHEN tapping a group THEN opens the group in the place of the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stack = listOf(Route.Home, Route.Groups, Route.Profile))

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnGroupClicked("trio"))

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, expensesOf("trio")), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a bill from Atividade WHEN tapping a group THEN opens it over Grupos instead`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                stack = listOf(
                    Route.Home,
                    Route.Activity(personKey = null),
                    Route.BillDetail("other", "b1", focusCommentId = null),
                ),
            )

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnGroupClicked("trio"))

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, expensesOf("trio")), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the same group opened from Inicio WHEN tapping it THEN stays on it`() = runTest(testDispatcher) {
        // Given
        val members = Route.GroupDetail(
            "trio",
            joinedAs = null,
            tab = GroupTab.MEMBERS,
        )
        prepareScenario(stack = listOf(Route.Home, members))

        // When
        viewModel.onEvent(HomeSidebarUiEvent.OnGroupClicked("trio"))

        // Then
        assertEquals(listOf(Route.Home, members), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another group opened from Inicio WHEN tapping a group THEN opens it over Grupos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stack = listOf(Route.Home, expensesOf("flat")))

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnGroupClicked("trio"))

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, expensesOf("trio")), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a back stack without the home WHEN tapping a group THEN stays where it is`() = runTest(testDispatcher) {
        // Given
        prepareScenario(stack = listOf(Route.Welcome))

        // When
        viewModel.onEvent(HomeSidebarUiEvent.OnGroupClicked("trio"))

        // Then
        assertEquals(listOf<Route>(Route.Welcome), navigator.backStack.value)
    }

    @Test
    fun `GIVEN an open group in the sidebar WHEN tapping it THEN tracks group_clicked with only the source`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stack = listOf(Route.Home, Route.Groups))

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnGroupClicked("trio"))

            // Then
            assertEquals(listOf("group_clicked" to mapOf<String, Any>("source" to "sidebar")), trackedEvents)
        }

    @Test
    fun `GIVEN a group opened from Groups WHEN tapping the logo THEN goes back to Home`() = runTest(testDispatcher) {
        // Given
        prepareScenario(stack = listOf(Route.Home, Route.Groups, expensesOf("trio")))

        // When
        viewModel.onEvent(HomeSidebarUiEvent.OnBrandClicked)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the sidebar WHEN tapping the logo THEN tracks brand_clicked from the sidebar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnBrandClicked)

            // Then
            assertEquals(listOf("brand_clicked" to mapOf<String, Any>("source" to "sidebar")), trackedEvents)
        }

    @Test
    fun `GIVEN the sidebar WHEN tapping the profile at its foot THEN tracks profile_avatar_clicked from the sidebar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnProfileClicked)

            // Then
            assertEquals(listOf("profile_avatar_clicked" to mapOf<String, Any>("source" to "sidebar")), trackedEvents)
        }

    @Test
    fun `GIVEN no group on the device WHEN the sidebar opens THEN Buscar is not offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = emptyList())

        // When
        val canSearch = viewModel.uiState.value.canSearch

        // Then
        assertFalse(canSearch)
    }

    @Test
    fun `GIVEN a group on the device WHEN the sidebar opens THEN Buscar is offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        val canSearch = viewModel.uiState.value.canSearch

        // Then
        assertTrue(canSearch)
    }

    @Test
    fun `GIVEN only a group where everyone settled WHEN the sidebar opens THEN Buscar is still offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("b1")),
                storedSettlements = listOf(
                    payment(
                        "p1",
                        from = "Ana",
                        to = "Pierre",
                        amountMinor = 3000L,
                    ),
                    payment(
                        "p2",
                        from = "Bruno",
                        to = "Pierre",
                        amountMinor = 3000L,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.openGroups.isEmpty())
            assertTrue(state.canSearch)
        }

    @Test
    fun `GIVEN no group on the device WHEN a sync brings the first one THEN Buscar is offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = emptyList())
            val arrived = trio()

            // When
            groups.groups.value += arrived

            // Then
            assertTrue(viewModel.uiState.value.canSearch)
        }

    @Test
    fun `GIVEN the groups not read yet WHEN tapping Buscar THEN it is not offered and nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                groupsRead = false,
                stack = listOf(Route.Home, Route.Profile),
            )

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnSearchClicked)

            // Then
            assertFalse(viewModel.uiState.value.canSearch)
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a group on the device WHEN the last group leaves the device THEN Buscar is no longer offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            groups.groups.value = emptyList()

            // Then
            assertFalse(viewModel.uiState.value.canSearch)
        }

    @Test
    fun `GIVEN a group opened from Grupos WHEN tapping Buscar THEN goes back to Grupos and opens the search over it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stack = listOf(Route.Home, Route.Groups, expensesOf("trio")))

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, Route.Search), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a group opened from Grupos WHEN tapping Buscar THEN tracks search_opened from the sidebar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stack = listOf(Route.Home, Route.Groups, expensesOf("trio")))

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf("search_opened" to mapOf<String, Any>("source" to "sidebar")), trackedEvents)
        }

    @Test
    fun `GIVEN the profile over Inicio WHEN tapping Buscar THEN goes back to Inicio and opens the search over it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stack = listOf(Route.Home, Route.Profile))

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Search), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Atividade of a contact opened from Contatos WHEN tapping Buscar THEN opens the search over Contatos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                stack = listOf(
                    Route.Home,
                    Route.Contacts,
                    Route.ContactProfile(anaKey),
                    Route.Activity(personKey = anaKey),
                ),
            )

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts, Route.Search), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a contact opened from a filtered Saldos WHEN tapping Buscar THEN opens the search over that Saldos`() =
        runTest(testDispatcher) {
            // Given
            val balances = Route.GlobalBalances(BalancesFilter.YOU_OWE, charge = null)
            prepareScenario(stack = listOf(Route.Home, balances, Route.ContactProfile(anaKey)))

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf(Route.Home, balances, Route.Search), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no group on the device WHEN a late tap on Buscar arrives THEN stays where it is`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = emptyList(),
                stack = listOf(Route.Home, Route.Profile),
            )

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no group on the device WHEN a late tap on Buscar arrives THEN tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = emptyList(),
                stack = listOf(Route.Home, Route.Profile),
            )

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnSearchClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a back stack without the home WHEN tapping Buscar THEN stays where it is and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stack = listOf(Route.Welcome, Route.Profile))

            // When
            viewModel.onEvent(HomeSidebarUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf(Route.Welcome, Route.Profile), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Buscar in the sidebar WHEN reading its analytics THEN the ViewModel sends search_opened`() {
        // Given
        val event = HomeSidebarUiEvent.OnSearchClicked

        // When
        val analytics = event.analytics

        // Then
        assertEquals(EventAnalytics.Manual("search_opened"), analytics)
    }

    // Why: Saturday, 31 October 2026, 15:00 in UTC, the time zone the tests count days in.
    private val now = Instant.parse("2026-10-31T15:00:00Z").toEpochMilliseconds()

    private val pierre = UserProfile("Pierre", AvatarColor.GREEN, photo = null)

    private val everyone = listOf("Pierre", "Ana", "Bruno")

    private val anaKey = "ana"

    private fun daysAgo(days: Int): Long = now - days.days.inWholeMilliseconds

    private fun expensesOf(groupId: String): Route.GroupDetail = Route.GroupDetail(
        groupId,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
    )

    // Why: Pierre is this device's user, in a group with Ana and Bruno.
    private fun trio(
        id: String = "trio",
        updatedAt: Long = daysAgo(10),
    ): Group = Group(
        id = id,
        name = "Apê 302",
        type = GroupType.OTHER,
        color = AvatarColor.BLUE,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(member("Pierre").copy(isMe = true), member("Ana"), member("Bruno")),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = updatedAt,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private fun member(name: String): GroupMember = GroupMember(
        id = "member-$name",
        name = name,
        avatarColor = AvatarColor.BLUE,
        photoUrl = null,
        isMe = false,
        hasJoined = false,
        userId = null,
        inviteCode = null,
    )

    // Why: one payer for the whole bill, split evenly over the members given.
    private fun bill(
        id: String,
        groupId: String = "trio",
        paidBy: String = "Pierre",
        amountMinor: Long = 9000L,
        splitOver: List<String> = everyone,
        updatedAt: Long = daysAgo(5),
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = "Mercado",
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer("member-$paidBy", amountMinor)),
        shares = splitOver.map { ExpenseShare("member-$it", amountMinor / splitOver.size, 1L) },
        spentAt = updatedAt,
        isPending = false,
        updatedAt = updatedAt,
        updatedByMemberId = null,
        items = null,
        categoryId = null,
    )

    private fun payment(
        id: String,
        groupId: String = "trio",
        from: String,
        to: String,
        amountMinor: Long,
    ): Settlement = Settlement(
        id = id,
        groupId = groupId,
        fromMemberId = "member-$from",
        toMemberId = "member-$to",
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = daysAgo(1),
        isPending = false,
        updatedAt = daysAgo(1),
    )

    private fun prepareScenario(
        status: AccountStatus = AccountStatus.LocalOnly,
        profile: UserProfile? = pierre,
        storedGroups: List<Group> = listOf(trio()),
        groupsRead: Boolean = true,
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
        stack: List<Route> = listOf(Route.Home, Route.Groups, expensesOf("trio")),
    ) {
        account = FakeAccountRepository(initialStatus = status)
        profiles = FakeProfileRepository(profile)
        groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        navigator = BackStackNavigator(startRoute = stack.first()).also { nav -> stack.drop(1).forEach(nav::navigate) }
        viewModel = HomeSidebarViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            navigator = navigator,
            currentTimeMillis = CurrentTimeMillis { now },
            currentTimeZone = CurrentTimeZone { TimeZone.UTC },
            observeProfile = ObserveProfileUseCase(profiles),
            observeAccount = ObserveAccountUseCase(account),
            // Why: a StateFlow is read at once, so the device not answering yet is a flow that has not emitted.
            observeGroups = if (groupsRead) ObserveGroupsUseCase(groups) else ObserveGroups { MutableSharedFlow() },
            observeGroupsMoney = ObserveGroupsMoneyUseCase(expenses, settlements),
        )
    }
}
